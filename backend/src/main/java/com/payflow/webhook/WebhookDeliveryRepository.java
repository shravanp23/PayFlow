package com.payflow.webhook;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {
    List<WebhookDelivery> findByWebhookEventId(UUID webhookEventId);
}
