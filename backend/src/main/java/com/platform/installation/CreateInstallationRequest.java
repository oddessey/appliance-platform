package com.platform.installation;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CreateInstallationRequest(
        @NotBlank @Size(max = 40) String applianceTypeCode,
        @NotBlank @Size(max = 500) String originAddress,
        @NotBlank @Size(max = 500) String destinationAddress,
        @NotNull @Future OffsetDateTime desiredStartAt,
        @Min(0) @Max(10000) Integer carryingDistanceMeters,
        @Min(0) @Max(200) Integer floorCount,
        @DecimalMin("0.00") @DecimalMax("1000.00") @Digits(integer = 4, fraction = 2)
        BigDecimal additionalPipingMeters
) {
    public CreateInstallationRequest {
        carryingDistanceMeters = carryingDistanceMeters == null ? 0 : carryingDistanceMeters;
        floorCount = floorCount == null ? 0 : floorCount;
        additionalPipingMeters = additionalPipingMeters == null ? BigDecimal.ZERO : additionalPipingMeters;
    }

    public CreateInstallationRequest(
            String applianceTypeCode,
            String originAddress,
            String destinationAddress,
            OffsetDateTime desiredStartAt
    ) {
        this(applianceTypeCode, originAddress, destinationAddress, desiredStartAt, 0, 0, BigDecimal.ZERO);
    }
}
