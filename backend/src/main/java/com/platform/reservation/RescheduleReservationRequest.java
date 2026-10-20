package com.platform.reservation;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record RescheduleReservationRequest(
        @NotNull OffsetDateTime scheduledStartAt,
        @NotNull OffsetDateTime scheduledEndAt
) {
}
