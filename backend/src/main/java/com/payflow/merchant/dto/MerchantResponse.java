package com.payflow.merchant.dto;

import com.payflow.merchant.Merchant;

import java.time.Instant;
import java.util.UUID;

public record MerchantResponse(
    UUID merchantId,
    String name,
    String email,
    String status,
    String webhookUrl,
    Instant createdAt
) {
    public static MerchantResponse from(Merchant m) {
        return new MerchantResponse(m.getId(), m.getName(), m.getEmail(), m.getStatus(), m.getWebhookUrl(), m.getCreatedAt());
    }
}
