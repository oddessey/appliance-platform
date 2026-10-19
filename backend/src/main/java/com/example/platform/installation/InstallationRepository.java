package com.example.platform.installation;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;

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
            UUID applianceTypeId
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
                INSERT INTO move_item (id, move_request_id, appliance_type_id, quantity)
                VALUES (?, ?, ?, 1)
                """, UUID.randomUUID(), id, applianceTypeId);

        return new InstallationResponse(id, requestNo, "SUBMITTED", createdAt);
    }
}
