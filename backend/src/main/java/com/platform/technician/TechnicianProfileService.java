package com.platform.technician;

import com.platform.appliance.ApplianceTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class TechnicianProfileService {

    private final TechnicianProfileRepository repository;
    private final ApplianceTypeRepository applianceTypeRepository;

    public TechnicianProfileService(
            TechnicianProfileRepository repository,
            ApplianceTypeRepository applianceTypeRepository
    ) {
        this.repository = repository;
        this.applianceTypeRepository = applianceTypeRepository;
    }

    @Transactional(readOnly = true)
    public TechnicianProfileResponse getProfile(UUID userId) {
        return repository.findByUserId(userId).orElseThrow(TechnicianProfileNotFoundException::new);
    }

    @Transactional
    public TechnicianProfileResponse saveProfile(UUID userId, TechnicianProfileUpdateRequest request) {
        List<String> serviceAreas = request.serviceAreas().stream().map(String::strip).toList();
        if (new HashSet<>(serviceAreas).size() != serviceAreas.size()
                || new HashSet<>(request.applianceTypeCodes()).size() != request.applianceTypeCodes().size()) {
            throw new InvalidTechnicianProfileException();
        }
        for (String code : request.applianceTypeCodes()) {
            if (applianceTypeRepository.findActiveByCode(code).isEmpty()) {
                throw new InvalidTechnicianProfileException();
            }
        }
        List<TechnicianAvailabilityResponse> availability = request.availability().stream()
                .map(item -> new TechnicianAvailabilityResponse(
                        item.dayOfWeek(), parseTime(item.startTime()), parseTime(item.endTime())))
                .toList();
        for (int index = 0; index < availability.size(); index++) {
            TechnicianAvailabilityResponse current = availability.get(index);
            if (!current.startTime().isBefore(current.endTime())) {
                throw new InvalidTechnicianProfileException();
            }
            for (int otherIndex = index + 1; otherIndex < availability.size(); otherIndex++) {
                TechnicianAvailabilityResponse other = availability.get(otherIndex);
                if (current.dayOfWeek() == other.dayOfWeek()
                        && current.startTime().isBefore(other.endTime())
                        && other.startTime().isBefore(current.endTime())) {
                    throw new InvalidTechnicianProfileException();
                }
            }
        }
        String licenseInfo = request.licenseInfo() == null ? null : request.licenseInfo().strip();
        if (licenseInfo != null && licenseInfo.isEmpty()) {
            licenseInfo = null;
        }
        TechnicianProfileResponse previousProfile = repository.findByUserId(userId).orElse(null);
        UUID profileId = repository.upsertProfile(userId, request.businessName().strip(), licenseInfo);
        if (previousProfile != null && !Objects.equals(previousProfile.licenseInfo(), licenseInfo)) {
            repository.recordLicenseInformationChange(
                    userId, profileId, previousProfile.approvalStatus(), previousProfile.active());
        }
        repository.replaceServiceAreas(profileId, serviceAreas);
        repository.replaceCapabilities(profileId, request.applianceTypeCodes());
        repository.replaceAvailability(profileId, availability);
        return repository.findByUserId(userId).orElseThrow(TechnicianProfileNotFoundException::new);
    }

    private LocalTime parseTime(String value) {
        return value.length() == 5 ? LocalTime.parse(value + ":00") : LocalTime.parse(value);
    }
}
