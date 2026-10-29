package com.payflow.payment;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Single source of truth for which Payment status transitions are legal.
 * No other class in the codebase should mutate Payment.status directly —
 * always route through {@link #transition}.
 *
 * Transition graph:
 *
 *   CREATED -> PROCESSING -> SUCCESS
 *   PROCESSING -> FAILED
 *   PROCESSING -> TIMEOUT -> RETRYING -> PROCESSING
 *   CREATED -> CANCELLED
 *   SUCCESS -> REFUND_REQUESTED -> REFUND_PROCESSING -> REFUNDED
 *   REFUND_PROCESSING -> REFUND_FAILED
 */
@Component
public class PaymentStateMachine {

    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED = new EnumMap<>(PaymentStatus.class);

    static {
        ALLOWED.put(PaymentStatus.CREATED, EnumSet.of(PaymentStatus.PROCESSING, PaymentStatus.CANCELLED));
        ALLOWED.put(PaymentStatus.PROCESSING, EnumSet.of(PaymentStatus.SUCCESS, PaymentStatus.FAILED, PaymentStatus.TIMEOUT));
        ALLOWED.put(PaymentStatus.TIMEOUT, EnumSet.of(PaymentStatus.RETRYING, PaymentStatus.FAILED));
        ALLOWED.put(PaymentStatus.RETRYING, EnumSet.of(PaymentStatus.PROCESSING, PaymentStatus.FAILED));
        ALLOWED.put(PaymentStatus.SUCCESS, EnumSet.of(PaymentStatus.REFUND_REQUESTED));
        ALLOWED.put(PaymentStatus.REFUND_REQUESTED, EnumSet.of(PaymentStatus.REFUND_PROCESSING));
        ALLOWED.put(PaymentStatus.REFUND_PROCESSING, EnumSet.of(PaymentStatus.REFUNDED, PaymentStatus.REFUND_FAILED));
        ALLOWED.put(PaymentStatus.REFUND_FAILED, EnumSet.of(PaymentStatus.REFUND_REQUESTED));
        // Terminal states — no outgoing transitions:
        ALLOWED.put(PaymentStatus.FAILED, EnumSet.noneOf(PaymentStatus.class));
        ALLOWED.put(PaymentStatus.CANCELLED, EnumSet.noneOf(PaymentStatus.class));
        ALLOWED.put(PaymentStatus.REFUNDED, EnumSet.noneOf(PaymentStatus.class));
    }

    /** Returns true if moving from `current` to `target` is a legal transition. */
    public boolean canTransition(PaymentStatus current, PaymentStatus target) {
        return ALLOWED.getOrDefault(current, Set.of()).contains(target);
    }

    /**
     * Validates the transition and throws if illegal. Does not mutate anything —
     * callers apply `target` to the entity themselves after this passes, inside
     * the same transaction.
     */
    public void transition(PaymentStatus current, PaymentStatus target) {
        if (!canTransition(current, target)) {
            throw new InvalidStateTransitionException(current, target);
        }
    }
}
