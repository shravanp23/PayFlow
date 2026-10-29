package com.payflow.payment.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreatePaymentRequest(

    @NotNull @DecimalMin(value = "1.00", message = "amount must be at least 1.00")
    BigDecimal amount,

    @NotBlank @Size(min = 3, max = 3, message = "currency must be a 3-letter ISO code")
    String currency,

    @NotBlank
    String paymentMethod,

    @NotNull
    java.util.UUID merchantId,

    String customerReference,

    String description,

    String provider
) {}
