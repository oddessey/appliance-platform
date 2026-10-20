package com.platform.technician;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Time;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TechnicianProfileRepository {

    private final JdbcTemplate jdbcTemplate;

    public TechnicianProfileRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<TechnicianProfileResponse> findByUserId(UUID userId) {
        return jdbcTemplate.query("""
                SELECT id, approval_status, active, business_name, license_info
                FROM technician_profile
                WHERE user_id = ?
                """, resultSet -> {
            if (!resultSet.next()) {
                return Optional.empty();
            }
            UUID profileId = resultSet.getObject("id", UUID.class);
            return Optional.of(new TechnicianProfileResponse(
                    profileId, resultSet.getString("approval_status"), resultSet.getBoolean("active"),
                    resultSet.getString("business_name"), resultSet.getString("license_info"),
                    findServiceAreas(profileId), findCapabilities(profileId), findAvailability(profileId)));
        }, userId);
    }

    public UUID upsertProfile(UUID userId, String businessName, String licenseInfo) {
        return jdbcTemplate.query("""
                INSERT INTO technician_profile (id, user_id, approval_status, business_name, license_info, active)
                VALUES (?, ?, 'PENDING', ?, ?, false)
                ON CONFLICT (user_id) DO UPDATE
                SET business_name = EXCLUDED.business_name, license_info = EXCLUDED.license_info,
                    approval_status = CASE
                        WHEN technician_profile.license_info IS DISTINCT FROM EXCLUDED.license_info
                            THEN 'PENDING'
                        ELSE technician_profile.approval_status
                    END,
                    active = CASE
                        WHEN technician_profile.license_info IS DISTINCT FROM EXCLUDED.license_info
                            THEN false
                        ELSE technician_profile.active
                    END,
                    updated_at = now()
                RETURNING id
                """, resultSet -> {
            if (!resultSet.next()) {
                throw new IllegalStateException("Technician profile upsert did not return an id");
            }
            return resultSet.getObject(1, UUID.class);
        }, UUID.randomUUID(), userId, businessName, licenseInfo);
    }

    public void recordLicenseInformationChange(
            UUID userId,
            UUID profileId,
            String previousApprovalStatus,
            boolean previousActive
    ) {
        jdbcTemplate.update("""
                INSERT INTO audit_log
                    (id, actor_user_id, action, entity_type, entity_id, before_summary, after_summary)
                VALUES (?, ?, 'TECHNICIAN_CREDENTIALS_CHANGED', 'TECHNICIAN_PROFILE', ?,
                        jsonb_build_object('approvalStatus', ?, 'active', ?),
                        jsonb_build_object('approvalStatus', 'PENDING', 'active', false,
                                           'credentialsUpdated', true))
                """, UUID.randomUUID(), userId, profileId, previousApprovalStatus, previousActive);
    }

    public void replaceServiceAreas(UUID profileId, List<String> serviceAreas) {
        jdbcTemplate.update("DELETE FROM technician_service_area WHERE technician_id = ?", profileId);
        for (String region : serviceAreas) {
            jdbcTemplate.update("""
                    INSERT INTO technician_service_area (id, technician_id, region_code, active)
                    VALUES (?, ?, ?, true)
                    """, UUID.randomUUID(), profileId, region);
        }
    }

    public void replaceCapabilities(UUID profileId, List<String> applianceCodes) {
        jdbcTemplate.update("DELETE FROM technician_capability WHERE technician_id = ?", profileId);
        for (String code : applianceCodes) {
            jdbcTemplate.update("""
                    INSERT INTO technician_capability (technician_id, appliance_type_id, active)
                    SELECT ?, id, true FROM appliance_type WHERE code = ? AND active = true
                    """, profileId, code);
        }
    }

    public void replaceAvailability(UUID profileId, List<TechnicianAvailabilityResponse> availability) {
        jdbcTemplate.update("DELETE FROM technician_availability WHERE technician_id = ?", profileId);
        for (TechnicianAvailabilityResponse slot : availability) {
            jdbcTemplate.update("""
                    INSERT INTO technician_availability
                        (id, technician_id, day_of_week, start_time, end_time, active)
                    VALUES (?, ?, ?, ?, ?, true)
                    """, UUID.randomUUID(), profileId, slot.dayOfWeek(),
                    Time.valueOf(slot.startTime()), Time.valueOf(slot.endTime()));
        }
    }

    private List<String> findServiceAreas(UUID profileId) {
        return jdbcTemplate.queryForList("""
                SELECT region_code FROM technician_service_area
                WHERE technician_id = ? AND active = true ORDER BY region_code
                """, String.class, profileId);
    }

    private List<String> findCapabilities(UUID profileId) {
        return jdbcTemplate.queryForList("""
                SELECT appliance_type.code
                FROM technician_capability
                JOIN appliance_type ON appliance_type.id = technician_capability.appliance_type_id
                WHERE technician_capability.technician_id = ? AND technician_capability.active = true
                ORDER BY appliance_type.display_order, appliance_type.name
                """, String.class, profileId);
    }

    private List<TechnicianAvailabilityResponse> findAvailability(UUID profileId) {
        return jdbcTemplate.query("""
                SELECT day_of_week, start_time, end_time
                FROM technician_availability
                WHERE technician_id = ? AND active = true
                ORDER BY day_of_week, start_time
                """, (resultSet, rowNumber) -> new TechnicianAvailabilityResponse(
                resultSet.getInt("day_of_week"), resultSet.getTime("start_time").toLocalTime(),
                resultSet.getTime("end_time").toLocalTime()), profileId);
    }
}
