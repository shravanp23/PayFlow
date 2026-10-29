package com.payflow.refund.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateRefundRequest(
    // Null amount means "refund the full remaining captured amount".
    @DecimalMin(value = "0.01", message = "amount must be positive") BigDecimal amount,
    String reason
) {}
