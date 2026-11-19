package com.platform.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentPrepareResponse(
        UUID paymentId,
        UUID reservationId,
        String orderId,
        String orderName,
        BigDecimal amount,
        String clientKey,
        String customerName,
        String successUrl,
        String failUrl
) {}
