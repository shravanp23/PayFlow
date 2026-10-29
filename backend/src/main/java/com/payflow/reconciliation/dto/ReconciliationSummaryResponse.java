package com.payflow.reconciliation.dto;

import java.time.Instant;

public record ReconciliationSummaryResponse(
    String runId,
    String status,
    int matched,
    int mismatched,
    int missing,
    int duplicates,
    Instant startedAt,
    Instant completedAt
) {}
