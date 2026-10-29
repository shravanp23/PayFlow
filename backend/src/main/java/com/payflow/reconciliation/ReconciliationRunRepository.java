package com.payflow.reconciliation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReconciliationRunRepository extends JpaRepository<ReconciliationRun, UUID> {
    Optional<ReconciliationRun> findByRunId(String runId);
}
