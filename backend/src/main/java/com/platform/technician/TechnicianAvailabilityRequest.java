package com.platform.technician;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record TechnicianAvailabilityRequest(
        @Min(1) @Max(7) int dayOfWeek,
        @NotNull @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d(:[0-5]\\d)?$") String startTime,
        @NotNull @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d(:[0-5]\\d)?$") String endTime
) {
}
