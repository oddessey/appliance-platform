package com.platform.technician;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TechnicianRepository {

    private final JdbcTemplate jdbcTemplate;

    public TechnicianRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UUID> findActiveApprovedProfileId(UUID userId) {
        return jdbcTemplate.query("""
                SELECT id
                FROM technician_profile
                WHERE user_id = ? AND active = true AND approval_status = 'APPROVED'
                """, resultSet -> resultSet.next()
                ? Optional.of(resultSet.getObject("id", UUID.class))
                : Optional.empty(), userId);
    }

    public List<TechnicianAssignmentResponse> findAssignments(
            UUID profileId,
            String status,
            int limit,
            int offset
    ) {
        return jdbcTemplate.query("""
                SELECT ta.id, r.move_request_id, ta.status, r.reservation_no,
                       r.scheduled_start_at, r.scheduled_end_at,
                       customer.name AS customer_name, customer.phone AS customer_phone,
                       mr.origin_address, mr.destination_address,
                       string_agg(DISTINCT appliance_type.name, ', ' ORDER BY appliance_type.name)
                           AS appliance_types,
                       mr.customer_memo, quote.quote_no, quote.total_amount
                FROM technician_assignment ta
                JOIN reservation r ON r.id = ta.reservation_id
                JOIN quote ON quote.id = r.quote_id
                JOIN move_request mr ON mr.id = r.move_request_id
                JOIN app_user customer ON customer.id = mr.customer_id
                JOIN move_item mi ON mi.move_request_id = mr.id
                JOIN appliance_type ON appliance_type.id = mi.appliance_type_id
                WHERE ta.technician_id = ?
                  AND (?::varchar IS NULL OR ta.status = ?)
                GROUP BY ta.id, r.move_request_id, ta.status, r.reservation_no,
                         r.scheduled_start_at, r.scheduled_end_at, customer.name,
                         customer.phone, mr.origin_address, mr.destination_address,
                         mr.customer_memo, quote.quote_no, quote.total_amount
                ORDER BY r.scheduled_start_at, ta.assigned_at DESC
                LIMIT ? OFFSET ?
                """, (resultSet, rowNumber) -> new TechnicianAssignmentResponse(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("move_request_id", UUID.class),
                resultSet.getString("status"),
                resultSet.getString("reservation_no"),
                resultSet.getObject("scheduled_start_at", java.time.OffsetDateTime.class),
                resultSet.getObject("scheduled_end_at", java.time.OffsetDateTime.class),
                resultSet.getString("customer_name"),
                resultSet.getString("customer_phone"),
                resultSet.getString("origin_address"),
                resultSet.getString("destination_address"),
                resultSet.getString("appliance_types"),
                resultSet.getString("customer_memo"),
                resultSet.getString("quote_no"),
                resultSet.getBigDecimal("total_amount")
        ), profileId, status, status, limit, offset);
    }

    public long countAssignments(UUID profileId, String status) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM technician_assignment
                WHERE technician_id = ? AND (?::varchar IS NULL OR status = ?)
                """, Long.class, profileId, status, status);
        return count == null ? 0 : count;
    }

    public Optional<String> lockAssignmentStatus(UUID assignmentId, UUID profileId) {
        return jdbcTemplate.query("""
                SELECT status
                FROM technician_assignment
                WHERE id = ? AND technician_id = ?
                FOR UPDATE
                """, resultSet -> resultSet.next()
                ? Optional.of(resultSet.getString("status"))
                : Optional.empty(), assignmentId, profileId);
    }

    public void updateAssignmentStatus(UUID assignmentId, String status, boolean completed) {
        jdbcTemplate.update("""
                UPDATE technician_assignment
                SET status = ?, responded_at = COALESCE(responded_at, now()),
                    completed_at = CASE WHEN ? THEN now() ELSE completed_at END,
                    updated_at = now()
                WHERE id = ?
                """, status, completed, assignmentId);
    }

    public void rejectAssignment(UUID assignmentId, String reason) {
        jdbcTemplate.update("""
                UPDATE technician_assignment
                SET status = 'REJECTED', rejection_reason = ?, responded_at = now(), updated_at = now()
                WHERE id = ?
                """, reason, assignmentId);
    }

    public void createWorkEvent(UUID assignmentId, String eventType, String note, UUID userId) {
        jdbcTemplate.update("""
                INSERT INTO work_event (id, assignment_id, event_type, note, created_by)
                VALUES (?, ?, ?, ?, ?)
                """, UUID.randomUUID(), assignmentId, eventType, note, userId);
    }

    public TechnicianAssignmentResponse findAssignment(UUID assignmentId, UUID profileId) {
        return jdbcTemplate.query("""
                SELECT ta.id, r.move_request_id, ta.status, r.reservation_no,
                       r.scheduled_start_at, r.scheduled_end_at,
                       customer.name AS customer_name, customer.phone AS customer_phone,
                       mr.origin_address, mr.destination_address,
                       string_agg(DISTINCT appliance_type.name, ', ' ORDER BY appliance_type.name)
                           AS appliance_types,
                       mr.customer_memo, quote.quote_no, quote.total_amount
                FROM technician_assignment ta
                JOIN reservation r ON r.id = ta.reservation_id
                JOIN quote ON quote.id = r.quote_id
                JOIN move_request mr ON mr.id = r.move_request_id
                JOIN app_user customer ON customer.id = mr.customer_id
                JOIN move_item mi ON mi.move_request_id = mr.id
                JOIN appliance_type ON appliance_type.id = mi.appliance_type_id
                WHERE ta.id = ? AND ta.technician_id = ?
                GROUP BY ta.id, r.move_request_id, ta.status, r.reservation_no,
                         r.scheduled_start_at, r.scheduled_end_at, customer.name,
                         customer.phone, mr.origin_address, mr.destination_address,
                         mr.customer_memo, quote.quote_no, quote.total_amount
                """, resultSet -> {
            if (!resultSet.next()) {
                throw new AssignmentNotFoundException();
            }
            return new TechnicianAssignmentResponse(
                    resultSet.getObject("id", UUID.class),
                    resultSet.getObject("move_request_id", UUID.class),
                    resultSet.getString("status"),
                    resultSet.getString("reservation_no"),
                    resultSet.getObject("scheduled_start_at", java.time.OffsetDateTime.class),
                    resultSet.getObject("scheduled_end_at", java.time.OffsetDateTime.class),
                    resultSet.getString("customer_name"),
                    resultSet.getString("customer_phone"),
                    resultSet.getString("origin_address"),
                    resultSet.getString("destination_address"),
                    resultSet.getString("appliance_types"),
                    resultSet.getString("customer_memo"),
                    resultSet.getString("quote_no"),
                    resultSet.getBigDecimal("total_amount")
            );
        }, assignmentId, profileId);
    }
}
