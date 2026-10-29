package com.payflow.payment;

/**
 * Thrown whenever code attempts to move a Payment to a status that is not
 * reachable from its current status. This is the ONLY place that should ever
 * reject a transition — see {@link PaymentStateMachine}.
 */
public class InvalidStateTransitionException extends RuntimeException {

    private final PaymentStatus from;
    private final PaymentStatus to;

    public InvalidStateTransitionException(PaymentStatus from, PaymentStatus to) {
        super("Cannot transition payment from %s to %s".formatted(from, to));
        this.from = from;
        this.to = to;
    }

    public PaymentStatus getFrom() {
        return from;
    }

    public PaymentStatus getTo() {
        return to;
    }
}
