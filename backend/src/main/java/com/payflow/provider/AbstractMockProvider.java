package com.payflow.provider;

import com.payflow.common.IdGenerator;

import java.math.BigDecimal;

/**
 * Shared deterministic scenario handling for mock providers. Concrete
 * subclasses only supply their name/reference prefix — behavior per
 * scenario is identical and fully deterministic (no randomness), so tests
 * can assert exact outcomes.
 */
public abstract class AbstractMockProvider implements PaymentProvider {

    protected abstract String referencePrefix();

    @Override
    public ProviderResult authorize(String paymentId, BigDecimal amount, String currency, SimulationScenario scenario) {
        SimulationScenario effective = scenario == null ? SimulationScenario.NORMAL : scenario;
        return switch (effective) {
            case NORMAL -> ProviderResult.success(IdGenerator.generate(referencePrefix()));
            case SLOW_PROVIDER -> {
                simulateLatency(3000);
                yield ProviderResult.success(IdGenerator.generate(referencePrefix()));
            }
            case PROVIDER_TIMEOUT -> ProviderResult.timeout();
            case TRANSIENT_FAILURE -> ProviderResult.transientError("Transient upstream error, safe to retry");
            case PROVIDER_FAILURE -> ProviderResult.failed("Payment declined by provider");
        };
    }

    @Override
    public ProviderResult capture(String paymentId, String providerReference) {
        return ProviderResult.success(providerReference);
    }

    @Override
    public ProviderResult refund(String paymentId, String providerReference, BigDecimal amount) {
        return ProviderResult.success(IdGenerator.generate("RFD"));
    }

    @Override
    public ProviderResult getStatus(String providerReference) {
        return ProviderResult.success(providerReference);
    }

    private void simulateLatency(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
