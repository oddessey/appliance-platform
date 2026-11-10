package com.platform.reservation;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservationRepositoryMapperTest {

    @Test
    void mapperXmlParsesAndKeepsLockingAndReturningStatements() throws Exception {
        Configuration configuration = loadMapperConfiguration();

        String requestLockSql = configuration.getMappedStatement(
                "com.platform.reservation.ReservationMapper.lockRequest")
                .getBoundSql(Map.of("requestId", UUID.randomUUID())).getSql();
        Map<String, Object> reservationLockParameters = new HashMap<>();
        reservationLockParameters.put("reservationId", UUID.randomUUID());
        reservationLockParameters.put("customerId", null);
        String reservationLockSql = configuration.getMappedStatement(
                "com.platform.reservation.ReservationMapper.lockReservation")
                .getBoundSql(reservationLockParameters).getSql();

        assertTrue(requestLockSql.contains("FOR UPDATE OF mr"));
        assertTrue(reservationLockSql.contains("FOR UPDATE OF mr, r"));
        assertTrue(configuration.getMappedStatement(
                "com.platform.reservation.ReservationMapper.createReservation")
                .getBoundSql(createReservationParameters()).getSql().contains("RETURNING id"));
        assertTrue(configuration.getMappedStatement(
                "com.platform.reservation.ReservationMapper.cancelOpenAssignments")
                .getBoundSql(Map.of("reservationId", UUID.randomUUID())).getSql().contains("RETURNING id"));
        assertTrue(configuration.getMappedStatement(
                "com.platform.reservation.ReservationMapper.expirePendingReservations")
                .getBoundSql(Map.of("cutoff", java.time.OffsetDateTime.now())).getSql().contains("RETURNING id"));
    }

    @Test
    void mapperXmlPreservesOptionalReservationFiltersAndCustomResultMappings() throws Exception {
        Configuration configuration = loadMapperConfiguration();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("status", null);
        parameters.put("assignmentStatus", null);
        parameters.put("limit", 20);
        parameters.put("offset", 0);

        String unfilteredSql = configuration.getMappedStatement(
                "com.platform.reservation.ReservationMapper.findAdminReservations")
                .getBoundSql(parameters).getSql();
        assertFalse(unfilteredSql.contains("r.status ="));
        assertFalse(unfilteredSql.contains("current_assignment.status ="));

        parameters.put("status", "CONFIRMED");
        parameters.put("assignmentStatus", "ACCEPTED");
        String filteredSql = configuration.getMappedStatement(
                "com.platform.reservation.ReservationMapper.findAdminReservations")
                .getBoundSql(parameters).getSql();
        assertTrue(filteredSql.contains("r.status = ?"));
        assertTrue(filteredSql.contains("current_assignment.status = ?"));
        assertEquals(21, configuration.getResultMap(
                "com.platform.reservation.ReservationMapper.reservationResponse").getConstructorResultMappings().size());
        assertEquals(3, configuration.getResultMap(
                "com.platform.reservation.ReservationMapper.reservationRequestState").getConstructorResultMappings().size());
    }

    private static Configuration loadMapperConfiguration() throws Exception {
        Configuration configuration = new Configuration();
        try (InputStream input = Resources.getResourceAsStream("mapper/ReservationMapper.xml")) {
            new XMLMapperBuilder(input, configuration, "mapper/ReservationMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
        return configuration;
    }

    private static Map<String, Object> createReservationParameters() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", UUID.randomUUID());
        parameters.put("reservationNo", "RSV-TEST");
        parameters.put("requestId", UUID.randomUUID());
        parameters.put("quoteId", UUID.randomUUID());
        parameters.put("startAt", java.time.OffsetDateTime.now());
        parameters.put("endAt", java.time.OffsetDateTime.now().plusHours(1));
        parameters.put("customerMemo", null);
        parameters.put("idempotencyKey", "key");
        parameters.put("requestHash", "hash");
        return parameters;
    }
}
