package com.payflow.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.common.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Called by PaymentWorker/RefundService when a state change should notify the merchant
 * (payment.success, payment.failed, refund.completed, etc). Builds the event payload,
 * assigns a unique eventId (used later for delivery deduplication), and publishes to
 * webhook.events for WebhookDeliveryWorker to pick up and actually POST out.
 */
@Component
@RequiredArgsConstructor
public class WebhookEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${payflow.kafka.topics.webhook-events}")
    private String webhookEventsTopic;

    public void publish(String eventType, String paymentId, UUID merchantId, Map<String, Object> data) {
        String eventId = IdGenerator.generate("evt");
        Instant now = Instant.now();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", eventId);
        payload.put("eventType", eventType);
        payload.put("paymentId", paymentId);
        payload.put("timestamp", now.toString());
        payload.put("data", data);

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize webhook payload", e);
        }

        WebhookDispatchEvent event = new WebhookDispatchEvent(eventId, eventType, paymentId, merchantId, payloadJson, now);
        var message = MessageBuilder.withPayload(event)
            .setHeader(KafkaHeaders.TOPIC, webhookEventsTopic)
            .setHeader(KafkaHeaders.KEY, eventId)
            .build();
        kafkaTemplate.send(message);
    }
}
