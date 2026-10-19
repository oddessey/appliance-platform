package com.example.platform.installation;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record CreateInstallationRequest(
        @NotBlank @Size(max = 40) String applianceTypeCode,
        @NotBlank @Size(max = 500) String originAddress,
        @NotBlank @Size(max = 500) String destinationAddress,
        @NotNull @Future OffsetDateTime desiredStartAt
) {
}
