package com.payflow.reconciliation;

import com.payflow.common.IdGenerator;
import com.payflow.payment.Payment;
import com.payflow.payment.PaymentRepository;
import com.payflow.payment.PaymentStatus;
import com.payflow.reconciliation.dto.ReconciliationItemResponse;
import com.payflow.reconciliation.dto.ReconciliationRunDetailResponse;
import com.payflow.reconciliation.dto.ReconciliationSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Matching algorithm:
 * 1. Generate the simulated settlement dataset from current SUCCESS payments (SettlementGenerator).
 * 2. Group provider transactions by paymentId. A paymentId with >1 provider transaction is DUPLICATE
 *    for every transaction beyond the first.
 * 3. For each internal SUCCESS payment: find its (first) provider transaction by paymentId.
 *    - none found -> MISSING_PROVIDER
 *    - amount differs -> AMOUNT_MISMATCH
 *    - provider status != SUCCESS -> STATUS_MISMATCH
 *    - otherwise -> MATCHED
 * 4. Any provider transaction whose paymentId is null or doesn't match a known internal
 *    payment -> MISSING_INTERNAL.
 */
@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);

    private final PaymentRepository paymentRepository;
    private final ProviderTransactionRepository providerTransactionRepository;
    private final ReconciliationRunRepository reconciliationRunRepository;
    private final ReconciliationItemRepository reconciliationItemRepository;
    private final SettlementGenerator settlementGenerator;

    @Transactional
    public ReconciliationSummaryResponse runReconciliation() {
        ReconciliationRun run = reconciliationRunRepository.save(ReconciliationRun.start(IdGenerator.generate("RECON")));
        log.info("Reconciliation run {} started", run.getRunId());

        List<Payment> successfulPayments = fetchAllSuccessfulPayments();
        List<ProviderTransaction> settlementRecords = settlementGenerator.generate(run.getId(), successfulPayments);
        providerTransactionRepository.saveAll(settlementRecords);

        Map<String, List<ProviderTransaction>> byPaymentId = new HashMap<>();
        List<ProviderTransaction> orphanRecords = new ArrayList<>();
        for (ProviderTransaction tx : settlementRecords) {
            if (tx.getPaymentId() == null) {
                orphanRecords.add(tx);
            } else {
                byPaymentId.computeIfAbsent(tx.getPaymentId(), k -> new ArrayList<>()).add(tx);
            }
        }

        List<ReconciliationItem> items = new ArrayList<>();

        for (Payment payment : successfulPayments) {
            List<ProviderTransaction> matches = byPaymentId.getOrDefault(payment.getId(), List.of());

            if (matches.isEmpty()) {
                items.add(ReconciliationItem.create(run.getId(), payment.getId(), null,
                    ReconciliationItemStatus.MISSING_PROVIDER, payment.getAmount(), null,
                    payment.getStatus().name(), null));
                continue;
            }

            ProviderTransaction primary = matches.get(0);
            if (primary.getAmount().compareTo(payment.getAmount()) != 0) {
                items.add(ReconciliationItem.create(run.getId(), payment.getId(), primary.getProviderReference(),
                    ReconciliationItemStatus.AMOUNT_MISMATCH, payment.getAmount(), primary.getAmount(),
                    payment.getStatus().name(), primary.getStatus()));
            } else if (!"SUCCESS".equals(primary.getStatus())) {
                items.add(ReconciliationItem.create(run.getId(), payment.getId(), primary.getProviderReference(),
                    ReconciliationItemStatus.STATUS_MISMATCH, payment.getAmount(), primary.getAmount(),
                    payment.getStatus().name(), primary.getStatus()));
            } else {
                items.add(ReconciliationItem.create(run.getId(), payment.getId(), primary.getProviderReference(),
                    ReconciliationItemStatus.MATCHED, payment.getAmount(), primary.getAmount(),
                    payment.getStatus().name(), primary.getStatus()));
            }

            // Every extra settlement record beyond the first for the same payment is a duplicate.
            for (int i = 1; i < matches.size(); i++) {
                ProviderTransaction dup = matches.get(i);
                items.add(ReconciliationItem.create(run.getId(), payment.getId(), dup.getProviderReference(),
                    ReconciliationItemStatus.DUPLICATE, payment.getAmount(), dup.getAmount(),
                    payment.getStatus().name(), dup.getStatus()));
            }
        }

        for (ProviderTransaction orphan : orphanRecords) {
            items.add(ReconciliationItem.create(run.getId(), null, orphan.getProviderReference(),
                ReconciliationItemStatus.MISSING_INTERNAL, null, orphan.getAmount(), null, orphan.getStatus()));
        }

        reconciliationItemRepository.saveAll(items);

        for (ReconciliationItem item : items) {
            switch (item.getStatus()) {
                case MATCHED -> run.setMatchedCount(run.getMatchedCount() + 1);
                case AMOUNT_MISMATCH, STATUS_MISMATCH -> run.setMismatchedCount(run.getMismatchedCount() + 1);
                case MISSING_INTERNAL, MISSING_PROVIDER -> run.setMissingCount(run.getMissingCount() + 1);
                case DUPLICATE -> run.setDuplicateCount(run.getDuplicateCount() + 1);
            }
        }
        run.complete();
        reconciliationRunRepository.save(run);

        log.info("Reconciliation run {} completed: matched={} mismatched={} missing={} duplicates={}",
            run.getRunId(), run.getMatchedCount(), run.getMismatchedCount(), run.getMissingCount(), run.getDuplicateCount());

        return toSummary(run);
    }

    @Transactional(readOnly = true)
    public ReconciliationRunDetailResponse getRun(String runId) {
        ReconciliationRun run = reconciliationRunRepository.findByRunId(runId)
            .orElseThrow(() -> new IllegalArgumentException("Reconciliation run not found: " + runId));
        List<ReconciliationItemResponse> items = reconciliationItemRepository.findByReconciliationRunId(run.getId())
            .stream().map(ReconciliationItemResponse::from).toList();
        return new ReconciliationRunDetailResponse(toSummary(run), items);
    }

    private List<Payment> fetchAllSuccessfulPayments() {
        // Pulled in pages to avoid loading an unbounded result set into memory on large datasets.
        return paymentRepository.findAll(PageRequest.of(0, 5000)).stream()
            .filter(p -> p.getStatus() == PaymentStatus.SUCCESS
                || p.getStatus() == PaymentStatus.REFUND_REQUESTED
                || p.getStatus() == PaymentStatus.REFUND_PROCESSING
                || p.getStatus() == PaymentStatus.REFUNDED)
            .toList();
    }

    private ReconciliationSummaryResponse toSummary(ReconciliationRun run) {
        return new ReconciliationSummaryResponse(run.getRunId(), run.getStatus(), run.getMatchedCount(),
            run.getMismatchedCount(), run.getMissingCount(), run.getDuplicateCount(), run.getStartedAt(), run.getCompletedAt());
    }
}
