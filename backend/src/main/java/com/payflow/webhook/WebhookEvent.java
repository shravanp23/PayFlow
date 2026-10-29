package com.payflow.webhook;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WebhookEvent {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId; // e.g. evt_8F29A1C4 — dedupe key for both inbound and outbound

    @Column(name = "event_type", nullable = false)
    private String eventType; // e.g. payment.success

    @Column(name = "payment_id", nullable = false)
    private String paymentId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
    private String payload; // JSON, signed on delivery

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static WebhookEvent create(String eventId, String eventType, String paymentId, UUID merchantId, String payload) {
        WebhookEvent e = new WebhookEvent();
        e.eventId = eventId;
        e.eventType = eventType;
        e.paymentId = paymentId;
        e.merchantId = merchantId;
        e.payload = payload;
        e.createdAt = Instant.now();
        return e;
    }
}
