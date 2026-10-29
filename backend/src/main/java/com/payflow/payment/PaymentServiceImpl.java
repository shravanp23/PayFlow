package com.payflow.payment;

import com.payflow.common.IdGenerator;
import com.payflow.idempotency.IdempotencyService;
import com.payflow.payment.dto.CreatePaymentRequest;
import com.payflow.payment.dto.PaymentResponse;
import com.payflow.payment.event.PaymentEventPublisher;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final PaymentStateMachine stateMachine;
    private final IdempotencyService idempotencyService;
    private final PaymentEventPublisher eventPublisher;

    /**
     * Idempotency strategy, two layers deep:
     * 1. Redis-backed fast path + Postgres durable fallback, coordinated by IdempotencyService
     *    (see that class for the full explanation of the locking protocol).
     * 2. Even if both of those are bypassed somehow, the unique constraint on
     *    (merchant_id, idempotency_key) directly on the payments table (Phase 2-3) is the final
     *    safety net — createPaymentInternal catches DataIntegrityViolationException too.
     */
    @Override
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request, String idempotencyKey) {
        String paymentId = idempotencyService.executeIdempotent(
            request.merchantId(),
            idempotencyKey,
            () -> createPaymentInternal(request, idempotencyKey).getId()
        );
        return getPayment(paymentId);
    }

    private Payment createPaymentInternal(CreatePaymentRequest request, String idempotencyKey) {
        Payment payment = Payment.create(
            IdGenerator.generate("PAY"),
            request.amount(),
            request.currency().toUpperCase(),
            request.merchantId(),
            request.paymentMethod(),
            request.provider(),
            request.customerReference(),
            request.description(),
            idempotencyKey
        );

        try {
            Payment saved = paymentRepository.save(payment);
            log.info("Payment created id={} merchant={} amount={} {}", saved.getId(), saved.getMerchantId(),
                saved.getAmount(), saved.getCurrency());
            eventPublisher.publishPaymentCreated(saved); // registered for AFTER_COMMIT dispatch — see PaymentEventPublisher
            return saved;
        } catch (DataIntegrityViolationException e) {
            log.warn("Idempotency race detected for merchant={} key={}, resolving to winner",
                request.merchantId(), idempotencyKey);
            return paymentRepository.findByMerchantIdAndIdempotencyKey(request.merchantId(), idempotencyKey)
                .orElseThrow(() -> e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(String paymentId) {
        return paymentRepository.findById(paymentId)
            .map(PaymentResponse::from)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> listPayments(UUID merchantId, PaymentStatus status, Pageable pageable) {
        Page<Payment> page = status != null
            ? paymentRepository.findByMerchantIdAndStatus(merchantId, status, pageable)
            : paymentRepository.findByMerchantId(merchantId, pageable);
        return page.map(PaymentResponse::from);
    }

    @Override
    @Transactional
    public void applyTransition(String paymentId, PaymentStatus target) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        stateMachine.transition(payment.getStatus(), target); // throws InvalidStateTransitionException if illegal
        payment.applyStatus(target);
        paymentRepository.save(payment);
        log.info("Payment {} transitioned to {}", paymentId, target);
    }
}
