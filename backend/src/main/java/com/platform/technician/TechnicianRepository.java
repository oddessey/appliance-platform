package com.platform.technician;

import com.platform.technician.mapper.TechnicianMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TechnicianRepository {

    private final TechnicianMapper mapper;

    public TechnicianRepository(TechnicianMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<UUID> findActiveApprovedProfileId(UUID userId) {
        return Optional.ofNullable(mapper.findActiveApprovedProfileId(userId));
    }

    public List<TechnicianAssignmentResponse> findAssignments(
            UUID profileId,
            String status,
            int limit,
            int offset
    ) {
        return mapper.findAssignments(profileId, status, limit, offset);
    }

    public long countAssignments(UUID profileId, String status) {
        return mapper.countAssignments(profileId, status);
    }

    public Optional<String> lockAssignmentStatus(UUID assignmentId, UUID profileId) {
        return Optional.ofNullable(mapper.lockAssignmentStatus(assignmentId, profileId));
    }

    public void updateAssignmentStatus(UUID assignmentId, String status, boolean completed) {
        mapper.updateAssignmentStatus(assignmentId, status, completed);
    }

    public void rejectAssignment(UUID assignmentId, String reason) {
        mapper.rejectAssignment(assignmentId, reason);
    }

    public void createWorkEvent(UUID assignmentId, String eventType, String note, UUID userId) {
        mapper.createWorkEvent(UUID.randomUUID(), assignmentId, eventType, note, userId);
    }

    public TechnicianAssignmentResponse findAssignment(UUID assignmentId, UUID profileId) {
        TechnicianAssignmentResponse response = mapper.findAssignment(assignmentId, profileId);
        if (response == null) {
            throw new AssignmentNotFoundException();
        }
        return response;
    }
}
