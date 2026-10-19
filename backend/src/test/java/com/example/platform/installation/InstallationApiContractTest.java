package com.example.platform.installation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InstallationApiContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void requestJsonUsesDocumentedFieldNamesAndOffsetDateTime() throws Exception {
        CreateInstallationRequest request = objectMapper.readValue("""
                {
                  "applianceTypeCode": "WASHING_MACHINE",
                  "originAddress": "서울특별시 강남구 테헤란로 1",
                  "destinationAddress": "경기도 성남시 분당구 판교로 2",
                  "desiredStartAt": "2030-11-02T09:00:00+09:00"
                }
                """, CreateInstallationRequest.class);

        assertEquals("WASHING_MACHINE", request.applianceTypeCode());
        assertEquals(
                OffsetDateTime.parse("2030-11-02T09:00:00+09:00").toInstant(),
                request.desiredStartAt().toInstant()
        );
    }

    @Test
    void responseJsonUsesDocumentedFieldNames() throws Exception {
        InstallationResponse response = new InstallationResponse(
                UUID.fromString("c8d4c8c0-89b5-48b6-9b13-0f93b7df4f3c"),
                "INS-1A2B3C4D5E6F",
                "SUBMITTED",
                OffsetDateTime.parse("2026-10-19T10:20:30+09:00")
        );

        var json = objectMapper.valueToTree(response);

        assertEquals(response.id().toString(), json.get("id").asText());
        assertEquals(response.requestNo(), json.get("requestNo").asText());
        assertEquals(response.status(), json.get("status").asText());
        assertEquals(response.createdAt().toString(), json.get("createdAt").asText());
    }
}
