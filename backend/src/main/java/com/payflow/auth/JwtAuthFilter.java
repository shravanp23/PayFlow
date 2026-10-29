package com.payflow.auth;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                var claims = jwtService.parse(token);
                var userId = jwtService.extractUserId(claims);
                var role = jwtService.extractRole(claims);

                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
                var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException | IllegalArgumentException e) {
                // Invalid/expired token: leave context unauthenticated, let Spring Security
                // reject downstream if the endpoint requires authentication.
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
