package com.payflow.payment;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentStateMachineTest {

    private final PaymentStateMachine machine = new PaymentStateMachine();

    @ParameterizedTest
    @CsvSource({
        "CREATED, PROCESSING",
        "CREATED, CANCELLED",
        "PROCESSING, SUCCESS",
        "PROCESSING, FAILED",
        "PROCESSING, TIMEOUT",
        "TIMEOUT, RETRYING",
        "TIMEOUT, FAILED",
        "RETRYING, PROCESSING",
        "SUCCESS, REFUND_REQUESTED",
        "REFUND_REQUESTED, REFUND_PROCESSING",
        "REFUND_PROCESSING, REFUNDED",
        "REFUND_PROCESSING, REFUND_FAILED",
        "REFUND_FAILED, REFUND_REQUESTED"
    })
    void allowsValidTransitions(PaymentStatus from, PaymentStatus to) {
        assertThat(machine.canTransition(from, to)).isTrue();
        machine.transition(from, to); // should not throw
    }

    @ParameterizedTest
    @CsvSource({
        "REFUNDED, PROCESSING",
        "FAILED, PROCESSING",
        "CANCELLED, PROCESSING",
        "CREATED, SUCCESS",
        "CREATED, REFUND_REQUESTED",
        "SUCCESS, PROCESSING",
        "SUCCESS, FAILED",
        "PROCESSING, REFUND_REQUESTED",
        "REFUND_REQUESTED, REFUNDED"
    })
    void rejectsInvalidTransitions(PaymentStatus from, PaymentStatus to) {
        assertThat(machine.canTransition(from, to)).isFalse();
        assertThatThrownBy(() -> machine.transition(from, to))
            .isInstanceOf(InvalidStateTransitionException.class)
            .hasMessageContaining(from.toString())
            .hasMessageContaining(to.toString());
    }

    @Test
    void terminalStatesHaveNoOutgoingTransitions() {
        for (PaymentStatus terminal : new PaymentStatus[]{PaymentStatus.FAILED, PaymentStatus.CANCELLED, PaymentStatus.REFUNDED}) {
            for (PaymentStatus target : PaymentStatus.values()) {
                assertThat(machine.canTransition(terminal, target))
                    .as("%s should never transition to %s", terminal, target)
                    .isFalse();
            }
        }
    }
}
