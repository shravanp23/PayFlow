package com.payflow.payment;

public enum PaymentStatus {
    CREATED,
    PROCESSING,
    SUCCESS,
    FAILED,
    TIMEOUT,
    RETRYING,
    CANCELLED,
    REFUND_REQUESTED,
    REFUND_PROCESSING,
    REFUNDED,
    REFUND_FAILED
}
