package com.platform.installation;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class InstallationRepository {

    private final InstallationMapper installationMapper;

    public InstallationRepository(InstallationMapper installationMapper) {
        this.installationMapper = installationMapper;
    }

    public InstallationResponse create(
            UUID id,
            String requestNo,
            UUID customerId,
            String originAddress,
            String destinationAddress,
            OffsetDateTime desiredStartAt,
            UUID applianceTypeId,
            int carryingDistanceMeters,
            int floorCount,
            BigDecimal additionalPipingMeters
    ) {
        OffsetDateTime createdAt = installationMapper.insertMoveRequest(
                id, requestNo, customerId, originAddress, destinationAddress, desiredStartAt);
        if (createdAt == null) {
            throw new EmptyResultDataAccessException(1);
        }

        installationMapper.insertMoveItem(
                UUID.randomUUID(), id, applianceTypeId,
                carryingDistanceMeters, floorCount, additionalPipingMeters);

        return new InstallationResponse(id, requestNo, "SUBMITTED", createdAt);
    }

    public List<InstallationHistoryItem> findByCustomerId(UUID customerId, int limit, int offset) {
        return installationMapper.findByCustomerId(customerId, limit, offset);
    }

    public long countByCustomerId(UUID customerId) {
        Long count = installationMapper.countByCustomerId(customerId);
        return count == null ? 0 : count;
    }

    public List<AdminInstallationSummary> findForAdmin(int limit, int offset, boolean reviewRequired) {
        return installationMapper.findForAdmin(limit, offset, reviewRequired);
    }

    public long countForAdmin(boolean reviewRequired) {
        Long count = installationMapper.countForAdmin(reviewRequired);
        return count == null ? 0 : count;
    }
}
