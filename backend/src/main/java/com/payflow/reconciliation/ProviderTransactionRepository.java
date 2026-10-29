package com.payflow.reconciliation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProviderTransactionRepository extends JpaRepository<ProviderTransaction, UUID> {
    List<ProviderTransaction> findByReconciliationRunId(UUID runId);
}
