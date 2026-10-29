package com.payflow.reconciliation;

public enum ReconciliationItemStatus {
    MATCHED,
    MISSING_INTERNAL,   // exists at the provider, not in PayFlow
    MISSING_PROVIDER,   // exists in PayFlow, not at the provider
    AMOUNT_MISMATCH,
    STATUS_MISMATCH,
    DUPLICATE
}
