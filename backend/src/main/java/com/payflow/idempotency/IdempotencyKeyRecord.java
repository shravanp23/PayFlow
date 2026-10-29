package com.payflow.idempotency;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys", uniqueConstraints = {
    @UniqueConstraint(name = "uq_idempotency_merchant_key", columnNames = {"merchant_id", "idempotency_key"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyKeyRecord {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "payment_id", nullable = false)
    private String paymentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static IdempotencyKeyRecord create(UUID merchantId, String idempotencyKey, String paymentId) {
        IdempotencyKeyRecord r = new IdempotencyKeyRecord();
        r.merchantId = merchantId;
        r.idempotencyKey = idempotencyKey;
        r.paymentId = paymentId;
        r.createdAt = Instant.now();
        return r;
    }
}
