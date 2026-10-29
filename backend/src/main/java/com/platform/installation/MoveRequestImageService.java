package com.platform.installation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.UUID;

@Service
public class MoveRequestImageService {

    private static final long MAX_SIZE_BYTES = 10 * 1024 * 1024;

    private final MoveRequestImageRepository imageRepository;
    private final Path storageRoot;

    public MoveRequestImageService(
            MoveRequestImageRepository imageRepository,
            @Value("${appliance.request-image.storage-path:uploads/request-images}") String storagePath
    ) {
        this.imageRepository = imageRepository;
        this.storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
    }

    @Transactional
    public MoveRequestImageResponse upload(
            UUID userId,
            boolean admin,
            UUID moveRequestId,
            String purposeValue,
            String moveItemIdValue,
            MultipartFile file
    ) {
        MoveRequestImagePurpose purpose = MoveRequestImagePurpose.from(purposeValue);
        UUID moveItemId = parseMoveItemId(moveItemIdValue);

        if (!admin && !imageRepository.isOwnedBy(moveRequestId, userId)) {
            throw new MoveRequestImageNotFoundException();
        }
        if (admin && !imageRepository.exists(moveRequestId)) {
            throw new MoveRequestImageNotFoundException();
        }
        if (moveItemId != null && !imageRepository.moveItemBelongsToRequest(moveItemId, moveRequestId)) {
            throw new InvalidMoveRequestImageException();
        }

        if (file == null || file.isEmpty() || file.getSize() > MAX_SIZE_BYTES) {
            throw new InvalidMoveRequestImageException();
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new MoveRequestImageStorageException(exception);
        }
        if (content.length == 0 || content.length > MAX_SIZE_BYTES) {
            throw new InvalidMoveRequestImageException();
        }

        String extension = validatedExtension(file.getContentType(), content);
        String originalFilename = normalizeFilename(file.getOriginalFilename(), extension);
        String objectKey = UUID.randomUUID() + "." + extension;
        Path destination = storageRoot.resolve(objectKey).normalize();
        if (!destination.startsWith(storageRoot)) {
            throw new InvalidMoveRequestImageException();
        }

        try {
            Files.createDirectories(storageRoot);
            Files.write(destination, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException exception) {
            throw new MoveRequestImageStorageException(exception);
        }

        try {
            return imageRepository.insert(
                    moveRequestId,
                    moveItemId,
                    objectKey,
                    originalFilename,
                    "image/" + extension,
                    content.length,
                    purpose,
                    userId
            );
        } catch (RuntimeException exception) {
            try {
                Files.deleteIfExists(destination);
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<MoveRequestImageResponse> list(UUID userId, boolean admin, UUID moveRequestId) {
        verifyReadAccess(userId, admin, moveRequestId);
        return imageRepository.findCustomerImages(moveRequestId);
    }

    @Transactional(readOnly = true)
    public MoveRequestImageFile download(UUID userId, boolean admin, UUID moveRequestId, UUID imageId) {
        verifyReadAccess(userId, admin, moveRequestId);
        MoveRequestImageFile file = imageRepository.findCustomerImageFile(moveRequestId, imageId)
                .orElseThrow(MoveRequestImageAssetNotFoundException::new);
        Path path = storageRoot.resolve(file.path()).normalize();
        if (!path.startsWith(storageRoot) || !Files.isRegularFile(path)) {
            throw new MoveRequestImageStorageException(new IOException("Request image object is missing"));
        }
        return new MoveRequestImageFile(path, file.originalFilename(), file.contentType());
    }

    private void verifyReadAccess(UUID userId, boolean admin, UUID moveRequestId) {
        if (admin) {
            if (!imageRepository.exists(moveRequestId)) {
                throw new MoveRequestImageNotFoundException();
            }
            return;
        }
        if (!imageRepository.hasAccess(moveRequestId, userId)) {
            throw new MoveRequestImageNotFoundException();
        }
    }

    private UUID parseMoveItemId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.strip());
        } catch (IllegalArgumentException exception) {
            throw new InvalidMoveRequestImageException();
        }
    }

    private String validatedExtension(String contentType, byte[] bytes) {
        if (contentType == null) {
            throw new InvalidMoveRequestImageException();
        }
        if (contentType.equalsIgnoreCase("image/jpeg")
                && bytes.length >= 3 && (bytes[0] & 0xff) == 0xff
                && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) {
            return "jpeg";
        }
        if (contentType.equalsIgnoreCase("image/png")
                && bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50
                && bytes[2] == 0x4e && bytes[3] == 0x47
                && bytes[4] == 0x0d && bytes[5] == 0x0a
                && bytes[6] == 0x1a && bytes[7] == 0x0a) {
            return "png";
        }
        if (contentType.equalsIgnoreCase("image/webp")
                && bytes.length >= 12 && new String(bytes, 0, 4, StandardCharsets.US_ASCII)
                .equals("RIFF") && new String(bytes, 8, 4, StandardCharsets.US_ASCII)
                .equals("WEBP")) {
            return "webp";
        }
        throw new InvalidMoveRequestImageException();
    }

    private String normalizeFilename(String filename, String extension) {
        String normalized = filename == null ? "" : filename.replace('\\', '/');
        String basename = normalized.substring(normalized.lastIndexOf('/') + 1);
        String cleaned = basename.replaceAll("[\\p{Cntrl}]", "").strip();
        if (cleaned.isEmpty()) {
            return "request-image." + extension;
        }
        return cleaned.substring(0, Math.min(cleaned.length(), 255));
    }
}
