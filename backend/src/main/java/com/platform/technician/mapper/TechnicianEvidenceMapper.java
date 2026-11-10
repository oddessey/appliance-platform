package com.platform.technician.mapper;

import com.platform.technician.TechnicianEvidenceFileRow;
import com.platform.technician.TechnicianEvidenceResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface TechnicianEvidenceMapper {

    String lockAssignmentStatus(@Param("assignmentId") UUID assignmentId, @Param("profileId") UUID profileId);

    boolean assignmentExists(@Param("assignmentId") UUID assignmentId, @Param("profileId") UUID profileId);

    TechnicianEvidenceResponse insertEvidence(
            @Param("imageId") UUID imageId,
            @Param("assignmentId") UUID assignmentId,
            @Param("profileId") UUID profileId,
            @Param("userId") UUID userId,
            @Param("objectKey") String objectKey,
            @Param("filename") String filename,
            @Param("contentType") String contentType,
            @Param("sizeBytes") long sizeBytes);

    void recordEvidenceEvent(
            @Param("id") UUID id,
            @Param("assignmentId") UUID assignmentId,
            @Param("imageId") UUID imageId,
            @Param("userId") UUID userId);

    List<TechnicianEvidenceResponse> listEvidence(
            @Param("assignmentId") UUID assignmentId,
            @Param("profileId") UUID profileId,
            @Param("userId") UUID userId);

    List<TechnicianEvidenceResponse> listEvidenceForAdmin(@Param("assignmentId") UUID assignmentId);

    TechnicianEvidenceFileRow findEvidenceFile(
            @Param("assignmentId") UUID assignmentId,
            @Param("profileId") UUID profileId,
            @Param("evidenceId") UUID evidenceId,
            @Param("userId") UUID userId);

    TechnicianEvidenceFileRow findEvidenceFileForAdmin(
            @Param("assignmentId") UUID assignmentId, @Param("evidenceId") UUID evidenceId);
}
