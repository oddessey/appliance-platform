package com.platform.reservation;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        String reservationNo,
        UUID moveRequestId,
        String requestNo,
        UUID quoteId,
        String quoteNo,
        String status,
        OffsetDateTime scheduledStartAt,
        OffsetDateTime scheduledEndAt,
        String customerMemo,
        OffsetDateTime confirmedAt,
        OffsetDateTime cancelledAt,
        OffsetDateTime createdAt,
        String customerName,
        String customerPhone,
        String originAddress,
        String destinationAddress,
        BigDecimal quoteTotalAmount,
        String technicianAssignmentStatus,
        String technicianName
) {
}
