package com.payflow.provider;

public record ProviderResult(
    Outcome outcome,
    String providerReference,
    String message
) {
    public enum Outcome {
        SUCCESS,
        FAILED,           // permanent — do not retry
        TIMEOUT,          // transient — retry
        TRANSIENT_ERROR   // transient — retry
    }

    public boolean isRetryable() {
        return outcome == Outcome.TIMEOUT || outcome == Outcome.TRANSIENT_ERROR;
    }

    public static ProviderResult success(String reference) {
        return new ProviderResult(Outcome.SUCCESS, reference, "Payment authorized and captured");
    }

    public static ProviderResult failed(String message) {
        return new ProviderResult(Outcome.FAILED, null, message);
    }

    public static ProviderResult timeout() {
        return new ProviderResult(Outcome.TIMEOUT, null, "Provider did not respond in time");
    }

    public static ProviderResult transientError(String message) {
        return new ProviderResult(Outcome.TRANSIENT_ERROR, null, message);
    }
}
