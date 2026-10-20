package com.platform.technician;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TechnicianEvidenceResponse(
        UUID id,
        String originalFilename,
        String contentType,
        long sizeBytes,
        OffsetDateTime uploadedAt
) {
}
