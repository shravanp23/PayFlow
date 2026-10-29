package com.payflow.webhook;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Signs outbound webhook payloads with HMAC-SHA256 so merchants can verify
 * a delivery genuinely came from PayFlow. The signature is sent in the
 * `X-PayFlow-Signature` header; merchants recompute it over the raw body
 * using their shared secret and compare.
 */
@Component
public class HmacSigner {

    private final String secret;

    public HmacSigner(@Value("${payflow.webhook.secret}") String secret) {
        this.secret = secret;
    }

    public String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign webhook payload", e);
        }
    }

    /** Constant-time comparison to avoid timing side-channel attacks on signature checks. */
    public boolean verify(String payload, String providedSignature) {
        if (providedSignature == null) return false;
        String expected = sign(payload);
        return MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8),
            providedSignature.getBytes(StandardCharsets.UTF_8)
        );
    }
}
