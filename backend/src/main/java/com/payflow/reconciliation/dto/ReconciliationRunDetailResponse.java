package com.payflow.reconciliation.dto;

import java.util.List;

public record ReconciliationRunDetailResponse(
    ReconciliationSummaryResponse summary,
    List<ReconciliationItemResponse> items
) {}
