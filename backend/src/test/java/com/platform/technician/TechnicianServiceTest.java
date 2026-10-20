package com.platform.technician;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TechnicianServiceTest {

    @Mock
    private TechnicianRepository technicianRepository;
    @InjectMocks
    private TechnicianService technicianService;

    @Test
    void listsAssignmentsForTheAuthenticatedTechnicianProfile() {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        TechnicianAssignmentResponse assignment = assignment(AssignmentStatus.PENDING);
        when(technicianRepository.findActiveApprovedProfileId(userId)).thenReturn(Optional.of(profileId));
        when(technicianRepository.findAssignments(profileId, "PENDING", 20, 0))
                .thenReturn(List.of(assignment));
        when(technicianRepository.countAssignments(profileId, "PENDING")).thenReturn(1L);

        TechnicianAssignmentPageResponse response = technicianService.listAssignments(
                userId, AssignmentStatus.PENDING, 0, 20);

        assertEquals(List.of(assignment), response.content());
        assertEquals(1L, response.totalElements());
        verify(technicianRepository).findAssignments(profileId, "PENDING", 20, 0);
    }

    @Test
    void acceptsOnlyPendingAssignmentAssignedToTechnician() {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        TechnicianAssignmentResponse updated = assignment(AssignmentStatus.ACCEPTED);
        when(technicianRepository.findActiveApprovedProfileId(userId)).thenReturn(Optional.of(profileId));
        when(technicianRepository.lockAssignmentStatus(assignmentId, profileId))
                .thenReturn(Optional.of("PENDING"));
        when(technicianRepository.findAssignment(assignmentId, profileId)).thenReturn(updated);

        TechnicianAssignmentResponse response = technicianService.accept(userId, assignmentId);

        assertEquals("ACCEPTED", response.status());
        verify(technicianRepository).updateAssignmentStatus(assignmentId, "ACCEPTED", false);
        verify(technicianRepository).createWorkEvent(assignmentId, "ACCEPTED", null, userId);
    }

    @Test
    void rejectsAssignmentAndRecordsReasonAsWorkEvent() {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        TechnicianAssignmentResponse updated = assignment(AssignmentStatus.REJECTED);
        when(technicianRepository.findActiveApprovedProfileId(userId)).thenReturn(Optional.of(profileId));
        when(technicianRepository.lockAssignmentStatus(assignmentId, profileId))
                .thenReturn(Optional.of("PENDING"));
        when(technicianRepository.findAssignment(assignmentId, profileId)).thenReturn(updated);

        technicianService.reject(userId, assignmentId, "  일정이 맞지 않습니다.  ");

        verify(technicianRepository).rejectAssignment(assignmentId, "일정이 맞지 않습니다.");
        verify(technicianRepository).createWorkEvent(
                assignmentId, "REJECTED", "일정이 맞지 않습니다.", userId);
    }

    @Test
    void rejectsWorkStateChangesThatSkipTheAllowedOrder() {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        when(technicianRepository.findActiveApprovedProfileId(userId)).thenReturn(Optional.of(profileId));
        when(technicianRepository.lockAssignmentStatus(assignmentId, profileId))
                .thenReturn(Optional.of("ACCEPTED"));

        assertThrows(InvalidAssignmentTransitionException.class, () -> technicianService.addEvent(
                userId, assignmentId, new AssignmentEventRequest(AssignmentStatus.WORK_COMPLETED, null)));
        verify(technicianRepository, never()).createWorkEvent(
                assignmentId, "WORK_COMPLETED", null, userId);
    }

    @Test
    void marksAssignmentCompletedAfterWorkStarts() {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        TechnicianAssignmentResponse updated = assignment(AssignmentStatus.WORK_COMPLETED);
        when(technicianRepository.findActiveApprovedProfileId(userId)).thenReturn(Optional.of(profileId));
        when(technicianRepository.lockAssignmentStatus(assignmentId, profileId))
                .thenReturn(Optional.of("WORK_STARTED"));
        when(technicianRepository.findAssignment(assignmentId, profileId)).thenReturn(updated);

        technicianService.addEvent(userId, assignmentId,
                new AssignmentEventRequest(AssignmentStatus.WORK_COMPLETED, "완료"));

        verify(technicianRepository).updateAssignmentStatus(assignmentId, "WORK_COMPLETED", true);
        verify(technicianRepository).createWorkEvent(assignmentId, "WORK_COMPLETED", "완료", userId);
    }

    private TechnicianAssignmentResponse assignment(AssignmentStatus status) {
        return new TechnicianAssignmentResponse(
                UUID.randomUUID(), status.name(), "RSV-123456", OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(1).plusHours(2), "고객", "010-1234-5678",
                "이전 주소", "설치 주소", "세탁기", null);
    }
}
