package com.payflow.payment.event;

import com.payflow.payment.Payment;
import com.payflow.payment.PaymentRepository;
import com.payflow.payment.PaymentStateMachine;
import com.payflow.payment.PaymentStatus;
import com.payflow.provider.PaymentProvider;
import com.payflow.provider.ProviderRegistry;
import com.payflow.provider.ProviderResult;
import com.payflow.provider.RetryExecutor;
import com.payflow.provider.SimulationScenario;
import com.payflow.webhook.WebhookEventPublisher;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Consumes payment.created, moves the payment through PROCESSING and calls the
 * resolved provider with retry/backoff. This is where CREATED -> PROCESSING ->
 * SUCCESS/FAILED/TIMEOUT actually happens, entirely off the request thread —
 * PaymentController returns as soon as the row is committed and the event is queued.
 */
@Component
@RequiredArgsConstructor
public class PaymentWorker {

    private static final Logger log = LoggerFactory.getLogger(PaymentWorker.class);

    private final PaymentRepository paymentRepository;
    private final PaymentStateMachine stateMachine;
    private final ProviderRegistry providerRegistry;
    private final RetryExecutor retryExecutor;
    private final WebhookEventPublisher webhookEventPublisher;

    @KafkaListener(topics = "${payflow.kafka.topics.payment-created}", groupId = "payflow-payment-workers")
    @Transactional
    public void onPaymentCreated(PaymentCreatedEvent event) {
        if (event.correlationId() != null) {
            MDC.put("correlationId", event.correlationId());
        }
        try {
            process(event);
        } finally {
            MDC.remove("correlationId");
        }
    }

    private void process(PaymentCreatedEvent event) {
        Payment payment = paymentRepository.findById(event.paymentId()).orElse(null);
        if (payment == null) {
            log.error("PaymentWorker received event for unknown payment {}", event.paymentId());
            return;
        }
        if (payment.getStatus() != PaymentStatus.CREATED) {
            log.warn("Payment {} already in status {} — skipping duplicate processing", payment.getId(), payment.getStatus());
            return; // idempotent consumer: re-delivery of an already-processed message is a no-op
        }

        stateMachine.transition(payment.getStatus(), PaymentStatus.PROCESSING);
        payment.applyStatus(PaymentStatus.PROCESSING);
        paymentRepository.save(payment);
        log.info("Payment {} -> PROCESSING", payment.getId());

        PaymentProvider provider = providerRegistry.resolve(event.provider());
        SimulationScenario scenario = resolveScenario(payment);

        var retryOutcome = retryExecutor.execute(payment.getId(), () ->
            provider.authorize(payment.getId(), payment.getAmount(), payment.getCurrency(), scenario));

        payment.setRetryCount(retryOutcome.attemptsMade() - 1);
        applyFinalOutcome(payment, retryOutcome.result());
    }

    private void applyFinalOutcome(Payment payment, ProviderResult result) {
        PaymentStatus target = switch (result.outcome()) {
            case SUCCESS -> PaymentStatus.SUCCESS;
            case FAILED -> PaymentStatus.FAILED;
            case TIMEOUT, TRANSIENT_ERROR -> PaymentStatus.FAILED; // retries exhausted at this point
        };
        stateMachine.transition(payment.getStatus(), target);
        payment.applyStatus(target);
        paymentRepository.save(payment);
        log.info("Payment {} -> {} ({})", payment.getId(), target, result.message());

        String eventType = target == PaymentStatus.SUCCESS ? "payment.success" : "payment.failed";
        webhookEventPublisher.publish(eventType, payment.getId(), payment.getMerchantId(), Map.of(
            "status", target.name(),
            "amount", payment.getAmount(),
            "currency", payment.getCurrency(),
            "providerReference", result.providerReference() == null ? "" : result.providerReference()
        ));
    }

    // In the real Failure Simulation panel (Phase 13), scenario is threaded through
    // via payment metadata set at creation time. Kept simple here: NORMAL by default.
    private SimulationScenario resolveScenario(Payment payment) {
        return SimulationScenario.NORMAL;
    }
}
