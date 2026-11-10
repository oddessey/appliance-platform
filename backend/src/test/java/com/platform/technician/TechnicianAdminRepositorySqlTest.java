package com.platform.technician;

import com.platform.reservation.UuidTypeHandler;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TechnicianAdminRepositorySqlTest {

    @Test
    void candidateMapperNormalizesPostalCodeAndProvinceAliases() throws Exception {
        Configuration configuration = new Configuration();
        configuration.getTypeHandlerRegistry().register(UuidTypeHandler.class);
        String resource = "mapper/technician/TechnicianAdminMapper.xml";
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }

        String sql = configuration.getMappedStatement(
                "com.platform.technician.mapper.TechnicianAdminMapper.findCandidates")
                .getBoundSql(Map.of("reservationId", UUID.randomUUID()))
                .getSql()
                .replaceAll("\\s+", " ");

        assertTrue(sql.contains("'^\\[[0-9]+\\][[:space:]]*'"),
                "postal code prefix must be stripped from reservation addresses");
        assertTrue(sql.contains("WHEN '서울' THEN '서울특별시'"),
                "short province names must be normalized to official names");
        assertTrue(sql.contains("region.district IS NULL OR region.district = target.origin_district"),
                "province-only service areas must cover the whole province");
        assertTrue(sql.contains("region.district IS NULL OR region.district = target.destination_district"),
                "province-only service areas must cover the whole province for destination");
    }
}
