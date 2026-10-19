package com.example.platform.installation;

import com.example.platform.appliance.ApplianceType;
import com.example.platform.appliance.ApplianceTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstallationServiceTest {

    @Mock
    private InstallationRepository installationRepository;
    @Mock
    private ApplianceTypeRepository applianceTypeRepository;
    @InjectMocks
    private InstallationService installationService;

    @Test
    void createsInstallationRequestForAuthenticatedCustomer() {
        UUID customerId = UUID.randomUUID();
        UUID applianceTypeId = UUID.randomUUID();
        OffsetDateTime desiredStartAt = OffsetDateTime.now().plusDays(3);
        CreateInstallationRequest request = new CreateInstallationRequest(
                "WASHING_MACHINE", "  이전 주소  ", "  설치 주소  ", desiredStartAt);
        InstallationResponse expected = new InstallationResponse(
                UUID.randomUUID(), "INS-ABC123", "SUBMITTED", OffsetDateTime.now());
        when(applianceTypeRepository.findActiveByCode("WASHING_MACHINE"))
                .thenReturn(Optional.of(new ApplianceType(applianceTypeId, "WASHING_MACHINE", "세탁기")));
        when(installationRepository.create(any(UUID.class), any(String.class), eq(customerId),
                eq("이전 주소"), eq("설치 주소"), eq(desiredStartAt), eq(applianceTypeId)))
                .thenReturn(expected);

        InstallationResponse response = installationService.create(customerId, request);

        assertEquals(expected, response);
        verify(installationRepository).create(any(UUID.class), any(String.class), eq(customerId),
                eq("이전 주소"), eq("설치 주소"), eq(desiredStartAt), eq(applianceTypeId));
    }

    @Test
    void rejectsInactiveOrUnknownApplianceTypeWithoutPersisting() {
        when(applianceTypeRepository.findActiveByCode("UNKNOWN")).thenReturn(Optional.empty());
        CreateInstallationRequest request = new CreateInstallationRequest(
                "UNKNOWN", "이전 주소", "설치 주소", OffsetDateTime.now().plusDays(3));

        assertThrows(UnknownApplianceTypeException.class,
                () -> installationService.create(UUID.randomUUID(), request));
        verify(installationRepository, never()).create(
                any(UUID.class), any(String.class), any(UUID.class), any(String.class),
                any(String.class), any(OffsetDateTime.class), any(UUID.class));
    }
}
