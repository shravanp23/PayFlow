package com.payflow.auth.dto;

public record LoginResponse(
    String token,
    String tokenType,
    long expiresInMinutes,
    String email,
    String role
) {}
