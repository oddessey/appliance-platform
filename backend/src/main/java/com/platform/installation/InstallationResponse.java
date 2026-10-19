package com.platform.installation;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InstallationResponse(
        UUID id,
        String requestNo,
        String status,
        OffsetDateTime createdAt
) {
}
