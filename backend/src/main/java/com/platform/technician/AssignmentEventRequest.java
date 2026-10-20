package com.platform.technician;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AssignmentEventRequest(
        @NotNull AssignmentStatus eventType,
        @Size(max = 1000) String note
) {
}
