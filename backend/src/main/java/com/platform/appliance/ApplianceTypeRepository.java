package com.platform.appliance;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ApplianceTypeRepository {

    private final JdbcTemplate jdbcTemplate;

    public ApplianceTypeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<ApplianceType> findActiveByCode(String code) {
        return jdbcTemplate.query("""
                SELECT id, code, name
                FROM appliance_type
                WHERE code = ? AND active = true
                """, (resultSet, rowNumber) -> new ApplianceType(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("code"),
                resultSet.getString("name")
        ), code).stream().findFirst();
    }

    public List<ApplianceTypeResponse> findAllActive() {
        return jdbcTemplate.query("""
                SELECT code, name
                FROM appliance_type
                WHERE active = true
                ORDER BY display_order, name
                """, (resultSet, rowNumber) -> new ApplianceTypeResponse(
                resultSet.getString("code"),
                resultSet.getString("name")
        ));
    }
}
