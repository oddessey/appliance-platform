package com.platform.installation;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Mapper
public interface InstallationMapper {

    OffsetDateTime insertMoveRequest(
            @Param("id") UUID id,
            @Param("requestNo") String requestNo,
            @Param("customerId") UUID customerId,
            @Param("originAddress") String originAddress,
            @Param("destinationAddress") String destinationAddress,
            @Param("desiredStartAt") OffsetDateTime desiredStartAt
    );

    int insertMoveItem(
            @Param("id") UUID id,
            @Param("moveRequestId") UUID moveRequestId,
            @Param("applianceTypeId") UUID applianceTypeId,
            @Param("carryingDistanceMeters") int carryingDistanceMeters,
            @Param("floorCount") int floorCount,
            @Param("additionalPipingMeters") BigDecimal additionalPipingMeters
    );

    List<InstallationHistoryItem> findByCustomerId(
            @Param("customerId") UUID customerId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    Long countByCustomerId(@Param("customerId") UUID customerId);

    List<AdminInstallationSummary> findForAdmin(
            @Param("limit") int limit,
            @Param("offset") int offset,
            @Param("reviewRequired") boolean reviewRequired
    );

    Long countForAdmin(@Param("reviewRequired") boolean reviewRequired);
}
