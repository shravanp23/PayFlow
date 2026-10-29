package com.payflow.reconciliation;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_items", indexes = {
    @Index(name = "idx_reconciliation_items_run_id", columnList = "reconciliation_run_id"),
    @Index(name = "idx_reconciliation_items_status", columnList = "status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReconciliationItem {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "reconciliation_run_id", nullable = false)
    private UUID reconciliationRunId;

    @Column(name = "payment_id")
    private String paymentId;

    @Column(name = "provider_reference")
    private String providerReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReconciliationItemStatus status;

    @Column(name = "internal_amount", precision = 19, scale = 2)
    private BigDecimal internalAmount;

    @Column(name = "provider_amount", precision = 19, scale = 2)
    private BigDecimal providerAmount;

    @Column(name = "internal_status", length = 32)
    private String internalStatus;

    @Column(name = "provider_status", length = 32)
    private String providerStatus;

    public static ReconciliationItem create(UUID runId, String paymentId, String providerReference,
                                             ReconciliationItemStatus status,
                                             BigDecimal internalAmount, BigDecimal providerAmount,
                                             String internalStatus, String providerStatus) {
        ReconciliationItem item = new ReconciliationItem();
        item.reconciliationRunId = runId;
        item.paymentId = paymentId;
        item.providerReference = providerReference;
        item.status = status;
        item.internalAmount = internalAmount;
        item.providerAmount = providerAmount;
        item.internalStatus = internalStatus;
        item.providerStatus = providerStatus;
        return item;
    }
}
