package com.platform.installation;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface MoveRequestImageMapper {

    Boolean isOwnedBy(
            @Param("moveRequestId") UUID moveRequestId,
            @Param("customerId") UUID customerId
    );

    Boolean hasAccess(
            @Param("moveRequestId") UUID moveRequestId,
            @Param("userId") UUID userId
    );

    Boolean exists(@Param("moveRequestId") UUID moveRequestId);

    Boolean moveItemBelongsToRequest(
            @Param("moveItemId") UUID moveItemId,
            @Param("moveRequestId") UUID moveRequestId
    );

    List<MoveRequestImageResponse> findCustomerImages(@Param("moveRequestId") UUID moveRequestId);

    MoveRequestImageFileRow findCustomerImageFile(
            @Param("moveRequestId") UUID moveRequestId,
            @Param("imageId") UUID imageId
    );

    MoveRequestImageResponse insert(
            @Param("imageId") UUID imageId,
            @Param("moveRequestId") UUID moveRequestId,
            @Param("moveItemId") UUID moveItemId,
            @Param("objectKey") String objectKey,
            @Param("originalFilename") String originalFilename,
            @Param("contentType") String contentType,
            @Param("sizeBytes") long sizeBytes,
            @Param("purpose") String purpose,
            @Param("uploadedBy") UUID uploadedBy
    );
}
