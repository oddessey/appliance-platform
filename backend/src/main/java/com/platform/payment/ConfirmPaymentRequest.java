package com.platform.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ConfirmPaymentRequest(
        @NotBlank String paymentKey,
        @NotBlank String orderId,
        @Positive BigDecimal amount
) {}
