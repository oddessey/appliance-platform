package com.platform.technician;

import com.platform.technician.mapper.TechnicianAdminMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TechnicianAdminRepository {

    private final TechnicianAdminMapper mapper;

    public TechnicianAdminRepository(TechnicianAdminMapper mapper) {
        this.mapper = mapper;
    }

    public List<TechnicianCandidateResponse> findCandidates(UUID reservationId) {
        return mapper.findCandidates(reservationId);
    }

    public List<UUID> findUnassignedConfirmedReservationIds(UUID afterId, int limit) {
        return mapper.findUnassignedConfirmedReservationIds(afterId, limit);
    }

    public List<PendingTechnicianReservationResponse> findPendingReservations(int limit, int offset) {
        return mapper.findPendingReservations(limit, offset);
    }

    public long countPendingReservations() {
        return mapper.countPendingReservations();
    }

    public boolean hasNonRejectedAssignment(UUID reservationId) {
        return mapper.hasNonRejectedAssignment(reservationId);
    }

    public Optional<String> findReservationStatusForUpdate(UUID reservationId) {
        return Optional.ofNullable(mapper.findReservationStatusForUpdate(reservationId));
    }

    public Optional<String> findReservationStatus(UUID reservationId) {
        return Optional.ofNullable(mapper.findReservationStatus(reservationId));
    }

    public Optional<AssignmentState> findAssignmentForUpdate(UUID assignmentId) {
        return Optional.ofNullable(mapper.findAssignmentForUpdate(assignmentId));
    }

    public void cancelAssignment(
            UUID assignmentId,
            UUID reservationId,
            String previousStatus,
            UUID actorUserId
    ) {
        mapper.cancelAssignment(assignmentId);
        mapper.recordAssignmentCancellation(UUID.randomUUID(), assignmentId, actorUserId);
        mapper.recordAssignmentCancellationAudit(
                UUID.randomUUID(), actorUserId, assignmentId, reservationId, previousStatus, reservationId);
    }

    public UUID createAssignment(
            UUID reservationId,
            UUID technicianProfileId,
            UUID actorUserId,
            String assignmentSource
    ) {
        UUID assignmentId = UUID.randomUUID();
        mapper.insertAssignment(assignmentId, reservationId, technicianProfileId);
        mapper.recordAssignmentCreationAudit(
                UUID.randomUUID(), actorUserId, "TECHNICIAN_ASSIGNMENT_" + assignmentSource,
                assignmentId, reservationId, technicianProfileId, assignmentSource);
        return assignmentId;
    }

    public Optional<TechnicianAssignmentResponse> findAssignment(UUID assignmentId) {
        return Optional.ofNullable(mapper.findAssignment(assignmentId));
    }

    public long countTechnicians(String approvalStatus) {
        return mapper.countTechnicians(approvalStatus);
    }

    public List<TechnicianAdminSummary> findTechnicians(String approvalStatus, int limit, int offset) {
        return mapper.findTechnicians(approvalStatus, limit, offset);
    }

    public boolean updateTechnician(
            UUID profileId,
            String approvalStatus,
            boolean active,
            UUID actorUserId
    ) {
        TechnicianApprovalState previous = mapper.findTechnicianApprovalStateForUpdate(profileId);
        if (previous == null) {
            return false;
        }
        mapper.updateTechnician(profileId, approvalStatus, active);
        mapper.recordTechnicianUpdateAudit(
                UUID.randomUUID(), actorUserId, profileId, previous.approvalStatus(), previous.active(),
                approvalStatus, active);
        return true;
    }

    public record AssignmentState(UUID reservationId, String status) {
    }

    public record TechnicianApprovalState(String approvalStatus, boolean active) {
    }
}
