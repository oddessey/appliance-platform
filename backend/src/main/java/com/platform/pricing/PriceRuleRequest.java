package com.platform.pricing;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PriceRuleRequest(
        @NotBlank @Size(max = 60) @Pattern(regexp = "[A-Z][A-Z0-9_]{1,59}") String ruleCode,
        @NotBlank @Size(max = 150) String name,
        @Size(max = 2000) String description,
        @Size(max = 40) String applianceTypeCode,
        @NotNull PriceConditionCode conditionCode,
        @NotNull PriceCalculationType calculationType,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @NotNull Integer priority,
        @NotNull Boolean active,
        OffsetDateTime validFrom,
        OffsetDateTime validTo
) {
}
