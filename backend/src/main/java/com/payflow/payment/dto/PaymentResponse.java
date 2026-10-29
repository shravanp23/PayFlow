package com.payflow.payment.dto;

import com.payflow.payment.Payment;
import com.payflow.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
    String paymentId,
    PaymentStatus status,
    BigDecimal amount,
    String currency,
    UUID merchantId,
    String paymentMethod,
    String provider,
    String description,
    int retryCount,
    Instant createdAt,
    Instant updatedAt
) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(
            p.getId(), p.getStatus(), p.getAmount(), p.getCurrency(), p.getMerchantId(),
            p.getPaymentMethod(), p.getProvider(), p.getDescription(), p.getRetryCount(),
            p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}
