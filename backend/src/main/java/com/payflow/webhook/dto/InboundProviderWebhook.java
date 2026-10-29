package com.payflow.webhook.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record InboundProviderWebhook(
    @NotBlank String eventId,
    @NotBlank String eventType,
    @NotBlank String paymentId,
    String timestamp,
    Map<String, Object> data
) {}
