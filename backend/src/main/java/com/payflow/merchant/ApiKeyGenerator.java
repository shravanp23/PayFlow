package com.payflow.merchant;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generates raw API keys (shown to the developer exactly once) and computes
 * their SHA-256 hash for storage/lookup. Raw keys are never persisted.
 */
@Component
public class ApiKeyGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    public record GeneratedKey(String rawKey, String hash, String prefix) {}

    public GeneratedKey generate(String environment) {
        byte[] randomBytes = new byte[32];
        RANDOM.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        String envTag = "LIVE".equalsIgnoreCase(environment) ? "live" : "test";
        String rawKey = "pf_" + envTag + "_" + token;
        String hash = sha256(rawKey);
        String prefix = rawKey.substring(0, Math.min(rawKey.length(), 12)) + "...";

        return new GeneratedKey(rawKey, hash, prefix);
    }

    public String hash(String rawKey) {
        return sha256(rawKey);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
