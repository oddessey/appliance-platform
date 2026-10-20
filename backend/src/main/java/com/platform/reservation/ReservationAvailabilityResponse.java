package com.platform.reservation;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReservationAvailabilityResponse(
        UUID moveRequestId,
        boolean available,
        OffsetDateTime scheduledStartAt,
        OffsetDateTime scheduledEndAt
) {
}
