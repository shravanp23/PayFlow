package com.payflow.payment;

import com.payflow.payment.dto.CreatePaymentRequest;
import com.payflow.payment.dto.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PaymentService {

    /**
     * Creates a payment, or if a payment already exists for this merchant+idempotencyKey,
     * returns the original result instead of creating a duplicate.
     */
    PaymentResponse createPayment(CreatePaymentRequest request, String idempotencyKey);

    PaymentResponse getPayment(String paymentId);

    Page<PaymentResponse> listPayments(UUID merchantId, PaymentStatus status, Pageable pageable);

    /** Moves a payment through PROCESSING -> SUCCESS/FAILED/TIMEOUT via the state machine. */
    void applyTransition(String paymentId, PaymentStatus target);
}
