package com.platform.pricing;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PricingRequestSummary(
        UUID id,
        String requestNo,
        String status,
        String customerName,
        String customerEmail,
        String applianceTypeCode,
        String applianceTypeName,
        String originAddress,
        String destinationAddress,
        OffsetDateTime desiredStartAt,
        OffsetDateTime createdAt,
        String latestQuoteStatus,
        BigDecimal latestQuoteTotalAmount
) {
}
