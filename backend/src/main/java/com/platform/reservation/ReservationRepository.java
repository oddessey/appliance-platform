package com.platform.reservation;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ReservationRepository {

    private static final String RESERVATION_COLUMNS = """
            r.id, r.reservation_no, r.move_request_id, mr.request_no, r.quote_id, q.quote_no,
            r.status, r.scheduled_start_at, r.scheduled_end_at, r.customer_memo,
            r.confirmed_at, r.cancelled_at, r.created_at, customer.name AS customer_name,
            customer.phone AS customer_phone, mr.origin_address, mr.destination_address,
            q.total_amount AS quote_total_amount,
            (SELECT ta.status
             FROM technician_assignment ta
             WHERE ta.reservation_id = r.id
             ORDER BY ta.assigned_at DESC, ta.id DESC
             LIMIT 1) AS technician_assignment_status,
            (SELECT ta.id
             FROM technician_assignment ta
             WHERE ta.reservation_id = r.id
             ORDER BY ta.assigned_at DESC, ta.id DESC
             LIMIT 1) AS technician_assignment_id,
            (SELECT technician.name
             FROM technician_assignment ta
             JOIN technician_profile tp ON tp.id = ta.technician_id
             JOIN app_user technician ON technician.id = tp.user_id
             WHERE ta.reservation_id = r.id
             ORDER BY ta.assigned_at DESC, ta.id DESC
             LIMIT 1) AS technician_name
            """;

    private final JdbcTemplate jdbcTemplate;

    public ReservationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<ReservationRequestState> lockRequest(UUID requestId) {
        return jdbcTemplate.query("""
                SELECT mr.customer_id, mr.status,
                       EXISTS (
                           SELECT 1 FROM reservation r
                           WHERE r.move_request_id = mr.id
                             AND r.status IN ('PENDING', 'CONFIRMED', 'RESCHEDULE_REQUESTED')
                       ) AS has_active_reservation
                FROM move_request mr
                WHERE mr.id = ?
                FOR UPDATE OF mr
                """, resultSet -> resultSet.next()
                ? Optional.of(new ReservationRequestState(
                    resultSet.getObject("customer_id", UUID.class),
                    resultSet.getString("status"),
                    resultSet.getBoolean("has_active_reservation")))
                : Optional.empty(), requestId);
    }

    public Optional<ReservationRequestState> findRequest(UUID requestId) {
        return jdbcTemplate.query("""
                SELECT mr.customer_id, mr.status,
                       EXISTS (
                           SELECT 1 FROM reservation r
                           WHERE r.move_request_id = mr.id
                             AND r.status IN ('PENDING', 'CONFIRMED', 'RESCHEDULE_REQUESTED')
                       ) AS has_active_reservation
                FROM move_request mr
                WHERE mr.id = ?
                """, resultSet -> resultSet.next()
                ? Optional.of(new ReservationRequestState(
                    resultSet.getObject("customer_id", UUID.class),
                    resultSet.getString("status"),
                    resultSet.getBoolean("has_active_reservation")))
                : Optional.empty(), requestId);
    }

    public boolean isLatestAcceptedQuote(UUID requestId, UUID quoteId) {
        Boolean accepted = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM quote q
                    WHERE q.id = ? AND q.move_request_id = ? AND q.status = 'ACCEPTED'
                      AND q.version_no = (
                          SELECT max(latest.version_no)
                          FROM quote latest
                          WHERE latest.move_request_id = q.move_request_id
                      )
                )
                """, Boolean.class, quoteId, requestId);
        return Boolean.TRUE.equals(accepted);
    }

    public boolean isAvailable(UUID requestId, OffsetDateTime startAt, OffsetDateTime endAt, UUID excludeId) {
        Boolean available = jdbcTemplate.queryForObject("""
                SELECT NOT EXISTS (
                    SELECT 1 FROM reservation r
                    WHERE r.move_request_id = ?
                      AND r.status IN ('PENDING', 'CONFIRMED', 'RESCHEDULE_REQUESTED')
                      AND r.scheduled_start_at < ?
                      AND r.scheduled_end_at > ?
                      AND (?::uuid IS NULL OR r.id <> ?)
                )
                """, Boolean.class, requestId, endAt, startAt, excludeId, excludeId);
        return Boolean.TRUE.equals(available);
    }

    public Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey) {
        return jdbcTemplate.query("""
                SELECT r.id, r.idempotency_request_hash
                FROM reservation r
                WHERE r.idempotency_key = ?
                """, resultSet -> resultSet.next()
                ? Optional.of(new IdempotencyRecord(
                    resultSet.getObject("id", UUID.class),
                    resultSet.getString("idempotency_request_hash")))
                : Optional.empty(), idempotencyKey);
    }

    public Optional<UUID> createReservation(
            UUID id,
            String reservationNo,
            UUID requestId,
            UUID quoteId,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            String customerMemo,
            String idempotencyKey,
            String requestHash
    ) {
        return jdbcTemplate.query("""
                INSERT INTO reservation (
                    id, reservation_no, move_request_id, quote_id, status,
                    scheduled_start_at, scheduled_end_at, customer_memo, confirmed_at,
                    idempotency_key, idempotency_request_hash
                )
                VALUES (?, ?, ?, ?, 'CONFIRMED', ?, ?, ?, now(), ?, ?)
                ON CONFLICT (idempotency_key) WHERE idempotency_key IS NOT NULL DO NOTHING
                RETURNING id
                """, resultSet -> resultSet.next()
                ? Optional.of(resultSet.getObject("id", UUID.class))
                : Optional.empty(), id, reservationNo, requestId, quoteId, startAt, endAt,
                customerMemo, idempotencyKey, requestHash);
    }

    public Optional<ReservationResponse> findById(UUID reservationId) {
        return findOne(reservationId, null);
    }

    public Optional<ReservationResponse> findCustomerReservation(UUID reservationId, UUID customerId) {
        return jdbcTemplate.query("""
                SELECT
                """ + RESERVATION_COLUMNS + """
                FROM reservation r
                JOIN move_request mr ON mr.id = r.move_request_id
                JOIN quote q ON q.id = r.quote_id
                JOIN app_user customer ON customer.id = mr.customer_id
                WHERE r.id = ? AND mr.customer_id = ?
                """, resultSet -> resultSet.next()
                ? Optional.of(mapReservation(resultSet, 0))
                : Optional.empty(), reservationId, customerId);
    }

    public ReservationPageResponse findCustomerReservations(
            UUID customerId,
            UUID requestId,
            int page,
            int size
    ) {
        Long total = jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM reservation r
                JOIN move_request mr ON mr.id = r.move_request_id
                WHERE mr.customer_id = ?
                  AND (?::uuid IS NULL OR r.move_request_id = ?)
                """, Long.class, customerId, requestId, requestId);
        return new ReservationPageResponse(page, size, total == null ? 0 : total,
                jdbcTemplate.query("""
                        SELECT
                        """ + RESERVATION_COLUMNS + """
                        FROM reservation r
                        JOIN move_request mr ON mr.id = r.move_request_id
                        JOIN quote q ON q.id = r.quote_id
                        JOIN app_user customer ON customer.id = mr.customer_id
                        WHERE mr.customer_id = ?
                          AND (?::uuid IS NULL OR r.move_request_id = ?)
                        ORDER BY r.created_at DESC, r.id DESC
                        LIMIT ? OFFSET ?
                        """, this::mapReservation, customerId, requestId, requestId, size, page * size));
    }

    public ReservationPageResponse findAdminReservations(
            String status,
            String assignmentStatus,
            int page,
            int size
    ) {
        Long total = jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM reservation r
                LEFT JOIN LATERAL (
                    SELECT ta.status
                    FROM technician_assignment ta
                    WHERE ta.reservation_id = r.id
                    ORDER BY ta.assigned_at DESC, ta.id DESC
                    LIMIT 1
                ) current_assignment ON true
                WHERE (?::varchar IS NULL OR r.status = ?)
                  AND (?::varchar IS NULL OR current_assignment.status = ?)
                """, Long.class, status, status, assignmentStatus, assignmentStatus);
        return new ReservationPageResponse(page, size, total == null ? 0 : total,
                jdbcTemplate.query("""
                        SELECT
                        """ + RESERVATION_COLUMNS + """
                        FROM reservation r
                        JOIN move_request mr ON mr.id = r.move_request_id
                        JOIN quote q ON q.id = r.quote_id
                        JOIN app_user customer ON customer.id = mr.customer_id
                        LEFT JOIN LATERAL (
                            SELECT ta.status
                            FROM technician_assignment ta
                            WHERE ta.reservation_id = r.id
                            ORDER BY ta.assigned_at DESC, ta.id DESC
                            LIMIT 1
                        ) current_assignment ON true
                        WHERE (?::varchar IS NULL OR r.status = ?)
                          AND (?::varchar IS NULL OR current_assignment.status = ?)
                        ORDER BY r.scheduled_start_at, r.id
                        LIMIT ? OFFSET ?
                        """, this::mapReservation, status, status,
                assignmentStatus, assignmentStatus, size, page * size));
    }

    public Optional<ReservationState> lockReservation(UUID reservationId, UUID customerId) {
        return jdbcTemplate.query("""
                SELECT r.move_request_id, r.status, r.scheduled_start_at, r.scheduled_end_at, mr.customer_id
                FROM reservation r
                JOIN move_request mr ON mr.id = r.move_request_id
                WHERE r.id = ? AND (?::uuid IS NULL OR mr.customer_id = ?)
                FOR UPDATE OF mr, r
                """, resultSet -> resultSet.next()
                ? Optional.of(new ReservationState(
                    resultSet.getObject("move_request_id", UUID.class),
                    resultSet.getString("status"),
                    resultSet.getObject("scheduled_start_at", OffsetDateTime.class),
                    resultSet.getObject("scheduled_end_at", OffsetDateTime.class),
                    resultSet.getObject("customer_id", UUID.class)))
                : Optional.empty(), reservationId, customerId, customerId);
    }

    public Optional<ReservationState> findReservationState(UUID reservationId, UUID customerId) {
        return jdbcTemplate.query("""
                SELECT r.move_request_id, r.status, r.scheduled_start_at, r.scheduled_end_at, mr.customer_id
                FROM reservation r
                JOIN move_request mr ON mr.id = r.move_request_id
                WHERE r.id = ? AND mr.customer_id = ?
                """, resultSet -> resultSet.next()
                ? Optional.of(new ReservationState(
                    resultSet.getObject("move_request_id", UUID.class),
                    resultSet.getString("status"),
                    resultSet.getObject("scheduled_start_at", OffsetDateTime.class),
                    resultSet.getObject("scheduled_end_at", OffsetDateTime.class),
                    resultSet.getObject("customer_id", UUID.class)))
                : Optional.empty(), reservationId, customerId);
    }

    public boolean updateSchedule(UUID reservationId, OffsetDateTime startAt, OffsetDateTime endAt) {
        return jdbcTemplate.update("""
                UPDATE reservation
                SET scheduled_start_at = ?, scheduled_end_at = ?, updated_at = now()
                WHERE id = ? AND status = 'CONFIRMED'
                """, startAt, endAt, reservationId) == 1;
    }

    public boolean hasWorkStartedAssignment(UUID reservationId) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM technician_assignment
                    WHERE reservation_id = ? AND status IN ('WORK_STARTED', 'WORK_COMPLETED')
                )
                """, Boolean.class, reservationId);
        return Boolean.TRUE.equals(exists);
    }

    public boolean hasAcceptedAssignment(UUID reservationId) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM technician_assignment
                    WHERE reservation_id = ?
                      AND status IN ('ACCEPTED', 'ON_THE_WAY', 'WORK_STARTED', 'WORK_COMPLETED')
                )
                """, Boolean.class, reservationId);
        return Boolean.TRUE.equals(exists);
    }

    public void cancelOpenAssignments(UUID reservationId, UUID actorId, String reason) {
        List<UUID> assignmentIds = jdbcTemplate.query("""
                UPDATE technician_assignment
                SET status = 'CANCELLED', updated_at = now()
                WHERE reservation_id = ? AND status IN ('PENDING', 'ACCEPTED', 'ON_THE_WAY')
                RETURNING id
                """, (resultSet, rowNumber) -> resultSet.getObject("id", UUID.class), reservationId);
        for (UUID assignmentId : assignmentIds) {
            jdbcTemplate.update("""
                    INSERT INTO work_event (id, assignment_id, event_type, note, created_by)
                    VALUES (?, ?, 'CANCELLED', ?, ?)
                    """, UUID.randomUUID(), assignmentId, reason, actorId);
        }
    }

    public boolean markRequestReserved(UUID requestId) {
        return jdbcTemplate.update("""
                UPDATE move_request
                SET status = 'RESERVED', updated_at = now()
                WHERE id = ? AND status = 'QUOTED'
                """, requestId) == 1;
    }

    public boolean cancelReservation(UUID reservationId) {
        return jdbcTemplate.update("""
                UPDATE reservation
                SET status = 'CANCELLED', cancelled_at = now(), updated_at = now()
                WHERE id = ? AND status = 'CONFIRMED'
                """, reservationId) == 1;
    }

    public void returnRequestToQuoted(UUID requestId) {
        jdbcTemplate.update("""
                UPDATE move_request
                SET status = 'QUOTED', updated_at = now()
                WHERE id = ? AND status = 'RESERVED'
                """, requestId);
    }

    public void recordAudit(
            UUID actorId,
            UUID reservationId,
            String action,
            String beforeStatus,
            String afterStatus,
            OffsetDateTime startAt,
            OffsetDateTime endAt
    ) {
        jdbcTemplate.update("""
                INSERT INTO audit_log (
                    id, actor_user_id, action, entity_type, entity_id, before_summary, after_summary
                )
                VALUES (?, ?, ?, 'RESERVATION', ?, jsonb_build_object(
                    'status', CAST(? AS text)
                ), jsonb_build_object(
                    'status', CAST(? AS text),
                    'scheduledStartAt', CAST(? AS text),
                    'scheduledEndAt', CAST(? AS text)
                ))
                """, UUID.randomUUID(), actorId, action, reservationId, beforeStatus,
                afterStatus, startAt == null ? null : startAt.toString(),
                endAt == null ? null : endAt.toString());
    }

    public void recordScheduleAudit(
            UUID actorId,
            UUID reservationId,
            String action,
            OffsetDateTime beforeStartAt,
            OffsetDateTime beforeEndAt,
            OffsetDateTime afterStartAt,
            OffsetDateTime afterEndAt
    ) {
        jdbcTemplate.update("""
                INSERT INTO audit_log (
                    id, actor_user_id, action, entity_type, entity_id, before_summary, after_summary
                )
                VALUES (?, ?, ?, 'RESERVATION', ?, jsonb_build_object(
                    'scheduledStartAt', CAST(? AS text),
                    'scheduledEndAt', CAST(? AS text)
                ), jsonb_build_object(
                    'scheduledStartAt', CAST(? AS text),
                    'scheduledEndAt', CAST(? AS text)
                ))
                """, UUID.randomUUID(), actorId, action, reservationId,
                beforeStartAt.toString(), beforeEndAt.toString(),
                afterStartAt.toString(), afterEndAt.toString());
    }

    public List<ExpiredReservation> expirePendingReservations(OffsetDateTime cutoff) {
        return jdbcTemplate.query("""
                WITH expired AS (
                    UPDATE reservation
                    SET status = 'EXPIRED', updated_at = now()
                    WHERE status = 'PENDING' AND created_at <= ?
                    RETURNING id, move_request_id
                )
                SELECT id, move_request_id FROM expired
                """, (resultSet, rowNumber) -> new ExpiredReservation(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("move_request_id", UUID.class)), cutoff);
    }

    private Optional<ReservationResponse> findOne(UUID reservationId, UUID customerId) {
        return jdbcTemplate.query("""
                SELECT
                """ + RESERVATION_COLUMNS + """
                FROM reservation r
                JOIN move_request mr ON mr.id = r.move_request_id
                JOIN quote q ON q.id = r.quote_id
                JOIN app_user customer ON customer.id = mr.customer_id
                WHERE r.id = ? AND (?::uuid IS NULL OR mr.customer_id = ?)
                """, resultSet -> resultSet.next()
                ? Optional.of(mapReservation(resultSet, 0))
                : Optional.empty(), reservationId, customerId, customerId);
    }

    private ReservationResponse mapReservation(java.sql.ResultSet resultSet, int rowNumber)
            throws java.sql.SQLException {
        return new ReservationResponse(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("reservation_no"),
                resultSet.getObject("move_request_id", UUID.class),
                resultSet.getString("request_no"),
                resultSet.getObject("quote_id", UUID.class),
                resultSet.getString("quote_no"),
                resultSet.getString("status"),
                resultSet.getObject("scheduled_start_at", OffsetDateTime.class),
                resultSet.getObject("scheduled_end_at", OffsetDateTime.class),
                resultSet.getString("customer_memo"),
                resultSet.getObject("confirmed_at", OffsetDateTime.class),
                resultSet.getObject("cancelled_at", OffsetDateTime.class),
                resultSet.getObject("created_at", OffsetDateTime.class),
                resultSet.getString("customer_name"),
                resultSet.getString("customer_phone"),
                resultSet.getString("origin_address"),
                resultSet.getString("destination_address"),
                resultSet.getBigDecimal("quote_total_amount"),
                resultSet.getString("technician_assignment_status"),
                resultSet.getObject("technician_assignment_id", UUID.class),
                resultSet.getString("technician_name"));
    }

    public record ReservationRequestState(UUID customerId, String status, boolean hasActiveReservation) {
    }

    public record ReservationState(
            UUID requestId,
            String status,
            OffsetDateTime scheduledStartAt,
            OffsetDateTime scheduledEndAt,
            UUID customerId
    ) {
    }

    public record IdempotencyRecord(UUID reservationId, String requestHash) {
    }

    public record ExpiredReservation(UUID reservationId, UUID requestId) {
    }
}
