package com.payflow.payment.event;

import com.payflow.payment.Payment;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Called from within PaymentServiceImpl's @Transactional method. Publishing here
 * is synchronous but only registers the event for AFTER_COMMIT dispatch via
 * PaymentEventListener — it does NOT touch Kafka directly. See that class.
 */
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publishPaymentCreated(Payment payment) {
        String correlationId = MDC.get("correlationId");
        PaymentCreatedEvent event = new PaymentCreatedEvent(
            payment.getId(), payment.getMerchantId(), payment.getAmount(), payment.getCurrency(),
            payment.getPaymentMethod(), payment.getProvider(), correlationId, Instant.now()
        );
        applicationEventPublisher.publishEvent(new PaymentCreatedApplicationEvent(event));
    }
}
