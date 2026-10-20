package com.platform.installation;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminInstallationSummary(
        UUID id,
        String requestNo,
        String customerName,
        String customerEmail,
        String status,
        String applianceSummary,
        String originAddress,
        String destinationAddress,
        OffsetDateTime desiredStartAt,
        OffsetDateTime submittedAt,
        OffsetDateTime createdAt,
        UUID reservationId,
        String reservationNo,
        String reservationStatus,
        String technicianAssignmentStatus,
        UUID technicianProfileId,
        String technicianName,
        AdminAiAnalysisSummary latestAnalysis
) {
}
