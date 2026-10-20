package com.platform.installation;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class InstallationRepository {

    private final JdbcTemplate jdbcTemplate;

    public InstallationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public InstallationResponse create(
            UUID id,
            String requestNo,
            UUID customerId,
            String originAddress,
            String destinationAddress,
            OffsetDateTime desiredStartAt,
            UUID applianceTypeId,
            int carryingDistanceMeters,
            int floorCount,
            BigDecimal additionalPipingMeters
    ) {
        OffsetDateTime createdAt = jdbcTemplate.queryForObject("""
                INSERT INTO move_request (
                    id, request_no, customer_id, status, origin_address,
                    destination_address, desired_start_at, submitted_at
                )
                VALUES (?, ?, ?, 'SUBMITTED', ?, ?, ?, now())
                RETURNING created_at
                """, (resultSet, rowNumber) -> resultSet.getObject("created_at", OffsetDateTime.class),
                id, requestNo, customerId, originAddress, destinationAddress, desiredStartAt);

        jdbcTemplate.update("""
                INSERT INTO move_item (
                    id, move_request_id, appliance_type_id, quantity, installation_conditions
                )
                VALUES (?, ?, ?, 1, jsonb_build_object(
                    'carryingDistanceMeters', ?,
                    'floorCount', ?,
                    'additionalPipingMeters', ?
                ))
                """, UUID.randomUUID(), id, applianceTypeId,
                carryingDistanceMeters, floorCount, additionalPipingMeters);

        return new InstallationResponse(id, requestNo, "SUBMITTED", createdAt);
    }

    public List<InstallationHistoryItem> findByCustomerId(UUID customerId, int limit, int offset) {
        return jdbcTemplate.query("""
                SELECT mr.id, mr.request_no, mr.status, at.code AS appliance_type_code,
                       at.name AS appliance_type_name, mr.origin_address, mr.destination_address,
                       mr.desired_start_at, mr.created_at
                FROM move_request mr
                JOIN move_item mi ON mi.move_request_id = mr.id
                JOIN appliance_type at ON at.id = mi.appliance_type_id
                WHERE mr.customer_id = ?
                ORDER BY mr.created_at DESC, mr.id
                LIMIT ? OFFSET ?
                """, (resultSet, rowNumber) -> new InstallationHistoryItem(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("request_no"),
                resultSet.getString("status"),
                resultSet.getString("appliance_type_code"),
                resultSet.getString("appliance_type_name"),
                resultSet.getString("origin_address"),
                resultSet.getString("destination_address"),
                resultSet.getObject("desired_start_at", OffsetDateTime.class),
                resultSet.getObject("created_at", OffsetDateTime.class)
        ), customerId, limit, offset);
    }

    public long countByCustomerId(UUID customerId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM move_request WHERE customer_id = ?
                """, Long.class, customerId);
        return count == null ? 0 : count;
    }

    public List<AdminInstallationSummary> findForAdmin(int limit, int offset, boolean reviewRequired) {
        return jdbcTemplate.query("""
                SELECT mr.id, mr.request_no, u.name AS customer_name, u.email AS customer_email,
                       mr.status, item_summary.appliance_summary, mr.origin_address,
                       mr.destination_address, mr.desired_start_at, mr.submitted_at, mr.created_at,
                       current_reservation.id AS reservation_id,
                       current_reservation.reservation_no,
                       current_reservation.status AS reservation_status,
                       current_assignment.status AS technician_assignment_status,
                       current_assignment.technician_profile_id,
                       current_assignment.technician_name,
                       analysis.id AS analysis_id, analysis.status AS analysis_status,
                       analysis.provider AS analysis_provider, analysis.model_name AS analysis_model_name,
                       analysis.confidence AS analysis_confidence, analysis.result_json::text AS analysis_result_json,
                       analysis.error_code AS analysis_error_code, analysis.error_message AS analysis_error_message,
                       analysis.created_at AS analysis_created_at
                FROM move_request mr
                JOIN app_user u ON u.id = mr.customer_id
                LEFT JOIN LATERAL (
                    SELECT string_agg(at.name || ' × ' || mi.quantity, ', ' ORDER BY at.name) AS appliance_summary
                    FROM move_item mi
                    JOIN appliance_type at ON at.id = mi.appliance_type_id
                    WHERE mi.move_request_id = mr.id
                ) item_summary ON true
                LEFT JOIN LATERAL (
                    SELECT r.id, r.reservation_no, r.status
                    FROM reservation r
                    WHERE r.move_request_id = mr.id
                    ORDER BY CASE WHEN r.status IN ('PENDING', 'CONFIRMED', 'RESCHEDULE_REQUESTED')
                                  THEN 0 ELSE 1 END,
                             r.created_at DESC, r.id DESC
                    LIMIT 1
                ) current_reservation ON true
                LEFT JOIN LATERAL (
                    SELECT ta.status, tp.id AS technician_profile_id, technician.name AS technician_name
                    FROM technician_assignment ta
                    JOIN technician_profile tp ON tp.id = ta.technician_id
                    JOIN app_user technician ON technician.id = tp.user_id
                    WHERE ta.reservation_id = current_reservation.id
                    ORDER BY ta.assigned_at DESC, ta.id DESC
                    LIMIT 1
                ) current_assignment ON true
                LEFT JOIN LATERAL (
                    SELECT aa.id, aa.status, aa.provider, aa.model_name, aa.confidence,
                           aa.result_json, aa.error_code, aa.error_message, aa.created_at
                    FROM ai_analysis aa
                    WHERE aa.move_request_id = mr.id
                    ORDER BY aa.created_at DESC, aa.id DESC
                    LIMIT 1
                ) analysis ON true
                WHERE (NOT ? OR mr.status = 'NEEDS_REVIEW'
                       OR analysis.status IN ('FAILED', 'NEEDS_REVIEW'))
                ORDER BY mr.created_at DESC, mr.id DESC
                LIMIT ? OFFSET ?
                """, (resultSet, rowNumber) -> mapAdminInstallation(resultSet), reviewRequired, limit, offset);
    }

    public long countForAdmin(boolean reviewRequired) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM move_request mr
                LEFT JOIN LATERAL (
                    SELECT aa.status
                    FROM ai_analysis aa
                    WHERE aa.move_request_id = mr.id
                    ORDER BY aa.created_at DESC, aa.id DESC
                    LIMIT 1
                ) analysis ON true
                WHERE (NOT ? OR mr.status = 'NEEDS_REVIEW'
                       OR analysis.status IN ('FAILED', 'NEEDS_REVIEW'))
                """, Long.class, reviewRequired);
        return count == null ? 0 : count;
    }

    private AdminInstallationSummary mapAdminInstallation(ResultSet resultSet) throws SQLException {
        UUID analysisId = resultSet.getObject("analysis_id", UUID.class);
        AdminAiAnalysisSummary analysis = analysisId == null ? null : new AdminAiAnalysisSummary(
                analysisId,
                resultSet.getString("analysis_status"),
                resultSet.getString("analysis_provider"),
                resultSet.getString("analysis_model_name"),
                resultSet.getBigDecimal("analysis_confidence"),
                resultSet.getString("analysis_result_json"),
                resultSet.getString("analysis_error_code"),
                resultSet.getString("analysis_error_message"),
                resultSet.getObject("analysis_created_at", OffsetDateTime.class)
        );
        return new AdminInstallationSummary(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("request_no"),
                resultSet.getString("customer_name"),
                resultSet.getString("customer_email"),
                resultSet.getString("status"),
                resultSet.getString("appliance_summary"),
                resultSet.getString("origin_address"),
                resultSet.getString("destination_address"),
                resultSet.getObject("desired_start_at", OffsetDateTime.class),
                resultSet.getObject("submitted_at", OffsetDateTime.class),
                resultSet.getObject("created_at", OffsetDateTime.class),
                resultSet.getObject("reservation_id", UUID.class),
                resultSet.getString("reservation_no"),
                resultSet.getString("reservation_status"),
                resultSet.getString("technician_assignment_status"),
                resultSet.getObject("technician_profile_id", UUID.class),
                resultSet.getString("technician_name"),
                analysis
        );
    }
}
