package com.platform.technician;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TechnicianEvidenceRepository {

    private final JdbcTemplate jdbcTemplate;

    public TechnicianEvidenceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<String> lockAssignmentStatus(UUID assignmentId, UUID profileId) {
        return jdbcTemplate.query("""
                SELECT status FROM technician_assignment
                WHERE id = ? AND technician_id = ? FOR UPDATE
                """, resultSet -> resultSet.next()
                ? Optional.of(resultSet.getString("status"))
                : Optional.empty(), assignmentId, profileId);
    }

    public boolean assignmentExists(UUID assignmentId, UUID profileId) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM technician_assignment WHERE id = ? AND technician_id = ?
                )
                """, Boolean.class, assignmentId, profileId);
        return Boolean.TRUE.equals(exists);
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
        UUID imageId = UUID.randomUUID();
        return jdbcTemplate.query("""
                INSERT INTO request_image
                    (id, move_request_id, storage_provider, object_key, original_filename,
                     content_type, size_bytes, purpose, uploaded_by)
                SELECT ?, mr.id, 'LOCAL', ?, ?, ?, ?, 'WORK_PROOF', ?
                FROM technician_assignment ta
                JOIN reservation r ON r.id = ta.reservation_id
                JOIN move_request mr ON mr.id = r.move_request_id
                WHERE ta.id = ? AND ta.technician_id = ?
                RETURNING id, original_filename, content_type, size_bytes, created_at
                """, resultSet -> {
            if (!resultSet.next()) {
                throw new AssignmentNotFoundException();
            }
            return new TechnicianEvidenceResponse(
                    resultSet.getObject("id", UUID.class),
                    resultSet.getString("original_filename"),
                    resultSet.getString("content_type"),
                    resultSet.getLong("size_bytes"),
                    resultSet.getObject("created_at", java.time.OffsetDateTime.class));
        }, imageId, objectKey, filename, contentType, sizeBytes, userId, assignmentId, profileId);
    }

    public void recordEvidenceEvent(UUID assignmentId, UUID userId, UUID imageId) {
        jdbcTemplate.update("""
                INSERT INTO work_event (id, assignment_id, event_type, note, evidence_image_id, created_by)
                VALUES (?, ?, 'EVIDENCE_UPLOADED', '작업 증빙 이미지를 등록했습니다.', ?, ?)
                """, UUID.randomUUID(), assignmentId, imageId, userId);
    }

    public List<TechnicianEvidenceResponse> listEvidence(UUID assignmentId, UUID profileId, UUID userId) {
        return jdbcTemplate.query("""
                SELECT DISTINCT image.id, image.original_filename, image.content_type,
                       image.size_bytes, image.created_at
                FROM technician_assignment ta
                JOIN work_event event ON event.assignment_id = ta.id
                JOIN request_image image ON image.id = event.evidence_image_id
                WHERE ta.id = ? AND ta.technician_id = ? AND image.uploaded_by = ?
                ORDER BY image.created_at
                """, (resultSet, rowNumber) -> new TechnicianEvidenceResponse(
                resultSet.getObject("id", UUID.class), resultSet.getString("original_filename"),
                resultSet.getString("content_type"), resultSet.getLong("size_bytes"),
                resultSet.getObject("created_at", java.time.OffsetDateTime.class)),
                assignmentId, profileId, userId);
    }

    public List<TechnicianEvidenceResponse> listEvidenceForAdmin(UUID assignmentId) {
        return jdbcTemplate.query("""
                SELECT DISTINCT image.id, image.original_filename, image.content_type,
                       image.size_bytes, image.created_at
                FROM technician_assignment ta
                JOIN work_event event ON event.assignment_id = ta.id
                JOIN request_image image ON image.id = event.evidence_image_id
                WHERE ta.id = ? AND image.purpose = 'WORK_PROOF'
                ORDER BY image.created_at
                """, (resultSet, rowNumber) -> new TechnicianEvidenceResponse(
                resultSet.getObject("id", UUID.class), resultSet.getString("original_filename"),
                resultSet.getString("content_type"), resultSet.getLong("size_bytes"),
                resultSet.getObject("created_at", java.time.OffsetDateTime.class)), assignmentId);
    }

    public Optional<TechnicianEvidenceFile> findEvidenceFile(
            UUID assignmentId,
            UUID evidenceId,
            UUID profileId,
            UUID userId
    ) {
        return jdbcTemplate.query("""
                SELECT image.object_key, image.original_filename, image.content_type
                FROM technician_assignment ta
                JOIN work_event event ON event.assignment_id = ta.id
                JOIN request_image image ON image.id = event.evidence_image_id
                WHERE ta.id = ? AND ta.technician_id = ? AND image.id = ?
                  AND image.uploaded_by = ? AND image.purpose = 'WORK_PROOF'
                """, resultSet -> resultSet.next()
                ? Optional.of(new TechnicianEvidenceFile(
                java.nio.file.Path.of(resultSet.getString("object_key")),
                resultSet.getString("original_filename"), resultSet.getString("content_type")))
                : Optional.empty(), assignmentId, profileId, evidenceId, userId);
    }

    public Optional<TechnicianEvidenceFile> findEvidenceFileForAdmin(UUID assignmentId, UUID evidenceId) {
        return jdbcTemplate.query("""
                SELECT image.object_key, image.original_filename, image.content_type
                FROM technician_assignment ta
                JOIN work_event event ON event.assignment_id = ta.id
                JOIN request_image image ON image.id = event.evidence_image_id
                WHERE ta.id = ? AND image.id = ? AND image.purpose = 'WORK_PROOF'
                """, resultSet -> resultSet.next()
                ? Optional.of(new TechnicianEvidenceFile(
                java.nio.file.Path.of(resultSet.getString("object_key")),
                resultSet.getString("original_filename"), resultSet.getString("content_type")))
                : Optional.empty(), assignmentId, evidenceId);
    }
}
