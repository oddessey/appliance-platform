package com.platform.installation;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminAiAnalysisSummary(
        UUID id,
        String status,
        String provider,
        String modelName,
        BigDecimal confidence,
        String resultJson,
        String errorCode,
        String errorMessage,
        OffsetDateTime createdAt
) {
}
