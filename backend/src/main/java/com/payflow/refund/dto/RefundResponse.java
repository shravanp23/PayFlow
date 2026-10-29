package com.payflow.refund.dto;

import com.payflow.refund.Refund;
import com.payflow.refund.RefundStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record RefundResponse(
    String refundId,
    String paymentId,
    BigDecimal amount,
    String reason,
    RefundStatus status,
    Instant createdAt,
    Instant updatedAt
) {
    public static RefundResponse from(Refund r) {
        return new RefundResponse(r.getRefundId(), r.getPaymentId(), r.getAmount(), r.getReason(),
            r.getStatus(), r.getCreatedAt(), r.getUpdatedAt());
    }
}
