package com.platform.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CancelPaymentRequest(
        @NotBlank String cancelReason,
        @Positive BigDecimal cancelAmount
) {}
