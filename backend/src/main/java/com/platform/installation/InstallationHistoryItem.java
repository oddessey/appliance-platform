package com.platform.installation;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InstallationHistoryItem(
        UUID id,
        String requestNo,
        String status,
        String applianceTypeCode,
        String applianceTypeName,
        String originAddress,
        String destinationAddress,
        OffsetDateTime desiredStartAt,
        OffsetDateTime createdAt
) {
}
