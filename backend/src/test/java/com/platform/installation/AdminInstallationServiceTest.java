package com.platform.installation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminInstallationServiceTest {

    @Mock
    private InstallationRepository installationRepository;
    @InjectMocks
    private AdminInstallationService service;

    @Test
    void listsInstallationsAndLatestAiReviewsForAdministrator() {
        AdminInstallationSummary summary = new AdminInstallationSummary(
                UUID.randomUUID(), "INS-ABC123", "Customer", "customer@example.com", "NEEDS_REVIEW",
                "세탁기 × 1", "이전 주소", "설치 주소", null, null, null,
                null, null, null, null, null, null, null);
        when(installationRepository.findForAdmin(20, 20, true)).thenReturn(List.of(summary));
        when(installationRepository.countForAdmin(true)).thenReturn(1L);

        AdminInstallationPageResponse response = service.list(1, 20, true);

        assertEquals(1, response.page());
        assertEquals(20, response.size());
        assertEquals(1L, response.totalElements());
        assertEquals(List.of(summary), response.content());
        verify(installationRepository).findForAdmin(20, 20, true);
    }

    @Test
    void rejectsInvalidAdminInstallationPage() {
        assertThrows(InvalidInstallationPageException.class, () -> service.list(-1, 20, false));
        assertThrows(InvalidInstallationPageException.class, () -> service.list(0, 0, false));
        assertThrows(InvalidInstallationPageException.class, () -> service.list(0, 101, false));
        assertThrows(InvalidInstallationPageException.class, () -> service.list(Integer.MAX_VALUE, 100, false));

        verify(installationRepository, never()).findForAdmin(20, 0, false);
    }
}
