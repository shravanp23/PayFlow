package com.payflow.refund;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refunds", indexes = {
    @Index(name = "idx_refunds_payment_id", columnList = "payment_id")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Refund {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "refund_id", nullable = false, unique = true)
    private String refundId; // e.g. RFD_8F29A1C4

    @Column(name = "payment_id", nullable = false)
    private String paymentId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RefundStatus status;

    @Column(name = "provider_reference")
    private String providerReference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static Refund create(String refundId, String paymentId, BigDecimal amount, String reason) {
        Refund r = new Refund();
        r.refundId = refundId;
        r.paymentId = paymentId;
        r.amount = amount;
        r.reason = reason;
        r.status = RefundStatus.REFUND_REQUESTED;
        Instant now = Instant.now();
        r.createdAt = now;
        r.updatedAt = now;
        return r;
    }

    public void applyStatus(RefundStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }
}
