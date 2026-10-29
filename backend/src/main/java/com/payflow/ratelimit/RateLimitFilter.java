package com.payflow.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Applies a per-merchant (falling back to per-IP for unauthenticated calls)
 * rate limit to /api/v1/** traffic. Runs after JwtAuthFilter so the
 * authenticated principal is available when present.
 */
@Component
@Order(10)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {

        if (!request.getRequestURI().startsWith("/api/v1/")) {
            chain.doFilter(request, response);
            return;
        }

        String key = resolveRateLimitKey(request);
        var result = rateLimiter.checkAndIncrement(key);

        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));

        if (!result.allowed()) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
            response.setContentType("application/json");
            response.getWriter().write("""
                {"status":429,"error":"RATE_LIMIT_EXCEEDED","message":"Too many requests, retry after %d seconds"}
                """.formatted(result.retryAfterSeconds()).trim());
            return;
        }

        chain.doFilter(request, response);
    }

    private String resolveRateLimitKey(HttpServletRequest request) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
            return "user:" + auth.getPrincipal();
        }
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null && !apiKey.isBlank()) {
            return "apikey:" + apiKey.substring(0, Math.min(apiKey.length(), 16));
        }
        return "ip:" + request.getRemoteAddr();
    }
}
