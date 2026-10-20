package com.platform.technician;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TechnicianAdminRepository {

    private final JdbcTemplate jdbcTemplate;

    public TechnicianAdminRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TechnicianCandidateResponse> findCandidates(UUID reservationId) {
        return jdbcTemplate.query("""
                SELECT tp.id, u.name, tp.business_name, u.phone,
                       (SELECT count(*)
                        FROM technician_assignment load_assignment
                        WHERE load_assignment.technician_id = tp.id
                          AND load_assignment.status IN
                              ('PENDING', 'ACCEPTED', 'ON_THE_WAY', 'WORK_STARTED')) AS active_count
                FROM reservation r
                JOIN move_request mr ON mr.id = r.move_request_id
                JOIN technician_profile tp
                  ON tp.active = true AND tp.approval_status = 'APPROVED'
                 AND tp.license_info IS NOT NULL AND trim(tp.license_info) <> ''
                JOIN app_user u ON u.id = tp.user_id AND u.status = 'ACTIVE' AND u.deleted_at IS NULL
                JOIN user_role role ON role.user_id = u.id AND role.role_code = 'TECHNICIAN'
                WHERE r.id = ? AND r.status = 'CONFIRMED'
                  AND EXISTS (
                      SELECT 1 FROM technician_service_area sa
                      WHERE sa.technician_id = tp.id AND sa.active = true
                        AND sa.region_code = split_part(trim(mr.origin_address), ' ', 1) || ' '
                                               || split_part(trim(mr.origin_address), ' ', 2))
                  AND EXISTS (
                      SELECT 1 FROM technician_service_area sa
                      WHERE sa.technician_id = tp.id AND sa.active = true
                        AND sa.region_code = split_part(trim(mr.destination_address), ' ', 1) || ' '
                                               || split_part(trim(mr.destination_address), ' ', 2))
                  AND NOT EXISTS (
                      SELECT 1
                      FROM move_item required_item
                      WHERE required_item.move_request_id = mr.id
                        AND NOT EXISTS (
                            SELECT 1
                            FROM technician_capability capability
                            WHERE capability.technician_id = tp.id
                              AND capability.appliance_type_id = required_item.appliance_type_id
                              AND capability.active = true))
                  AND EXISTS (
                      SELECT 1
                      FROM technician_availability availability
                      WHERE availability.technician_id = tp.id
                        AND availability.active = true
                        AND availability.day_of_week =
                            extract(isodow FROM r.scheduled_start_at AT TIME ZONE 'Asia/Seoul')::smallint
                        AND availability.start_time <=
                            (r.scheduled_start_at AT TIME ZONE 'Asia/Seoul')::time
                        AND availability.end_time >=
                            (r.scheduled_end_at AT TIME ZONE 'Asia/Seoul')::time
                        AND (r.scheduled_start_at AT TIME ZONE 'Asia/Seoul')::date =
                            (r.scheduled_end_at AT TIME ZONE 'Asia/Seoul')::date)
                  AND NOT EXISTS (
                      SELECT 1
                      FROM technician_assignment prior_assignment
                      WHERE prior_assignment.technician_id = tp.id
                        AND prior_assignment.reservation_id = r.id
                        AND prior_assignment.status NOT IN ('REJECTED', 'CANCELLED'))
                  AND NOT EXISTS (
                      SELECT 1
                      FROM technician_assignment existing
                      JOIN reservation occupied ON occupied.id = existing.reservation_id
                      WHERE existing.technician_id = tp.id
                        AND existing.reservation_id <> r.id
                        AND existing.status IN
                            ('PENDING', 'ACCEPTED', 'ON_THE_WAY', 'WORK_STARTED')
                        AND occupied.scheduled_start_at < r.scheduled_end_at
                        AND occupied.scheduled_end_at > r.scheduled_start_at)
                ORDER BY active_count, tp.created_at, tp.id
                LIMIT 100
                """, (resultSet, rowNumber) -> new TechnicianCandidateResponse(
                resultSet.getObject("id", UUID.class), resultSet.getString("name"),
                resultSet.getString("business_name"), resultSet.getString("phone"),
                resultSet.getLong("active_count")), reservationId);
    }

    public List<UUID> findUnassignedConfirmedReservationIds(UUID afterId, int limit) {
        return jdbcTemplate.query("""
                SELECT r.id
                FROM reservation r
                WHERE r.status = 'CONFIRMED'
                  AND (?::uuid IS NULL OR r.id > ?)
                  AND NOT EXISTS (
                      SELECT 1
                      FROM technician_assignment ta
                      WHERE ta.reservation_id = r.id AND ta.status NOT IN ('REJECTED', 'CANCELLED'))
                ORDER BY r.id
                LIMIT ?
                """, (resultSet, rowNumber) -> resultSet.getObject("id", UUID.class),
                afterId, afterId, limit);
    }

    public List<PendingTechnicianReservationResponse> findPendingReservations(int limit, int offset) {
        return jdbcTemplate.query("""
                SELECT r.id AS reservation_id, r.reservation_no, r.scheduled_start_at, r.scheduled_end_at,
                       customer.name AS customer_name, customer.phone AS customer_phone,
                       mr.origin_address, mr.destination_address,
                       string_agg(DISTINCT appliance_type.name, ', ' ORDER BY appliance_type.name)
                           AS appliance_types
                FROM reservation r
                JOIN move_request mr ON mr.id = r.move_request_id
                JOIN app_user customer ON customer.id = mr.customer_id
                JOIN move_item mi ON mi.move_request_id = mr.id
                JOIN appliance_type ON appliance_type.id = mi.appliance_type_id
                WHERE r.status = 'CONFIRMED'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM technician_assignment ta
                      WHERE ta.reservation_id = r.id AND ta.status NOT IN ('REJECTED', 'CANCELLED'))
                GROUP BY r.id, r.reservation_no, r.scheduled_start_at, r.scheduled_end_at,
                         customer.name, customer.phone, mr.origin_address, mr.destination_address
                ORDER BY r.scheduled_start_at, r.id
                LIMIT ? OFFSET ?
                """, (resultSet, rowNumber) -> new PendingTechnicianReservationResponse(
                resultSet.getObject("reservation_id", UUID.class),
                resultSet.getString("reservation_no"),
                resultSet.getObject("scheduled_start_at", java.time.OffsetDateTime.class),
                resultSet.getObject("scheduled_end_at", java.time.OffsetDateTime.class),
                resultSet.getString("customer_name"),
                resultSet.getString("customer_phone"),
                resultSet.getString("origin_address"),
                resultSet.getString("destination_address"),
                resultSet.getString("appliance_types")), limit, offset);
    }

    public long countPendingReservations() {
        Long count = jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM reservation r
                WHERE r.status = 'CONFIRMED'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM technician_assignment ta
                      WHERE ta.reservation_id = r.id AND ta.status NOT IN ('REJECTED', 'CANCELLED'))
                """, Long.class);
        return count == null ? 0 : count;
    }

    public boolean hasNonRejectedAssignment(UUID reservationId) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM technician_assignment
                    WHERE reservation_id = ? AND status NOT IN ('REJECTED', 'CANCELLED')
                )
                """, Boolean.class, reservationId);
        return Boolean.TRUE.equals(exists);
    }

    public Optional<String> findReservationStatusForUpdate(UUID reservationId) {
        return jdbcTemplate.query("""
                SELECT status FROM reservation WHERE id = ? FOR UPDATE
                """, resultSet -> resultSet.next()
                ? Optional.of(resultSet.getString("status"))
                : Optional.empty(), reservationId);
    }

    public Optional<String> findReservationStatus(UUID reservationId) {
        return jdbcTemplate.query("""
                SELECT status FROM reservation WHERE id = ?
                """, resultSet -> resultSet.next()
                ? Optional.of(resultSet.getString("status"))
                : Optional.empty(), reservationId);
    }

    public UUID createAssignment(
            UUID reservationId,
            UUID technicianProfileId,
            UUID actorUserId,
            String assignmentSource
    ) {
        UUID assignmentId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO technician_assignment (id, reservation_id, technician_id, status)
                VALUES (?, ?, ?, 'PENDING')
                """, assignmentId, reservationId, technicianProfileId);
        jdbcTemplate.update("""
                INSERT INTO audit_log
                    (id, actor_user_id, action, entity_type, entity_id, after_summary)
                VALUES (?, ?, ?, 'TECHNICIAN_ASSIGNMENT', ?,
                        jsonb_build_object('reservationId', ?, 'technicianProfileId', ?,
                                           'status', 'PENDING', 'source', ?))
                """, UUID.randomUUID(), actorUserId, "TECHNICIAN_ASSIGNMENT_" + assignmentSource,
                assignmentId, reservationId, technicianProfileId, assignmentSource);
        return assignmentId;
    }

    public Optional<TechnicianAssignmentResponse> findAssignment(UUID assignmentId) {
        return jdbcTemplate.query("""
                SELECT ta.id, ta.status, r.reservation_no, r.scheduled_start_at, r.scheduled_end_at,
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
                WHERE ta.id = ?
                GROUP BY ta.id, ta.status, r.reservation_no, r.scheduled_start_at,
                         r.scheduled_end_at, customer.name, customer.phone,
                         mr.origin_address, mr.destination_address, mr.customer_memo,
                         quote.quote_no, quote.total_amount
                """, resultSet -> resultSet.next()
                ? Optional.of(new TechnicianAssignmentResponse(
                resultSet.getObject("id", UUID.class), resultSet.getString("status"),
                resultSet.getString("reservation_no"),
                resultSet.getObject("scheduled_start_at", java.time.OffsetDateTime.class),
                resultSet.getObject("scheduled_end_at", java.time.OffsetDateTime.class),
                resultSet.getString("customer_name"), resultSet.getString("customer_phone"),
                resultSet.getString("origin_address"), resultSet.getString("destination_address"),
                resultSet.getString("appliance_types"), resultSet.getString("customer_memo"),
                resultSet.getString("quote_no"), resultSet.getBigDecimal("total_amount")))
                : Optional.empty(), assignmentId);
    }

    public long countTechnicians(String approvalStatus) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM technician_profile
                WHERE (?::varchar IS NULL OR approval_status = ?)
                """, Long.class, approvalStatus, approvalStatus);
        return count == null ? 0 : count;
    }

    public List<TechnicianAdminSummary> findTechnicians(String approvalStatus, int limit, int offset) {
        return jdbcTemplate.query("""
                SELECT tp.id, tp.user_id, u.name, u.email, u.phone, tp.approval_status,
                       tp.active, tp.business_name, tp.license_info,
                       (SELECT string_agg(area.region_code, ', ' ORDER BY area.region_code)
                        FROM technician_service_area area
                        WHERE area.technician_id = tp.id AND area.active = true) AS service_areas,
                       (SELECT string_agg(appliance_type.code, ', ' ORDER BY appliance_type.code)
                        FROM technician_capability capability
                        JOIN appliance_type ON appliance_type.id = capability.appliance_type_id
                        WHERE capability.technician_id = tp.id AND capability.active = true) AS appliance_codes,
                       (SELECT string_agg(
                            availability.day_of_week || ':' || to_char(availability.start_time, 'HH24:MI')
                            || '-' || to_char(availability.end_time, 'HH24:MI'), ', '
                            ORDER BY availability.day_of_week, availability.start_time)
                        FROM technician_availability availability
                        WHERE availability.technician_id = tp.id AND availability.active = true)
                           AS availability
                FROM technician_profile tp
                JOIN app_user u ON u.id = tp.user_id
                WHERE (?::varchar IS NULL OR tp.approval_status = ?)
                ORDER BY tp.created_at DESC, tp.id
                LIMIT ? OFFSET ?
                """, (resultSet, rowNumber) -> new TechnicianAdminSummary(
                resultSet.getObject("id", UUID.class), resultSet.getObject("user_id", UUID.class),
                resultSet.getString("name"), resultSet.getString("email"), resultSet.getString("phone"),
                resultSet.getString("approval_status"), resultSet.getBoolean("active"),
                resultSet.getString("business_name"), resultSet.getString("license_info"),
                resultSet.getString("service_areas"), resultSet.getString("appliance_codes"),
                resultSet.getString("availability")), approvalStatus, approvalStatus, limit, offset);
    }

    public boolean updateTechnician(
            UUID profileId,
            String approvalStatus,
            boolean active,
            UUID actorUserId
    ) {
        Optional<TechnicianApprovalState> previous = jdbcTemplate.query("""
                SELECT approval_status, active
                FROM technician_profile
                WHERE id = ?
                FOR UPDATE
                """, resultSet -> resultSet.next()
                ? Optional.of(new TechnicianApprovalState(
                resultSet.getString("approval_status"), resultSet.getBoolean("active")))
                : Optional.empty(), profileId);
        if (previous.isEmpty()) {
            return false;
        }
        jdbcTemplate.update("""
                UPDATE technician_profile
                SET approval_status = ?, active = ?, updated_at = now()
                WHERE id = ?
                """, approvalStatus, active, profileId);
        jdbcTemplate.update("""
                INSERT INTO audit_log
                    (id, actor_user_id, action, entity_type, entity_id, before_summary, after_summary)
                VALUES (?, ?, 'TECHNICIAN_PROFILE_UPDATED', 'TECHNICIAN_PROFILE', ?,
                        jsonb_build_object('approvalStatus', ?, 'active', ?),
                        jsonb_build_object('approvalStatus', ?, 'active', ?))
                """, UUID.randomUUID(), actorUserId, profileId, previous.get().approvalStatus(),
                previous.get().active(), approvalStatus, active);
        return true;
    }

    private record TechnicianApprovalState(String approvalStatus, boolean active) {
    }
}
