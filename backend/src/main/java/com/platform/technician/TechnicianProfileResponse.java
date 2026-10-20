package com.platform.technician;

import java.util.List;
import java.util.UUID;

public record TechnicianProfileResponse(
        UUID id,
        String approvalStatus,
        boolean active,
        String businessName,
        String licenseInfo,
        List<String> serviceAreas,
        List<String> applianceTypeCodes,
        List<TechnicianAvailabilityResponse> availability
) {
}
