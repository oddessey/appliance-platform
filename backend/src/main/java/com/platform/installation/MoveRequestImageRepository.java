package com.platform.installation;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MoveRequestImageRepository {

    private final JdbcTemplate jdbcTemplate;

    public MoveRequestImageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isOwnedBy(UUID moveRequestId, UUID customerId) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM move_request WHERE id = ? AND customer_id = ?
                )
                """, Boolean.class, moveRequestId, customerId);
        return Boolean.TRUE.equals(exists);
    }

    public boolean hasAccess(UUID moveRequestId, UUID userId) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                    FROM move_request mr
                    WHERE mr.id = ?
                      AND (
                        mr.customer_id = ?
                        OR EXISTS (
                            SELECT 1
                            FROM reservation r
                            JOIN technician_assignment ta ON ta.reservation_id = r.id
                            JOIN technician_profile tp ON tp.id = ta.technician_id
                            WHERE r.move_request_id = mr.id
                              AND tp.user_id = ?
                              AND tp.active = true
                              AND tp.approval_status = 'APPROVED'
                              AND ta.status IN (
                                  'PENDING', 'ACCEPTED', 'ON_THE_WAY',
                                  'WORK_STARTED', 'WORK_COMPLETED'
                              )
                        )
                      )
                )
                """, Boolean.class, moveRequestId, userId, userId);
        return Boolean.TRUE.equals(exists);
    }

    public boolean exists(UUID moveRequestId) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM move_request WHERE id = ?)
                """, Boolean.class, moveRequestId);
        return Boolean.TRUE.equals(exists);
    }

    public boolean moveItemBelongsToRequest(UUID moveItemId, UUID moveRequestId) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM move_item WHERE id = ? AND move_request_id = ?
                )
                """, Boolean.class, moveItemId, moveRequestId);
        return Boolean.TRUE.equals(exists);
    }

    public List<MoveRequestImageResponse> findCustomerImages(UUID moveRequestId) {
        return jdbcTemplate.query("""
                SELECT id, move_request_id, move_item_id, purpose, original_filename,
                       content_type, size_bytes, created_at
                FROM request_image
                WHERE move_request_id = ?
                  AND purpose IN ('APPLIANCE', 'INSTALLATION_ENV')
                ORDER BY created_at, id
                """, (resultSet, rowNumber) -> new MoveRequestImageResponse(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("move_request_id", UUID.class),
                resultSet.getObject("move_item_id", UUID.class),
                resultSet.getString("purpose"),
                resultSet.getString("original_filename"),
                resultSet.getString("content_type"),
                resultSet.getLong("size_bytes"),
                resultSet.getObject("created_at", OffsetDateTime.class)
        ), moveRequestId);
    }

    public Optional<MoveRequestImageFile> findCustomerImageFile(UUID moveRequestId, UUID imageId) {
        return jdbcTemplate.query("""
                SELECT object_key, original_filename, content_type
                FROM request_image
                WHERE id = ? AND move_request_id = ?
                  AND purpose IN ('APPLIANCE', 'INSTALLATION_ENV')
                """, resultSet -> resultSet.next()
                ? Optional.of(new MoveRequestImageFile(
                Path.of(resultSet.getString("object_key")),
                resultSet.getString("original_filename"),
                resultSet.getString("content_type")))
                : Optional.empty(), imageId, moveRequestId);
    }

    public MoveRequestImageResponse insert(
            UUID moveRequestId,
            UUID moveItemId,
            String objectKey,
            String originalFilename,
            String contentType,
            long sizeBytes,
            MoveRequestImagePurpose purpose,
            UUID uploadedBy
    ) {
        UUID imageId = UUID.randomUUID();
        return jdbcTemplate.execute((ConnectionCallback<MoveRequestImageResponse>) connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO request_image (
                        id, move_request_id, move_item_id, storage_provider, object_key,
                        original_filename, content_type, size_bytes, purpose, uploaded_by
                    )
                    VALUES (?, ?, ?, 'LOCAL', ?, ?, ?, ?, ?, ?)
                    RETURNING id, move_request_id, move_item_id, purpose, original_filename,
                              content_type, size_bytes, created_at
                    """)) {
                statement.setObject(1, imageId);
                statement.setObject(2, moveRequestId);
                if (moveItemId == null) {
                    statement.setNull(3, Types.OTHER);
                } else {
                    statement.setObject(3, moveItemId);
                }
                statement.setString(4, objectKey);
                statement.setString(5, originalFilename);
                statement.setString(6, contentType);
                statement.setLong(7, sizeBytes);
                statement.setString(8, purpose.name());
                statement.setObject(9, uploadedBy);

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new MoveRequestImageNotFoundException();
                    }
                    return new MoveRequestImageResponse(
                            resultSet.getObject("id", UUID.class),
                            resultSet.getObject("move_request_id", UUID.class),
                            resultSet.getObject("move_item_id", UUID.class),
                            resultSet.getString("purpose"),
                            resultSet.getString("original_filename"),
                            resultSet.getString("content_type"),
                            resultSet.getLong("size_bytes"),
                            resultSet.getObject("created_at", OffsetDateTime.class)
                    );
                }
            }
        });
    }
}
