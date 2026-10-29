package com.payflow.reconciliation;

import com.payflow.common.IdGenerator;
import com.payflow.payment.Payment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Generates a simulated provider settlement dataset from PayFlow's own SUCCESS
 * payments. Since there's no real settlement file to ingest, this plays that
 * role for the demo/test environment while preserving the exact shape a real
 * ingestion pipeline would produce.
 *
 * Deterministic by design (bucketed on Payment.id.hashCode(), never Math.random())
 * so that every mismatch category is reliably reproducible in tests and in the
 * "Run Reconciliation" demo — a fixed ~80% of payments settle cleanly, and each
 * remaining bucket exercises exactly one mismatch category.
 */
@Component
public class SettlementGenerator {

    public List<ProviderTransaction> generate(UUID runId, List<Payment> successfulPayments) {
        List<ProviderTransaction> transactions = new ArrayList<>();

        for (Payment payment : successfulPayments) {
            int bucket = Math.floorMod(payment.getId().hashCode(), 20);

            if (bucket < 15) {
                // MATCHED: settlement exactly mirrors the internal record.
                transactions.add(ProviderTransaction.create(runId, payment.getId(),
                    IdGenerator.generate("SETL"), payment.getAmount(), "SUCCESS"));

            } else if (bucket == 15) {
                // AMOUNT_MISMATCH: provider settled a slightly different amount.
                BigDecimal skewed = payment.getAmount().add(new BigDecimal("10.00"));
                transactions.add(ProviderTransaction.create(runId, payment.getId(),
                    IdGenerator.generate("SETL"), skewed, "SUCCESS"));

            } else if (bucket == 16) {
                // STATUS_MISMATCH: provider still reports it as pending/processing.
                transactions.add(ProviderTransaction.create(runId, payment.getId(),
                    IdGenerator.generate("SETL"), payment.getAmount(), "PROCESSING"));

            } else if (bucket == 17) {
                // MISSING_PROVIDER: PayFlow shows SUCCESS, provider never reports it — omit entirely.
                continue;

            } else if (bucket == 18) {
                // DUPLICATE: provider reports the same payment twice.
                transactions.add(ProviderTransaction.create(runId, payment.getId(),
                    IdGenerator.generate("SETL"), payment.getAmount(), "SUCCESS"));
                transactions.add(ProviderTransaction.create(runId, payment.getId(),
                    IdGenerator.generate("SETL"), payment.getAmount(), "SUCCESS"));

            } else {
                // MISSING_INTERNAL: provider reports a settlement with no matching PayFlow payment
                // (this payment's own settlement is intentionally omitted, so it independently
                // shows up as MISSING_PROVIDER too — both categories get exercised by this bucket).
                transactions.add(ProviderTransaction.create(runId, null,
                    IdGenerator.generate("SETL"), payment.getAmount(), "SUCCESS"));
            }
        }

        return transactions;
    }
}
