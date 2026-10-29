package com.platform.technician;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TechnicianAssignmentResponse(
        UUID id,
        UUID moveRequestId,
        String status,
        String reservationNo,
        OffsetDateTime scheduledStartAt,
        OffsetDateTime scheduledEndAt,
        String customerName,
        String customerPhone,
        String originAddress,
        String destinationAddress,
        String applianceTypes,
        String customerMemo,
        String quoteNo,
        BigDecimal quoteTotalAmount
) {
}
