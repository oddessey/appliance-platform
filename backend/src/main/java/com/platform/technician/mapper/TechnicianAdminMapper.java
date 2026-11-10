package com.platform.technician.mapper;

import com.platform.technician.PendingTechnicianReservationResponse;
import com.platform.technician.TechnicianAdminRepository.AssignmentState;
import com.platform.technician.TechnicianAdminRepository.TechnicianApprovalState;
import com.platform.technician.TechnicianAdminSummary;
import com.platform.technician.TechnicianAssignmentResponse;
import com.platform.technician.TechnicianCandidateResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface TechnicianAdminMapper {

    List<TechnicianCandidateResponse> findCandidates(@Param("reservationId") UUID reservationId);

    List<UUID> findUnassignedConfirmedReservationIds(@Param("afterId") UUID afterId, @Param("limit") int limit);

    List<PendingTechnicianReservationResponse> findPendingReservations(
            @Param("limit") int limit, @Param("offset") int offset);

    long countPendingReservations();

    boolean hasNonRejectedAssignment(@Param("reservationId") UUID reservationId);

    String findReservationStatusForUpdate(@Param("reservationId") UUID reservationId);

    String findReservationStatus(@Param("reservationId") UUID reservationId);

    AssignmentState findAssignmentForUpdate(@Param("assignmentId") UUID assignmentId);

    void cancelAssignment(@Param("assignmentId") UUID assignmentId);

    void recordAssignmentCancellation(
            @Param("id") UUID id, @Param("assignmentId") UUID assignmentId, @Param("actorUserId") UUID actorUserId);

    void recordAssignmentCancellationAudit(
            @Param("id") UUID id,
            @Param("actorUserId") UUID actorUserId,
            @Param("assignmentId") UUID assignmentId,
            @Param("reservationId") UUID reservationId,
            @Param("previousStatus") String previousStatus,
            @Param("afterReservationId") UUID afterReservationId);

    void insertAssignment(
            @Param("id") UUID id,
            @Param("reservationId") UUID reservationId,
            @Param("technicianProfileId") UUID technicianProfileId);

    void recordAssignmentCreationAudit(
            @Param("id") UUID id,
            @Param("actorUserId") UUID actorUserId,
            @Param("action") String action,
            @Param("assignmentId") UUID assignmentId,
            @Param("reservationId") UUID reservationId,
            @Param("technicianProfileId") UUID technicianProfileId,
            @Param("assignmentSource") String assignmentSource);

    TechnicianAssignmentResponse findAssignment(@Param("assignmentId") UUID assignmentId);

    long countTechnicians(@Param("approvalStatus") String approvalStatus);

    List<TechnicianAdminSummary> findTechnicians(
            @Param("approvalStatus") String approvalStatus,
            @Param("limit") int limit,
            @Param("offset") int offset);

    TechnicianApprovalState findTechnicianApprovalStateForUpdate(@Param("profileId") UUID profileId);

    void updateTechnician(
            @Param("profileId") UUID profileId,
            @Param("approvalStatus") String approvalStatus,
            @Param("active") boolean active);

    void recordTechnicianUpdateAudit(
            @Param("id") UUID id,
            @Param("actorUserId") UUID actorUserId,
            @Param("profileId") UUID profileId,
            @Param("previousApprovalStatus") String previousApprovalStatus,
            @Param("previousActive") boolean previousActive,
            @Param("approvalStatus") String approvalStatus,
            @Param("active") boolean active);
}
