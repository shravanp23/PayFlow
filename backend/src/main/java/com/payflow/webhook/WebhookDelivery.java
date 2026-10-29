package com.payflow.webhook;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_deliveries", indexes = {
    @Index(name = "idx_webhook_deliveries_event_id", columnList = "webhook_event_id"),
    @Index(name = "idx_webhook_deliveries_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WebhookDelivery {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "webhook_event_id", nullable = false)
    private UUID webhookEventId;

    @Column(name = "target_url", nullable = false)
    private String targetUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private WebhookDeliveryStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "last_response_status")
    private Integer lastResponseStatus;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static WebhookDelivery create(UUID webhookEventId, String targetUrl) {
        WebhookDelivery d = new WebhookDelivery();
        d.webhookEventId = webhookEventId;
        d.targetUrl = targetUrl;
        d.status = WebhookDeliveryStatus.PENDING;
        d.createdAt = Instant.now();
        return d;
    }

    public void recordAttempt(boolean succeeded, int responseStatus, boolean willRetry) {
        this.attemptCount++;
        this.lastResponseStatus = responseStatus;
        this.lastAttemptAt = Instant.now();
        this.status = succeeded ? WebhookDeliveryStatus.DELIVERED
            : (willRetry ? WebhookDeliveryStatus.RETRYING : WebhookDeliveryStatus.FAILED);
    }
}
