package com.payflow.payment.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Kafka message payload for the payment.created topic. */
public record PaymentCreatedEvent(
    String paymentId,
    UUID merchantId,
    BigDecimal amount,
    String currency,
    String paymentMethod,
    String provider,
    String correlationId,
    Instant occurredAt
) {}
