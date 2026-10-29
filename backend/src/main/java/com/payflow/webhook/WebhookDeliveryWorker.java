package com.payflow.webhook;

import com.payflow.merchant.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

/**
 * Consumes webhook.events, dedupes by eventId (WebhookEvent.eventId is unique),
 * and attempts delivery to the merchant's configured webhookUrl. Failures retry
 * with exponential backoff (mirrors the payment retry pattern) up to a configured
 * max, at which point the delivery is marked FAILED for operator visibility rather
 * than being lost silently.
 */
@Component
@RequiredArgsConstructor
public class WebhookDeliveryWorker {

    private static final Logger log = LoggerFactory.getLogger(WebhookDeliveryWorker.class);
    private static final int MAX_ATTEMPTS = 4;

    private final WebhookEventRepository webhookEventRepository;
    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final MerchantRepository merchantRepository;
    private final HmacSigner hmacSigner;
    private final RestTemplate restTemplate;

    @Value("${payflow.retry.initial-backoff-seconds:1}")
    private long initialBackoffSeconds;

    @KafkaListener(topics = "${payflow.kafka.topics.webhook-events}", groupId = "payflow-webhook-workers")
    @Transactional
    public void onWebhookEvent(WebhookDispatchEvent event) {
        // Dedup: if this eventId was already recorded, this is a re-delivery of the
        // same Kafka message (e.g. after a consumer rebalance) — do not double-send.
        Optional<WebhookEvent> existing = webhookEventRepository.findByEventId(event.eventId());
        WebhookEvent webhookEvent = existing.orElseGet(() -> webhookEventRepository.save(
            WebhookEvent.create(event.eventId(), event.eventType(), event.paymentId(), event.merchantId(), event.payloadJson())
        ));

        if (existing.isPresent() && !webhookDeliveryRepository.findByWebhookEventId(webhookEvent.getId()).isEmpty()) {
            log.info("Webhook event {} already has delivery attempts recorded, skipping duplicate dispatch", event.eventId());
            return;
        }

        String targetUrl = merchantRepository.findById(event.merchantId())
            .map(m -> m.getWebhookUrl())
            .orElse(null);

        if (targetUrl == null || targetUrl.isBlank()) {
            log.warn("Merchant {} has no webhookUrl configured, skipping delivery for event {}", event.merchantId(), event.eventId());
            return;
        }

        WebhookDelivery delivery = webhookDeliveryRepository.save(WebhookDelivery.create(webhookEvent.getId(), targetUrl));
        deliverWithRetry(delivery, event.payloadJson());
    }

    private void deliverWithRetry(WebhookDelivery delivery, String payloadJson) {
        String signature = hmacSigner.sign(payloadJson);

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            boolean success = attemptDelivery(delivery, payloadJson, signature);
            if (success) {
                delivery.recordAttempt(true, 200, false);
                webhookDeliveryRepository.save(delivery);
                log.info("Webhook delivered to {} after {} attempt(s)", delivery.getTargetUrl(), attempt);
                return;
            }

            boolean willRetry = attempt < MAX_ATTEMPTS;
            delivery.recordAttempt(false, 0, willRetry);
            webhookDeliveryRepository.save(delivery);

            if (willRetry) {
                long backoff = initialBackoffSeconds * (1L << (attempt - 1));
                log.warn("Webhook delivery to {} failed on attempt {}/{}, retrying in {}s",
                    delivery.getTargetUrl(), attempt, MAX_ATTEMPTS, backoff);
                sleep(backoff);
            } else {
                log.error("Webhook delivery to {} failed permanently after {} attempts", delivery.getTargetUrl(), MAX_ATTEMPTS);
            }
        }
    }

    private boolean attemptDelivery(WebhookDelivery delivery, String payloadJson, String signature) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("X-PayFlow-Signature", signature);
            ResponseEntity<String> response = restTemplate.postForEntity(
                delivery.getTargetUrl(), new HttpEntity<>(payloadJson, headers), String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            return false;
        }
    }

    private void sleep(long seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
