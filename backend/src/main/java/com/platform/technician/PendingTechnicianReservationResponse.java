package com.platform.technician;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PendingTechnicianReservationResponse(
        UUID reservationId,
        String reservationNo,
        OffsetDateTime scheduledStartAt,
        OffsetDateTime scheduledEndAt,
        String customerName,
        String customerPhone,
        String originAddress,
        String destinationAddress,
        String applianceTypes
) {
}
