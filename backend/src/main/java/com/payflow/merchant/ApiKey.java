package com.payflow.merchant;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "api_keys")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiKey {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    // Never store the raw key. Only its hash, plus a short unrecoverable
    // prefix so the UI can show "pf_live_8f29...".
    @Column(name = "key_hash", nullable = false, unique = true)
    private String keyHash;

    @Column(name = "key_prefix", nullable = false)
    private String keyPrefix;

    @Column(nullable = false, length = 16)
    private String environment; // TEST or LIVE

    @Column(nullable = false)
    private boolean revoked = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public static ApiKey create(UUID merchantId, String keyHash, String keyPrefix, String environment) {
        ApiKey k = new ApiKey();
        k.merchantId = merchantId;
        k.keyHash = keyHash;
        k.keyPrefix = keyPrefix;
        k.environment = environment;
        k.createdAt = Instant.now();
        return k;
    }

    public void revoke() {
        this.revoked = true;
        this.revokedAt = Instant.now();
    }
}
