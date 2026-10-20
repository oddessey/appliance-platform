package com.platform.technician;

import java.util.UUID;

public record TechnicianAdminSummary(
        UUID id,
        UUID userId,
        String name,
        String email,
        String phone,
        String approvalStatus,
        boolean active,
        String businessName,
        String licenseInfo,
        String serviceAreas,
        String applianceTypeCodes,
        String availability
) {
}
