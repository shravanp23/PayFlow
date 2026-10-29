package com.payflow.provider;

import java.math.BigDecimal;

/**
 * Abstraction over an external payment provider. Adding a new real provider
 * (Razorpay, Stripe, etc.) later means implementing this interface — nothing
 * else in the codebase should need to change.
 */
public interface PaymentProvider {

    String getName();

    ProviderResult authorize(String paymentId, BigDecimal amount, String currency, SimulationScenario scenario);

    ProviderResult capture(String paymentId, String providerReference);

    ProviderResult refund(String paymentId, String providerReference, BigDecimal amount);

    ProviderResult getStatus(String providerReference);
}
