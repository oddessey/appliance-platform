package com.platform.technician.mapper;

import com.platform.technician.TechnicianAvailabilityResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Mapper
public interface TechnicianProfileMapper {

    TechnicianProfileRow findByUserId(@Param("userId") UUID userId);

    UUID upsertProfile(
            @Param("id") UUID id,
            @Param("userId") UUID userId,
            @Param("businessName") String businessName,
            @Param("licenseInfo") String licenseInfo);

    void recordLicenseInformationChange(
            @Param("id") UUID id,
            @Param("userId") UUID userId,
            @Param("profileId") UUID profileId,
            @Param("previousApprovalStatus") String previousApprovalStatus,
            @Param("previousActive") boolean previousActive);

    void deleteServiceAreas(@Param("profileId") UUID profileId);

    void insertServiceArea(
            @Param("id") UUID id, @Param("profileId") UUID profileId, @Param("region") String region);

    void deleteCapabilities(@Param("profileId") UUID profileId);

    void insertCapability(@Param("profileId") UUID profileId, @Param("code") String code);

    void deleteAvailability(@Param("profileId") UUID profileId);

    void insertAvailability(
            @Param("id") UUID id,
            @Param("profileId") UUID profileId,
            @Param("dayOfWeek") int dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    List<String> findServiceAreas(@Param("profileId") UUID profileId);

    List<String> findCapabilities(@Param("profileId") UUID profileId);

    List<TechnicianAvailabilityResponse> findAvailability(@Param("profileId") UUID profileId);

    record TechnicianProfileRow(
            UUID id, String approvalStatus, boolean active, String businessName, String licenseInfo) {
    }
}
