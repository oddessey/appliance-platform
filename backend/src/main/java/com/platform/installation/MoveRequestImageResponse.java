package com.platform.installation;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MoveRequestImageResponse(
        UUID id,
        UUID moveRequestId,
        UUID moveItemId,
        String purpose,
        String originalFilename,
        String contentType,
        long sizeBytes,
        OffsetDateTime uploadedAt
) {
}
