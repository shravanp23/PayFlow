package com.payflow.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Fixed-window rate limiter backed by Redis INCR + EXPIRE.
 * Window resets every 60 seconds per merchant. Simple and cheap; a
 * sliding-window log could replace this later if burst-at-boundary
 * behavior becomes a real problem.
 */
@Service
@RequiredArgsConstructor
public class RateLimiter {

    private final StringRedisTemplate redisTemplate;

    @Value("${payflow.ratelimit.requests-per-minute:100}")
    private int requestsPerMinute;

    public record RateLimitResult(boolean allowed, long remaining, long retryAfterSeconds) {}

    public RateLimitResult checkAndIncrement(String merchantKey) {
        String redisKey = "ratelimit:" + merchantKey + ":" + currentWindow();
        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count != null && count == 1L) {
            redisTemplate.expire(redisKey, Duration.ofSeconds(60));
        }
        long current = count == null ? 1 : count;
        boolean allowed = current <= requestsPerMinute;
        long remaining = Math.max(0, requestsPerMinute - current);
        long retryAfter = allowed ? 0 : secondsUntilNextWindow();
        return new RateLimitResult(allowed, remaining, retryAfter);
    }

    private long currentWindow() {
        return System.currentTimeMillis() / 60_000;
    }

    private long secondsUntilNextWindow() {
        long now = System.currentTimeMillis();
        long nextWindowStart = ((now / 60_000) + 1) * 60_000;
        return (nextWindowStart - now) / 1000;
    }
}
