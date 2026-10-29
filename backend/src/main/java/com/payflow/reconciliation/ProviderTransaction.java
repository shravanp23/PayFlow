package com.payflow.reconciliation;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A simulated provider settlement record — what the provider "says" happened,
 * generated deterministically (see SettlementGenerator) rather than pulled from
 * a real settlement file, since PayFlow uses mock providers. Structurally this
 * plays the exact role a real settlement-file row would play in reconciliation.
 */
@Entity
@Table(name = "provider_transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProviderTransaction {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "reconciliation_run_id", nullable = false)
    private UUID reconciliationRunId;

    // Nullable: a null paymentId models a provider-side record with no matching
    // internal payment at all (MISSING_INTERNAL scenario).
    @Column(name = "payment_id")
    private String paymentId;

    @Column(name = "provider_reference", nullable = false)
    private String providerReference;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 32)
    private String status; // provider's view of the payment status, as a string

    @Column(name = "settled_at", nullable = false)
    private Instant settledAt;

    public static ProviderTransaction create(UUID runId, String paymentId, String providerReference,
                                               BigDecimal amount, String status) {
        ProviderTransaction t = new ProviderTransaction();
        t.reconciliationRunId = runId;
        t.paymentId = paymentId;
        t.providerReference = providerReference;
        t.amount = amount;
        t.status = status;
        t.settledAt = Instant.now();
        return t;
    }
}
