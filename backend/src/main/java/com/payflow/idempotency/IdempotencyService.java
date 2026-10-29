package com.payflow.idempotency;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Idempotency coordination for payment creation.
 *
 * Fast path: Redis SET key value NX EX <ttl> is atomic — exactly one caller
 * can "win" the key within the TTL window, making it a cheap, low-latency
 * first-writer-wins lock without touching Postgres under contention.
 *
 * Durable path: Postgres remains the source of truth. A unique constraint on
 * (merchant_id, idempotency_key) means correctness never depends on Redis
 * being up — if Redis is unavailable or the key expired, the DB constraint
 * still prevents a duplicate payment; we just fall through to a slower path.
 */
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private static final Duration REDIS_TTL = Duration.ofHours(24);
    private static final String KEY_PREFIX = "idempotency:";

    private final StringRedisTemplate redisTemplate;
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    /**
     * Executes `createFn` under idempotency protection. If a payment already exists
     * for this (merchantId, idempotencyKey), returns its ID instead of calling createFn.
     */
    @Transactional
    public String executeIdempotent(UUID merchantId, String idempotencyKey, Supplier<String> createFn) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return createFn.get(); // no idempotency requested — caller accepts duplicate risk
        }

        String redisKey = redisKey(merchantId, idempotencyKey);

        // 1. Redis fast path: try to acquire the key atomically.
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(redisKey, "PENDING", REDIS_TTL);

        if (Boolean.FALSE.equals(acquired)) {
            // Someone else holds/held this key. Check what they stored.
            String cached = redisTemplate.opsForValue().get(redisKey);
            if (cached != null && !"PENDING".equals(cached)) {
                log.info("Idempotency Redis hit merchant={} key={} -> {}", merchantId, idempotencyKey, cached);
                return cached;
            }
            // Still PENDING (in-flight) or Redis lost the value — fall through to the durable DB check.
        }

        Optional<IdempotencyKeyRecord> existing =
            idempotencyKeyRepository.findByMerchantIdAndIdempotencyKey(merchantId, idempotencyKey);
        if (existing.isPresent()) {
            String paymentId = existing.get().getPaymentId();
            redisTemplate.opsForValue().set(redisKey, paymentId, REDIS_TTL);
            return paymentId;
        }

        // 2. We hold the Redis lock (or Redis was unavailable) — attempt the real creation.
        String paymentId = createFn.get();

        try {
            idempotencyKeyRepository.save(IdempotencyKeyRecord.create(merchantId, idempotencyKey, paymentId));
        } catch (DataIntegrityViolationException e) {
            // Lost the DB race despite winning/bypassing Redis — resolve to the actual winner.
            paymentId = idempotencyKeyRepository.findByMerchantIdAndIdempotencyKey(merchantId, idempotencyKey)
                .map(IdempotencyKeyRecord::getPaymentId)
                .orElseThrow(() -> e);
        }

        redisTemplate.opsForValue().set(redisKey, paymentId, REDIS_TTL);
        return paymentId;
    }

    private String redisKey(UUID merchantId, String idempotencyKey) {
        return KEY_PREFIX + merchantId + ":" + idempotencyKey;
    }
}
