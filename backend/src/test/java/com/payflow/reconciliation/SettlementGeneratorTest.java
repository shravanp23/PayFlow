package com.payflow.reconciliation;

import com.payflow.payment.Payment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Payment IDs below were chosen so that Math.floorMod(id.hashCode(), 20) lands
 * in a specific bucket of SettlementGenerator (buckets 0-14=MATCHED, 15=AMOUNT_MISMATCH,
 * 16=STATUS_MISMATCH, 17=MISSING_PROVIDER, 18=DUPLICATE, 19=MISSING_INTERNAL-trigger),
 * so this test deterministically exercises every category without relying on chance.
 */
class SettlementGeneratorTest {

    private final SettlementGenerator generator = new SettlementGenerator();

    private Payment payment(String id, String amount) {
        return Payment.create(id, new BigDecimal(amount), "INR", UUID.randomUUID(), "UPI", "MockUPIProvider", null, null, null);
    }

    @Test
    void bucket15ProducesAmountMismatch() {
        Payment p = payment("PAY_TEST0002", "1000.00"); // bucket 15
        List<ProviderTransaction> result = generator.generate(UUID.randomUUID(), List.of(p));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAmount()).isEqualByComparingTo("1010.00");
        assertThat(result.get(0).getPaymentId()).isEqualTo(p.getId());
    }

    @Test
    void bucket16ProducesStatusMismatch() {
        Payment p = payment("PAY_TEST0003", "500.00"); // bucket 16
        List<ProviderTransaction> result = generator.generate(UUID.randomUUID(), List.of(p));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("PROCESSING");
    }

    @Test
    void bucket17ProducesMissingProvider() {
        Payment p = payment("PAY_TEST0004", "500.00"); // bucket 17
        List<ProviderTransaction> result = generator.generate(UUID.randomUUID(), List.of(p));
        assertThat(result).isEmpty();
    }

    @Test
    void bucket18ProducesDuplicate() {
        Payment p = payment("PAY_TEST0005", "500.00"); // bucket 18
        List<ProviderTransaction> result = generator.generate(UUID.randomUUID(), List.of(p));
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(tx -> tx.getPaymentId().equals(p.getId()));
    }

    @Test
    void bucket19ProducesOrphanMissingInternalRecord() {
        Payment p = payment("PAY_TEST0006", "500.00"); // bucket 19
        List<ProviderTransaction> result = generator.generate(UUID.randomUUID(), List.of(p));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPaymentId()).isNull(); // orphan, no internal match
    }

    @Test
    void mostBucketsProduceCleanMatch() {
        Payment p = payment("PAY_TEST0007", "500.00"); // bucket 0
        List<ProviderTransaction> result = generator.generate(UUID.randomUUID(), List.of(p));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAmount()).isEqualByComparingTo("500.00");
        assertThat(result.get(0).getStatus()).isEqualTo("SUCCESS");
    }
}
