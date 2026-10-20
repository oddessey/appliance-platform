package com.platform.technician;

import com.platform.appliance.ApplianceType;
import com.platform.appliance.ApplianceTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TechnicianProfileServiceTest {

    @Mock
    private TechnicianProfileRepository repository;
    @Mock
    private ApplianceTypeRepository applianceTypeRepository;
    @InjectMocks
    private TechnicianProfileService service;

    @Test
    void savesProfileAndAvailabilityWithoutChangingApprovalState() {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        when(applianceTypeRepository.findActiveByCode("WASHING_MACHINE"))
                .thenReturn(Optional.of(new ApplianceType(UUID.randomUUID(), "WASHING_MACHINE", "세탁기")));
        when(repository.upsertProfile(userId, "기사 사업자", "자격 확인 완료")).thenReturn(profileId);
        TechnicianProfileResponse response = new TechnicianProfileResponse(
                profileId, "PENDING", false, "기사 사업자", "자격 확인 완료", List.of("서울특별시 강남구"),
                List.of("WASHING_MACHINE"),
                List.of(new TechnicianAvailabilityResponse(1, LocalTime.of(9, 0), LocalTime.of(18, 0))));
        when(repository.findByUserId(userId)).thenReturn(Optional.of(response), Optional.of(response));
        TechnicianProfileUpdateRequest request = new TechnicianProfileUpdateRequest(
                " 기사 사업자 ", " 자격 확인 완료 ",
                List.of("서울특별시 강남구"), List.of("WASHING_MACHINE"),
                List.of(new TechnicianAvailabilityRequest(1, "09:00", "18:00")));

        TechnicianProfileResponse result = service.saveProfile(userId, request);

        assertEquals("PENDING", result.approvalStatus());
        verify(repository).replaceAvailability(profileId,
                List.of(new TechnicianAvailabilityResponse(1, LocalTime.of(9, 0), LocalTime.of(18, 0))));
        verify(repository).replaceServiceAreas(profileId, List.of("서울특별시 강남구"));
        verify(repository).replaceCapabilities(profileId, List.of("WASHING_MACHINE"));
    }

    @Test
    void recordsCredentialChangesThatRequireReapproval() {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        when(repository.findByUserId(userId)).thenReturn(
                Optional.of(new TechnicianProfileResponse(
                        profileId, "APPROVED", true, "기사 사업자", "기존 자격",
                        List.of("서울특별시 강남구"), List.of("WASHING_MACHINE"), List.of())),
                Optional.of(new TechnicianProfileResponse(
                        profileId, "PENDING", false, "기사 사업자", "새 자격",
                        List.of("서울특별시 강남구"), List.of("WASHING_MACHINE"), List.of())));
        when(applianceTypeRepository.findActiveByCode("WASHING_MACHINE"))
                .thenReturn(Optional.of(new ApplianceType(UUID.randomUUID(), "WASHING_MACHINE", "세탁기")));
        when(repository.upsertProfile(userId, "기사 사업자", "새 자격")).thenReturn(profileId);
        TechnicianProfileUpdateRequest request = new TechnicianProfileUpdateRequest(
                "기사 사업자", "새 자격", List.of("서울특별시 강남구"), List.of("WASHING_MACHINE"),
                List.of(new TechnicianAvailabilityRequest(1, "09:00", "18:00")));

        TechnicianProfileResponse response = service.saveProfile(userId, request);

        assertEquals("PENDING", response.approvalStatus());
        verify(repository).recordLicenseInformationChange(userId, profileId, "APPROVED", true);
    }

    @Test
    void rejectsOverlappingAvailabilityBeforePersistence() {
        UUID userId = UUID.randomUUID();
        when(applianceTypeRepository.findActiveByCode("WASHING_MACHINE"))
                .thenReturn(Optional.of(new ApplianceType(UUID.randomUUID(), "WASHING_MACHINE", "세탁기")));
        TechnicianProfileUpdateRequest request = new TechnicianProfileUpdateRequest(
                "기사 사업자", null, List.of("서울특별시 강남구"), List.of("WASHING_MACHINE"),
                List.of(new TechnicianAvailabilityRequest(1, "09:00", "12:00"),
                        new TechnicianAvailabilityRequest(1, "11:00", "13:00")));

        assertThrows(InvalidTechnicianProfileException.class, () -> service.saveProfile(userId, request));
    }

    @Test
    void rejectsInactiveOrUnknownApplianceTypes() {
        UUID userId = UUID.randomUUID();
        when(applianceTypeRepository.findActiveByCode("UNKNOWN")).thenReturn(Optional.empty());
        TechnicianProfileUpdateRequest request = new TechnicianProfileUpdateRequest(
                "기사 사업자", null, List.of("서울특별시 강남구"), List.of("UNKNOWN"),
                List.of(new TechnicianAvailabilityRequest(1, "09:00", "18:00")));

        assertThrows(InvalidTechnicianProfileException.class, () -> service.saveProfile(userId, request));
    }
}
