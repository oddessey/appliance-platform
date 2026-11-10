package com.platform.technician;

import com.platform.technician.mapper.TechnicianProfileMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TechnicianProfileRepository {

    private final TechnicianProfileMapper mapper;

    public TechnicianProfileRepository(TechnicianProfileMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<TechnicianProfileResponse> findByUserId(UUID userId) {
        TechnicianProfileMapper.TechnicianProfileRow profile = mapper.findByUserId(userId);
        if (profile == null) {
            return Optional.empty();
        }
        return Optional.of(new TechnicianProfileResponse(
                profile.id(), profile.approvalStatus(), profile.active(), profile.businessName(), profile.licenseInfo(),
                mapper.findServiceAreas(profile.id()), mapper.findCapabilities(profile.id()),
                mapper.findAvailability(profile.id())));
    }

    public UUID upsertProfile(UUID userId, String businessName, String licenseInfo) {
        UUID id = mapper.upsertProfile(UUID.randomUUID(), userId, businessName, licenseInfo);
        if (id == null) {
            throw new IllegalStateException("Technician profile upsert did not return an id");
        }
        return id;
    }

    public void recordLicenseInformationChange(
            UUID userId,
            UUID profileId,
            String previousApprovalStatus,
            boolean previousActive
    ) {
        mapper.recordLicenseInformationChange(
                UUID.randomUUID(), userId, profileId, previousApprovalStatus, previousActive);
    }

    public void replaceServiceAreas(UUID profileId, List<String> serviceAreas) {
        mapper.deleteServiceAreas(profileId);
        for (String region : serviceAreas) {
            mapper.insertServiceArea(UUID.randomUUID(), profileId, region);
        }
    }

    public void replaceCapabilities(UUID profileId, List<String> applianceCodes) {
        mapper.deleteCapabilities(profileId);
        for (String code : applianceCodes) {
            mapper.insertCapability(profileId, code);
        }
    }

    public void replaceAvailability(UUID profileId, List<TechnicianAvailabilityResponse> availability) {
        mapper.deleteAvailability(profileId);
        for (TechnicianAvailabilityResponse slot : availability) {
            mapper.insertAvailability(UUID.randomUUID(), profileId, slot.dayOfWeek(),
                    slot.startTime(), slot.endTime());
        }
    }
}
