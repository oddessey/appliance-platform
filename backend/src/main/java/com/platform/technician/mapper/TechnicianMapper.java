package com.platform.technician.mapper;

import com.platform.technician.TechnicianAssignmentResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface TechnicianMapper {

    UUID findActiveApprovedProfileId(@Param("userId") UUID userId);

    List<TechnicianAssignmentResponse> findAssignments(
            @Param("profileId") UUID profileId,
            @Param("status") String status,
            @Param("limit") int limit,
            @Param("offset") int offset);

    long countAssignments(@Param("profileId") UUID profileId, @Param("status") String status);

    String lockAssignmentStatus(@Param("assignmentId") UUID assignmentId, @Param("profileId") UUID profileId);

    void updateAssignmentStatus(
            @Param("assignmentId") UUID assignmentId,
            @Param("status") String status,
            @Param("completed") boolean completed);

    void rejectAssignment(@Param("assignmentId") UUID assignmentId, @Param("reason") String reason);

    void createWorkEvent(
            @Param("id") UUID id,
            @Param("assignmentId") UUID assignmentId,
            @Param("eventType") String eventType,
            @Param("note") String note,
            @Param("userId") UUID userId);

    TechnicianAssignmentResponse findAssignment(
            @Param("assignmentId") UUID assignmentId, @Param("profileId") UUID profileId);
}
