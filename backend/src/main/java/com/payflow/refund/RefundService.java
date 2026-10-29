package com.payflow.refund;

import com.payflow.common.IdGenerator;
import com.payflow.payment.Payment;
import com.payflow.payment.PaymentRepository;
import com.payflow.payment.PaymentStateMachine;
import com.payflow.payment.PaymentStatus;
import com.payflow.provider.PaymentProvider;
import com.payflow.provider.ProviderRegistry;
import com.payflow.provider.ProviderResult;
import com.payflow.refund.dto.CreateRefundRequest;
import com.payflow.refund.dto.RefundResponse;
import com.payflow.webhook.WebhookEventPublisher;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RefundService {

    private static final Logger log = LoggerFactory.getLogger(RefundService.class);

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentStateMachine paymentStateMachine;
    private final ProviderRegistry providerRegistry;
    private final WebhookEventPublisher webhookEventPublisher;

    /**
     * Rules enforced here, in order:
     * 1. Payment must exist and be in SUCCESS state (only successful payments are refundable).
     * 2. Requested amount (or full remaining amount if null) must not push total refunded
     *    beyond the original captured amount — partial refunds accumulate against that ceiling.
     * 3. The payment's own state machine transitions SUCCESS -> REFUND_REQUESTED exactly once;
     *    subsequent refund requests on the same payment (partial refunds) do NOT re-transition
     *    the payment — only the first refund flips it out of SUCCESS.
     */
    @Transactional
    public RefundResponse createRefund(String paymentId, CreateRefundRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new RefundException("Payment not found: " + paymentId));

        boolean firstRefundOnThisPayment = payment.getStatus() == PaymentStatus.SUCCESS;
        boolean subsequentPartialRefund = payment.getStatus() == PaymentStatus.REFUND_REQUESTED
            || payment.getStatus() == PaymentStatus.REFUND_PROCESSING;

        if (!firstRefundOnThisPayment && !subsequentPartialRefund) {
            throw new RefundException("Payment %s cannot be refunded from status %s"
                .formatted(paymentId, payment.getStatus()));
        }

        BigDecimal alreadyRefunded = refundRepository.sumActiveRefundedAmount(paymentId);
        BigDecimal remaining = payment.getAmount().subtract(alreadyRefunded);
        BigDecimal requestedAmount = request.amount() != null ? request.amount() : remaining;

        if (requestedAmount.compareTo(remaining) > 0) {
            throw new RefundException("Refund amount %s exceeds remaining refundable amount %s for payment %s"
                .formatted(requestedAmount, remaining, paymentId));
        }
        if (requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RefundException("Nothing left to refund on payment " + paymentId);
        }

        Refund refund = Refund.create(IdGenerator.generate("RFD"), paymentId, requestedAmount, request.reason());
        refundRepository.save(refund);

        if (firstRefundOnThisPayment) {
            paymentStateMachine.transition(payment.getStatus(), PaymentStatus.REFUND_REQUESTED);
            payment.applyStatus(PaymentStatus.REFUND_REQUESTED);
            paymentRepository.save(payment);
        }

        log.info("Refund {} created for payment {} amount={}", refund.getRefundId(), paymentId, requestedAmount);

        processRefund(refund, payment);
        return RefundResponse.from(refund);
    }

    /**
     * Synchronous processing for simplicity here; a production system would route this
     * through Kafka (payment.refund topic) the same way payment creation does — the
     * state-machine and business-rule logic above is identical either way.
     */
    private void processRefund(Refund refund, Payment payment) {
        paymentStateMachine.transition(payment.getStatus(), PaymentStatus.REFUND_PROCESSING);
        payment.applyStatus(PaymentStatus.REFUND_PROCESSING);
        paymentRepository.save(payment);
        refund.applyStatus(RefundStatus.REFUND_PROCESSING);
        refundRepository.save(refund);

        PaymentProvider provider = providerRegistry.resolve(payment.getProvider());
        ProviderResult result = provider.refund(payment.getId(), payment.getId(), refund.getAmount());

        if (result.outcome() == ProviderResult.Outcome.SUCCESS) {
            refund.setProviderReference(result.providerReference());
            refund.applyStatus(RefundStatus.REFUNDED);
            paymentStateMachine.transition(payment.getStatus(), PaymentStatus.REFUNDED);
            payment.applyStatus(PaymentStatus.REFUNDED);
        } else {
            refund.applyStatus(RefundStatus.REFUND_FAILED);
            paymentStateMachine.transition(payment.getStatus(), PaymentStatus.REFUND_FAILED);
            payment.applyStatus(PaymentStatus.REFUND_FAILED);
        }
        refundRepository.save(refund);
        paymentRepository.save(payment);
        log.info("Refund {} finished with status {}", refund.getRefundId(), refund.getStatus());

        String eventType = refund.getStatus() == RefundStatus.REFUNDED ? "refund.completed" : "refund.failed";
        webhookEventPublisher.publish(eventType, payment.getId(), payment.getMerchantId(), Map.of(
            "refundId", refund.getRefundId(),
            "amount", refund.getAmount(),
            "status", refund.getStatus().name()
        ));
    }

    @Transactional(readOnly = true)
    public List<RefundResponse> listRefundsForPayment(String paymentId) {
        return refundRepository.findByPaymentId(paymentId).stream().map(RefundResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public RefundResponse getRefund(String refundId) {
        return refundRepository.findByRefundId(refundId)
            .map(RefundResponse::from)
            .orElseThrow(() -> new RefundException("Refund not found: " + refundId));
    }
}
