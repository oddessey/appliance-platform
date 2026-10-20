package com.platform.technician;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectAssignmentRequest(@NotBlank @Size(max = 500) String reason) {
}
