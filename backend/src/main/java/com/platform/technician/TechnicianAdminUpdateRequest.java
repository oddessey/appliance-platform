package com.platform.technician;

import jakarta.validation.constraints.NotNull;

public record TechnicianAdminUpdateRequest(
        @NotNull TechnicianApprovalStatus approvalStatus,
        boolean active
) {
}
