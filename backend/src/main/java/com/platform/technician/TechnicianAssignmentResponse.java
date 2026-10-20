package com.platform.technician;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TechnicianAssignmentResponse(
        UUID id,
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
    public TechnicianAssignmentResponse(
            UUID id,
            String status,
            String reservationNo,
            OffsetDateTime scheduledStartAt,
            OffsetDateTime scheduledEndAt,
            String customerName,
            String customerPhone,
            String originAddress,
            String destinationAddress,
            String applianceTypes,
            String customerMemo
    ) {
        this(id, status, reservationNo, scheduledStartAt, scheduledEndAt, customerName, customerPhone,
                originAddress, destinationAddress, applianceTypes, customerMemo, null, null);
    }
}
