package com.platform.technician;

import com.platform.technician.mapper.TechnicianEvidenceMapper;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TechnicianEvidenceRepository {

    private final TechnicianEvidenceMapper mapper;

    public TechnicianEvidenceRepository(TechnicianEvidenceMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<String> lockAssignmentStatus(UUID assignmentId, UUID profileId) {
        return Optional.ofNullable(mapper.lockAssignmentStatus(assignmentId, profileId));
    }

    public boolean assignmentExists(UUID assignmentId, UUID profileId) {
        return mapper.assignmentExists(assignmentId, profileId);
    }

    public TechnicianEvidenceResponse insertEvidence(
            UUID assignmentId,
            UUID profileId,
            UUID userId,
            String objectKey,
            String filename,
            String contentType,
            long sizeBytes
    ) {
        TechnicianEvidenceResponse response = mapper.insertEvidence(
                UUID.randomUUID(), assignmentId, profileId, userId, objectKey, filename, contentType, sizeBytes);
        if (response == null) {
            throw new AssignmentNotFoundException();
        }
        return response;
    }

    public void recordEvidenceEvent(UUID assignmentId, UUID userId, UUID imageId) {
        mapper.recordEvidenceEvent(UUID.randomUUID(), assignmentId, imageId, userId);
    }

    public List<TechnicianEvidenceResponse> listEvidence(UUID assignmentId, UUID profileId, UUID userId) {
        return mapper.listEvidence(assignmentId, profileId, userId);
    }

    public List<TechnicianEvidenceResponse> listEvidenceForAdmin(UUID assignmentId) {
        return mapper.listEvidenceForAdmin(assignmentId);
    }

    public Optional<TechnicianEvidenceFile> findEvidenceFile(
            UUID assignmentId,
            UUID evidenceId,
            UUID profileId,
            UUID userId
    ) {
        return Optional.ofNullable(mapper.findEvidenceFile(assignmentId, profileId, evidenceId, userId))
                .map(file -> new TechnicianEvidenceFile(
                        Path.of(file.objectKey()), file.originalFilename(), file.contentType()));
    }

    public Optional<TechnicianEvidenceFile> findEvidenceFileForAdmin(UUID assignmentId, UUID evidenceId) {
        return Optional.ofNullable(mapper.findEvidenceFileForAdmin(assignmentId, evidenceId))
                .map(file -> new TechnicianEvidenceFile(
                        Path.of(file.objectKey()), file.originalFilename(), file.contentType()));
    }
}
