package com.platform.installation;

import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MoveRequestImageRepository {

    private final MoveRequestImageMapper imageMapper;

    public MoveRequestImageRepository(MoveRequestImageMapper imageMapper) {
        this.imageMapper = imageMapper;
    }

    public boolean isOwnedBy(UUID moveRequestId, UUID customerId) {
        return Boolean.TRUE.equals(imageMapper.isOwnedBy(moveRequestId, customerId));
    }

    public boolean hasAccess(UUID moveRequestId, UUID userId) {
        return Boolean.TRUE.equals(imageMapper.hasAccess(moveRequestId, userId));
    }

    public boolean exists(UUID moveRequestId) {
        return Boolean.TRUE.equals(imageMapper.exists(moveRequestId));
    }

    public boolean moveItemBelongsToRequest(UUID moveItemId, UUID moveRequestId) {
        return Boolean.TRUE.equals(imageMapper.moveItemBelongsToRequest(moveItemId, moveRequestId));
    }

    public List<MoveRequestImageResponse> findCustomerImages(UUID moveRequestId) {
        return imageMapper.findCustomerImages(moveRequestId);
    }

    public Optional<MoveRequestImageFile> findCustomerImageFile(UUID moveRequestId, UUID imageId) {
        return Optional.ofNullable(imageMapper.findCustomerImageFile(moveRequestId, imageId))
                .map(row -> new MoveRequestImageFile(
                        Path.of(row.objectKey()),
                        row.originalFilename(),
                        row.contentType()));
    }

    public MoveRequestImageResponse insert(
            UUID moveRequestId,
            UUID moveItemId,
            String objectKey,
            String originalFilename,
            String contentType,
            long sizeBytes,
            MoveRequestImagePurpose purpose,
            UUID uploadedBy
    ) {
        MoveRequestImageResponse response = imageMapper.insert(
                UUID.randomUUID(),
                moveRequestId,
                moveItemId,
                objectKey,
                originalFilename,
                contentType,
                sizeBytes,
                purpose.name(),
                uploadedBy);
        if (response == null) {
            throw new MoveRequestImageNotFoundException();
        }
        return response;
    }
}
