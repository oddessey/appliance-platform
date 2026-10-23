package com.platform.reservation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationRepositorySqlTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void customerReservationListSqlKeepsSelectAndColumnsSeparated() {
        ReservationRepository repository = new ReservationRepository(jdbcTemplate);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(), any(), any())).thenReturn(0L);
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<ReservationResponse>>any(),
                any(), any(), any(), any(), any())).thenReturn(List.of());

        repository.findCustomerReservations(UUID.randomUUID(), null, 0, 20);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sql.capture(), ArgumentMatchers.<RowMapper<ReservationResponse>>any(),
                any(), any(), any(), any(), any());
        assertSelectAndColumnsSeparated(sql.getValue());
    }

    @Test
    void reservationDetailSqlKeepsSelectAndColumnsSeparated() {
        ReservationRepository repository = new ReservationRepository(jdbcTemplate);
        when(jdbcTemplate.query(anyString(),
                ArgumentMatchers.<ResultSetExtractor<Optional<ReservationResponse>>>any(),
                any(), any(), any())).thenReturn(Optional.empty());

        repository.findById(UUID.randomUUID());

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sql.capture(),
                ArgumentMatchers.<ResultSetExtractor<Optional<ReservationResponse>>>any(),
                any(), any(), any());
        assertSelectAndColumnsSeparated(sql.getValue());
    }

    private static void assertSelectAndColumnsSeparated(String sql) {
        String normalized = sql.replaceAll("\\s+", " ");
        assertFalse(sql.contains("SELECTr"), "SELECT must not be glued to the column list");
        assertTrue(normalized.startsWith("SELECT r.id, r.reservation_no"),
                "select list must be separated from SELECT keyword");
        assertTrue(normalized.contains("technician_name FROM reservation r"),
                "column list must be separated from FROM clause");
    }
}
