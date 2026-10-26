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
class TechnicianAdminServiceTest {

    @Mock
    private TechnicianAdminRepository repository;
    @InjectMocks
    private TechnicianAdminService service;

    @Test
    void assignsOnlyAnEligibleCandidateToAConfirmedReservation() {
        UUID reservationId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        when(repository.findReservationStatusForUpdate(reservationId)).thenReturn(Optional.of("CONFIRMED"));
        when(repository.findCandidates(reservationId)).thenReturn(List.of(
                new TechnicianCandidateResponse(profileId, "기사", "사업자", "01012345678", 0)));
        when(repository.createAssignment(reservationId, profileId, actorId, "MANUAL")).thenReturn(assignmentId);
        TechnicianAssignmentResponse assignment = new TechnicianAssignmentResponse(
                assignmentId, "PENDING", "RSV-001", OffsetDateTime.now(), OffsetDateTime.now().plusHours(2),
                "고객", "01012345678", "출발", "도착", "세탁기", null);
        when(repository.findAssignment(assignmentId)).thenReturn(Optional.of(assignment));

        assertEquals(assignment, service.assign(reservationId, profileId, actorId));
        verify(repository).createAssignment(reservationId, profileId, actorId, "MANUAL");
    }

    @Test
    void doesNotAssignAnIneligibleCandidate() {
        UUID reservationId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        when(repository.findReservationStatusForUpdate(reservationId)).thenReturn(Optional.of("CONFIRMED"));
        when(repository.findCandidates(reservationId)).thenReturn(List.of());

        assertThrows(InvalidTechnicianAssignmentException.class,
                () -> service.assign(reservationId, profileId, actorId));
        verify(repository, never()).createAssignment(reservationId, profileId, actorId, "MANUAL");
    }

    @Test
    void automaticallyAssignsTheHighestPriorityEligibleCandidate() {
        UUID reservationId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        when(repository.findReservationStatusForUpdate(reservationId)).thenReturn(Optional.of("CONFIRMED"));
        when(repository.hasNonRejectedAssignment(reservationId)).thenReturn(false);
        when(repository.findCandidates(reservationId)).thenReturn(List.of(
                new TechnicianCandidateResponse(profileId, "가장 여유로운 기사", "업체", "01012345678", 0),
                new TechnicianCandidateResponse(UUID.randomUUID(), "다른 기사", "업체2", "01022223333", 3)));

        service.autoAssign(reservationId);

        verify(repository).createAssignment(reservationId, profileId, null, "AUTO");
    }

    @Test
    void leavesConfirmedReservationUnassignedWhenNoEligibleTechnicianExists() {
        UUID reservationId = UUID.randomUUID();
        when(repository.findReservationStatusForUpdate(reservationId)).thenReturn(Optional.of("CONFIRMED"));
        when(repository.hasNonRejectedAssignment(reservationId)).thenReturn(false);
        when(repository.findCandidates(reservationId)).thenReturn(List.of());

        service.autoAssign(reservationId);

        verify(repository, never()).createAssignment(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void automaticAssignmentDoesNotCreateADuplicate() {
        UUID reservationId = UUID.randomUUID();
        when(repository.findReservationStatusForUpdate(reservationId)).thenReturn(Optional.of("CONFIRMED"));
        when(repository.hasNonRejectedAssignment(reservationId)).thenReturn(true);

        service.autoAssign(reservationId);

        verify(repository, never()).findCandidates(reservationId);
    }

    @Test
    void manualAssignmentIsRejectedWhenAnActiveAssignmentExists() {
        UUID reservationId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        when(repository.findReservationStatusForUpdate(reservationId)).thenReturn(Optional.of("CONFIRMED"));
        when(repository.hasNonRejectedAssignment(reservationId)).thenReturn(true);

        assertThrows(InvalidTechnicianAssignmentException.class,
                () -> service.assign(reservationId, profileId, actorId));
        verify(repository, never()).findCandidates(reservationId);
        verify(repository, never()).createAssignment(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void adminCancelsAnOpenAssignment() {
        UUID assignmentId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        when(repository.findAssignmentForUpdate(assignmentId)).thenReturn(Optional.of(
                new TechnicianAdminRepository.AssignmentState(reservationId, "ACCEPTED")));

        service.cancelAssignment(assignmentId, actorId);

        verify(repository).cancelAssignment(assignmentId, reservationId, "ACCEPTED", actorId);
    }

    @Test
    void adminCannotCancelAnAssignmentThatHasStartedWork() {
        UUID assignmentId = UUID.randomUUID();
        when(repository.findAssignmentForUpdate(assignmentId)).thenReturn(Optional.of(
                new TechnicianAdminRepository.AssignmentState(UUID.randomUUID(), "WORK_STARTED")));

        assertThrows(InvalidAssignmentTransitionException.class,
                () -> service.cancelAssignment(assignmentId, UUID.randomUUID()));
        verify(repository, never()).cancelAssignment(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void adminCannotCancelAMissingAssignment() {
        UUID assignmentId = UUID.randomUUID();
        when(repository.findAssignmentForUpdate(assignmentId)).thenReturn(Optional.empty());

        assertThrows(AssignmentNotFoundException.class,
                () -> service.cancelAssignment(assignmentId, UUID.randomUUID()));
        verify(repository, never()).cancelAssignment(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsReservationsThatAreNotConfirmed() {
        UUID reservationId = UUID.randomUUID();
        when(repository.findReservationStatus(reservationId)).thenReturn(Optional.of("PENDING"));

        assertThrows(InvalidTechnicianAssignmentException.class,
                () -> service.findCandidates(reservationId));
    }
}
