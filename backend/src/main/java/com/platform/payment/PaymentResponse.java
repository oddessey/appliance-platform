package com.platform.payment;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID reservationId,
        String provider,
        String orderId,
        String paymentKey,
        BigDecimal amount,
        String status,
        String method,
        OffsetDateTime approvedAt,
        OffsetDateTime paidAt,
        OffsetDateTime createdAt
) {}
