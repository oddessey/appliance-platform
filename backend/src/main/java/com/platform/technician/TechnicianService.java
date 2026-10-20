package com.platform.technician;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TechnicianService {

    private static final int MAX_PAGE_SIZE = 100;

    private final TechnicianRepository technicianRepository;

    public TechnicianService(TechnicianRepository technicianRepository) {
        this.technicianRepository = technicianRepository;
    }

    @Transactional(readOnly = true)
    public TechnicianAssignmentPageResponse listAssignments(
            UUID userId,
            AssignmentStatus status,
            int page,
            int size
    ) {
        validatePage(page, size);
        UUID profileId = findProfileId(userId);
        String statusValue = status == null ? null : status.name();
        List<TechnicianAssignmentResponse> content = technicianRepository.findAssignments(
                profileId, statusValue, size, page * size);
        return new TechnicianAssignmentPageResponse(page, size,
                technicianRepository.countAssignments(profileId, statusValue), content);
    }

    @Transactional
    public TechnicianAssignmentResponse accept(UUID userId, UUID assignmentId) {
        return transition(userId, assignmentId, AssignmentStatus.ACCEPTED, null);
    }

    @Transactional
    public TechnicianAssignmentResponse reject(UUID userId, UUID assignmentId, String reason) {
        String normalizedReason = reason.strip();
        if (normalizedReason.isEmpty()) {
            throw new InvalidAssignmentTransitionException();
        }
        UUID profileId = findProfileId(userId);
        AssignmentStatus current = lockCurrentStatus(assignmentId, profileId);
        if (current != AssignmentStatus.PENDING) {
            throw new InvalidAssignmentTransitionException();
        }
        technicianRepository.rejectAssignment(assignmentId, normalizedReason);
        technicianRepository.createWorkEvent(assignmentId, AssignmentStatus.REJECTED.name(),
                normalizedReason, userId);
        return technicianRepository.findAssignment(assignmentId, profileId);
    }

    @Transactional
    public TechnicianAssignmentResponse addEvent(
            UUID userId,
            UUID assignmentId,
            AssignmentEventRequest request
    ) {
        return transition(userId, assignmentId, request.eventType(), request.note());
    }

    private TechnicianAssignmentResponse transition(
            UUID userId,
            UUID assignmentId,
            AssignmentStatus nextStatus,
            String note
    ) {
        UUID profileId = findProfileId(userId);
        AssignmentStatus current = lockCurrentStatus(assignmentId, profileId);
        if (!isAllowed(current, nextStatus)) {
            throw new InvalidAssignmentTransitionException();
        }
        technicianRepository.updateAssignmentStatus(
                assignmentId, nextStatus.name(), nextStatus == AssignmentStatus.WORK_COMPLETED);
        technicianRepository.createWorkEvent(assignmentId, nextStatus.name(), note, userId);
        return technicianRepository.findAssignment(assignmentId, profileId);
    }

    private UUID findProfileId(UUID userId) {
        return technicianRepository.findActiveApprovedProfileId(userId)
                .orElseThrow(TechnicianProfileUnavailableException::new);
    }

    private AssignmentStatus lockCurrentStatus(UUID assignmentId, UUID profileId) {
        String status = technicianRepository.lockAssignmentStatus(assignmentId, profileId)
                .orElseThrow(AssignmentNotFoundException::new);
        try {
            return AssignmentStatus.valueOf(status);
        } catch (IllegalArgumentException exception) {
            throw new InvalidAssignmentTransitionException();
        }
    }

    private boolean isAllowed(AssignmentStatus current, AssignmentStatus next) {
        return switch (next) {
            case ACCEPTED -> current == AssignmentStatus.PENDING;
            case ON_THE_WAY -> current == AssignmentStatus.ACCEPTED;
            case WORK_STARTED -> current == AssignmentStatus.ON_THE_WAY;
            case WORK_COMPLETED -> current == AssignmentStatus.WORK_STARTED;
            case PENDING, REJECTED, CANCELLED -> false;
        };
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE || (long) page * size > Integer.MAX_VALUE) {
            throw new InvalidAssignmentPageException();
        }
    }
}
