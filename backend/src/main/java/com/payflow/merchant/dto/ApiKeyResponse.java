package com.payflow.merchant.dto;

import java.time.Instant;
import java.util.UUID;

public record ApiKeyResponse(
    UUID id,
    String environment,
    String keyPrefix,
    String rawKey, // populated ONLY on creation/rotation response; null thereafter
    boolean revoked,
    Instant createdAt
) {}
