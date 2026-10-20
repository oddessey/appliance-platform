package com.platform.reservation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateReservationRequest(
        @NotNull UUID moveRequestId,
        @NotNull UUID quoteId,
        @NotNull OffsetDateTime scheduledStartAt,
        @NotNull OffsetDateTime scheduledEndAt,
        @Size(max = 2000) String customerMemo
) {
}
