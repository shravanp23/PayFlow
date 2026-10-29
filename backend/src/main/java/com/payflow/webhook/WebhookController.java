package com.payflow.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.webhook.dto.InboundProviderWebhook;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Simulated inbound provider callback. In production this is where a real
 * payment provider (Razorpay, Stripe, etc.) would notify PayFlow of an
 * asynchronous status change; here it's exposed so the demo/failure-simulation
 * flow can trigger it directly. Signature verification uses the same HmacSigner
 * as outbound delivery — the provider signs with a shared secret, we verify here.
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final HmacSigner hmacSigner;
    private final ObjectMapper objectMapper;

    @PostMapping("/payment")
    public ResponseEntity<Void> receiveProviderWebhook(
        @Valid @RequestBody InboundProviderWebhook webhook,
        @RequestHeader(value = "X-Provider-Signature", required = false) String signature
    ) throws com.fasterxml.jackson.core.JsonProcessingException {

        String rawBody = objectMapper.writeValueAsString(webhook);
        if (signature != null && !hmacSigner.verify(rawBody, signature)) {
            log.warn("Rejected inbound provider webhook {} — signature mismatch", webhook.eventId());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Received provider webhook eventId={} type={} paymentId={}",
            webhook.eventId(), webhook.eventType(), webhook.paymentId());

        // In a full implementation this would publish an internal event to reconcile
        // the payment's status against the provider's report (see reconciliation module).
        return ResponseEntity.ok().build();
    }
}
