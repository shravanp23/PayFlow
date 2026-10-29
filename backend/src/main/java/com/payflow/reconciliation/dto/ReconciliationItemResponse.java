package com.payflow.reconciliation.dto;

import com.payflow.reconciliation.ReconciliationItem;
import com.payflow.reconciliation.ReconciliationItemStatus;

import java.math.BigDecimal;

public record ReconciliationItemResponse(
    String paymentId,
    String providerReference,
    ReconciliationItemStatus status,
    BigDecimal internalAmount,
    BigDecimal providerAmount,
    String internalStatus,
    String providerStatus
) {
    public static ReconciliationItemResponse from(ReconciliationItem item) {
        return new ReconciliationItemResponse(item.getPaymentId(), item.getProviderReference(), item.getStatus(),
            item.getInternalAmount(), item.getProviderAmount(), item.getInternalStatus(), item.getProviderStatus());
    }
}
