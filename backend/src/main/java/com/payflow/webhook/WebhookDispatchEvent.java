package com.payflow.webhook;

import java.time.Instant;
import java.util.UUID;

/** Kafka message payload for the webhook.events topic — one per outbound delivery attempt to schedule. */
public record WebhookDispatchEvent(
    String eventId,
    String eventType,
    String paymentId,
    UUID merchantId,
    String payloadJson,
    Instant occurredAt
) {}
