package com.platform.technician;

import java.util.UUID;

public record TechnicianCandidateResponse(
        UUID technicianProfileId,
        String name,
        String businessName,
        String phone,
        long activeAssignmentCount
) {
}
