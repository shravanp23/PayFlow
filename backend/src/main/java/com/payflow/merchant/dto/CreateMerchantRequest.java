package com.payflow.merchant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateMerchantRequest(
    @NotBlank String name,
    @NotBlank @Email String email
) {}
