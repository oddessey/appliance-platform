package com.platform.technician;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TechnicianProfileUpdateRequest(
        @NotBlank @Size(max = 150) String businessName,
        @Size(max = 2000) String licenseInfo,
        @NotEmpty @Size(max = 50) List<@NotBlank @Size(max = 30) String> serviceAreas,
        @NotEmpty @Size(max = 100) List<@NotBlank @Size(max = 40) String> applianceTypeCodes,
        @NotEmpty @Size(max = 50) List<@Valid TechnicianAvailabilityRequest> availability
) {
}
