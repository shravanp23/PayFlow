package com.payflow.payment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payments_status", columnList = "status"),
    @Index(name = "idx_payments_merchant", columnList = "merchant_id"),
    @Index(name = "idx_payments_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private String id; // e.g. PAY_8F29A1C4

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(name = "provider")
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentStatus status;

    @Column(name = "customer_reference")
    private String customerReference;

    @Column(name = "description")
    private String description;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version; // optimistic locking guard for concurrent state updates

    public static Payment create(String id, BigDecimal amount, String currency, UUID merchantId,
                                  String paymentMethod, String provider, String customerReference,
                                  String description, String idempotencyKey) {
        Payment p = new Payment();
        p.id = id;
        p.amount = amount;
        p.currency = currency;
        p.merchantId = merchantId;
        p.paymentMethod = paymentMethod;
        p.provider = provider;
        p.customerReference = customerReference;
        p.description = description;
        p.idempotencyKey = idempotencyKey;
        p.status = PaymentStatus.CREATED;
        Instant now = Instant.now();
        p.createdAt = now;
        p.updatedAt = now;
        return p;
    }

    public void applyStatus(PaymentStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    public void incrementRetryCount() {
        this.retryCount++;
        this.updatedAt = Instant.now();
    }
}
