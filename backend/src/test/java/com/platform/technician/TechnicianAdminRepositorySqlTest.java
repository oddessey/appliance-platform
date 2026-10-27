package com.platform.technician;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TechnicianAdminRepositorySqlTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void candidateSqlNormalizesPostalCodeAndProvinceAliases() {
        TechnicianAdminRepository repository = new TechnicianAdminRepository(jdbcTemplate);
        when(jdbcTemplate.query(anyString(),
                ArgumentMatchers.<RowMapper<TechnicianCandidateResponse>>any(), any(), any()))
                .thenReturn(List.of());

        repository.findCandidates(UUID.randomUUID());

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sql.capture(),
                ArgumentMatchers.<RowMapper<TechnicianCandidateResponse>>any(), any(), any());
        String normalized = sql.getValue().replaceAll("\\s+", " ");
        assertTrue(normalized.contains("'^\\[[0-9]+\\][[:space:]]*'"),
                "postal code prefix must be stripped from reservation addresses");
        assertTrue(normalized.contains("WHEN '서울' THEN '서울특별시'"),
                "short province names must be normalized to official names");
        assertTrue(normalized.contains("region.district IS NULL OR region.district = target.origin_district"),
                "province-only service areas must cover the whole province");
        assertTrue(normalized.contains("region.district IS NULL OR region.district = target.destination_district"),
                "province-only service areas must cover the whole province for destination");
    }
}
