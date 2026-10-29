package com.payflow.reconciliation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReconciliationItemRepository extends JpaRepository<ReconciliationItem, UUID> {
    List<ReconciliationItem> findByReconciliationRunId(UUID runId);
    List<ReconciliationItem> findByReconciliationRunIdAndStatus(UUID runId, ReconciliationItemStatus status);
}
