package com.platform.technician;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class TechnicianAdminService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> CANCELLABLE_ASSIGNMENT_STATUSES =
            Set.of("PENDING", "ACCEPTED", "ON_THE_WAY");

    private final TechnicianAdminRepository repository;

    public TechnicianAdminService(TechnicianAdminRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<TechnicianCandidateResponse> findCandidates(UUID reservationId) {
        requireConfirmedReservation(reservationId, false);
        return repository.findCandidates(reservationId);
    }

    @Transactional(readOnly = true)
    public PendingTechnicianReservationPageResponse listPendingReservations(int page, int size) {
        validatePage(page, size);
        return new PendingTechnicianReservationPageResponse(page, size,
                repository.countPendingReservations(),
                repository.findPendingReservations(size, page * size));
    }

    @Transactional(readOnly = true)
    public List<UUID> findUnassignedConfirmedReservationIds(UUID afterId, int limit) {
        return repository.findUnassignedConfirmedReservationIds(afterId, limit);
    }

    @Transactional
    public void autoAssign(UUID reservationId) {
        String status = repository.findReservationStatusForUpdate(reservationId).orElse(null);
        if (!"CONFIRMED".equals(status) || repository.hasNonRejectedAssignment(reservationId)) {
            return;
        }
        repository.findCandidates(reservationId).stream()
                .findFirst()
                .ifPresent(candidate -> repository.createAssignment(
                        reservationId, candidate.technicianProfileId(), null, "AUTO"));
    }

    @Transactional
    public TechnicianAssignmentResponse assign(UUID reservationId, UUID profileId, UUID actorUserId) {
        requireConfirmedReservation(reservationId, true);
        if (repository.hasNonRejectedAssignment(reservationId)) {
            throw new InvalidTechnicianAssignmentException();
        }
        boolean eligible = repository.findCandidates(reservationId).stream()
                .anyMatch(candidate -> candidate.technicianProfileId().equals(profileId));
        if (!eligible) {
            throw new InvalidTechnicianAssignmentException();
        }
        UUID assignmentId = repository.createAssignment(reservationId, profileId, actorUserId, "MANUAL");
        return repository.findAssignment(assignmentId).orElseThrow(InvalidTechnicianAssignmentException::new);
    }

    @Transactional
    public void cancelAssignment(UUID assignmentId, UUID actorUserId) {
        TechnicianAdminRepository.AssignmentState assignment = repository.findAssignmentForUpdate(assignmentId)
                .orElseThrow(AssignmentNotFoundException::new);
        if (!CANCELLABLE_ASSIGNMENT_STATUSES.contains(assignment.status())) {
            throw new InvalidAssignmentTransitionException();
        }
        repository.cancelAssignment(assignmentId, assignment.reservationId(), assignment.status(), actorUserId);
    }

    @Transactional(readOnly = true)
    public TechnicianAdminPageResponse listTechnicians(TechnicianApprovalStatus status, int page, int size) {
        validatePage(page, size);
        String statusValue = status == null ? null : status.name();
        return new TechnicianAdminPageResponse(page, size, repository.countTechnicians(statusValue),
                repository.findTechnicians(statusValue, size, page * size));
    }

    @Transactional
    public void updateTechnician(
            UUID profileId,
            TechnicianAdminUpdateRequest request,
            UUID actorUserId
    ) {
        if (request.active() && request.approvalStatus() != TechnicianApprovalStatus.APPROVED) {
            throw new InvalidTechnicianAssignmentException();
        }
        if (!repository.updateTechnician(profileId, request.approvalStatus().name(), request.active(), actorUserId)) {
            throw new TechnicianProfileNotFoundException();
        }
    }

    private void requireConfirmedReservation(UUID reservationId, boolean lock) {
        String status;
        if (lock) {
            status = repository.findReservationStatusForUpdate(reservationId)
                    .orElseThrow(ReservationNotFoundException::new);
        } else {
            status = repository.findReservationStatus(reservationId)
                    .orElseThrow(ReservationNotFoundException::new);
        }
        if (!"CONFIRMED".equals(status)) {
            throw new InvalidTechnicianAssignmentException();
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE || (long) page * size > Integer.MAX_VALUE) {
            throw new InvalidAssignmentPageException();
        }
    }
}
