package com.payflow.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Executes a provider call with exponential backoff on retryable outcomes only.
 * Backoff sequence with default config (initial=1s): 1s, 2s, 4s, 8s.
 * Permanent failures (ProviderResult.Outcome.FAILED) are never retried —
 * that decision is made per-attempt by the caller inspecting the result.
 *
 * NOTE: In production this sleeps the calling (consumer) thread, which is
 * acceptable because the Kafka consumer processes one payment at a time on
 * its thread; a fully async retry (re-publishing a delayed message) is a
 * valid alternative evolution documented in docs/retry-strategy.md.
 */
@Component
public class RetryExecutor {

    private static final Logger log = LoggerFactory.getLogger(RetryExecutor.class);

    private final int maxAttempts;
    private final long initialBackoffSeconds;

    public RetryExecutor(
        @Value("${payflow.retry.max-attempts:4}") int maxAttempts,
        @Value("${payflow.retry.initial-backoff-seconds:1}") long initialBackoffSeconds
    ) {
        this.maxAttempts = maxAttempts;
        this.initialBackoffSeconds = initialBackoffSeconds;
    }

    public record RetryOutcome(ProviderResult result, int attemptsMade) {}

    public RetryOutcome execute(String paymentId, Supplier<ProviderResult> attempt) {
        ProviderResult lastResult = null;

        for (int attemptNumber = 1; attemptNumber <= maxAttempts; attemptNumber++) {
            lastResult = attempt.get();

            if (!lastResult.isRetryable()) {
                // SUCCESS or permanent FAILED — stop immediately either way.
                return new RetryOutcome(lastResult, attemptNumber);
            }

            if (attemptNumber < maxAttempts) {
                long backoffSeconds = initialBackoffSeconds * (1L << (attemptNumber - 1)); // 1,2,4,8...
                log.warn("Payment {} attempt {}/{} returned {} — retrying in {}s",
                    paymentId, attemptNumber, maxAttempts, lastResult.outcome(), backoffSeconds);
                sleep(backoffSeconds);
            } else {
                log.error("Payment {} exhausted {} attempts, last outcome {}",
                    paymentId, maxAttempts, lastResult.outcome());
            }
        }

        return new RetryOutcome(lastResult, maxAttempts);
    }

    private void sleep(long seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
