package com.payflow.payment.event;

/**
 * Internal Spring application event, distinct from the Kafka message it eventually
 * produces. Published synchronously inside the DB transaction; a
 * @TransactionalEventListener(phase = AFTER_COMMIT) picks it up and only THEN
 * sends to Kafka — so a rolled-back transaction never results in a stray Kafka event.
 */
public record PaymentCreatedApplicationEvent(PaymentCreatedEvent payload) {}
