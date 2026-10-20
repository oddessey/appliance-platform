package com.platform.pricing;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PriceRule(
        UUID id,
        String ruleCode,
        String name,
        String description,
        String applianceTypeCode,
        PriceConditionCode conditionCode,
        PriceCalculationType calculationType,
        BigDecimal amount,
        int priority,
        boolean active,
        int version,
        OffsetDateTime validFrom,
        OffsetDateTime validTo
) {
    public PriceRuleResponse toResponse() {
        return new PriceRuleResponse(id, ruleCode, name, description, applianceTypeCode, conditionCode,
                calculationType, amount, priority, active, version, validFrom, validTo);
    }
}
