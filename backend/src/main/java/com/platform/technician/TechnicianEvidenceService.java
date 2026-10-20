package com.platform.technician;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.UUID;

@Service
public class TechnicianEvidenceService {

    private static final long MAX_SIZE_BYTES = 10 * 1024 * 1024;
    private final TechnicianRepository technicianRepository;
    private final TechnicianEvidenceRepository evidenceRepository;
    private final Path storageRoot;

    public TechnicianEvidenceService(
            TechnicianRepository technicianRepository,
            TechnicianEvidenceRepository evidenceRepository,
            @Value("${appliance.evidence.storage-path:uploads/work-evidence}") String storagePath
    ) {
        this.technicianRepository = technicianRepository;
        this.evidenceRepository = evidenceRepository;
        this.storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
    }

    @Transactional
    public TechnicianEvidenceResponse upload(UUID userId, UUID assignmentId, MultipartFile file) {
        UUID profileId = technicianRepository.findActiveApprovedProfileId(userId)
                .orElseThrow(TechnicianProfileUnavailableException::new);
        String status = evidenceRepository.lockAssignmentStatus(assignmentId, profileId)
                .orElseThrow(AssignmentNotFoundException::new);
        if (!status.equals(AssignmentStatus.WORK_STARTED.name())
                && !status.equals(AssignmentStatus.WORK_COMPLETED.name())) {
            throw new InvalidTechnicianEvidenceException();
        }
        if (file == null || file.isEmpty() || file.getSize() > MAX_SIZE_BYTES) {
            throw new InvalidTechnicianEvidenceException();
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new TechnicianEvidenceStorageException(exception);
        }
        if (content.length == 0 || content.length > MAX_SIZE_BYTES) {
            throw new InvalidTechnicianEvidenceException();
        }
        String extension = validatedExtension(file.getContentType(), content);
        String originalFilename = normalizeFilename(file.getOriginalFilename(), extension);
        String objectKey = UUID.randomUUID() + "." + extension;
        Path destination = storageRoot.resolve(objectKey).normalize();
        if (!destination.startsWith(storageRoot)) {
            throw new InvalidTechnicianEvidenceException();
        }
        try {
            Files.createDirectories(storageRoot);
            Files.write(destination, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException exception) {
            throw new TechnicianEvidenceStorageException(exception);
        }
        try {
            TechnicianEvidenceResponse evidence = evidenceRepository.insertEvidence(
                    assignmentId, profileId, userId, objectKey, originalFilename,
                    "image/" + extension, content.length);
            evidenceRepository.recordEvidenceEvent(assignmentId, userId, evidence.id());
            return evidence;
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
    public List<TechnicianEvidenceResponse> list(UUID userId, UUID assignmentId) {
        UUID profileId = technicianRepository.findActiveApprovedProfileId(userId)
                .orElseThrow(TechnicianProfileUnavailableException::new);
        if (!evidenceRepository.assignmentExists(assignmentId, profileId)) {
            throw new AssignmentNotFoundException();
        }
        return evidenceRepository.listEvidence(assignmentId, profileId, userId);
    }

    @Transactional(readOnly = true)
    public TechnicianEvidenceFile download(UUID userId, UUID assignmentId, UUID evidenceId) {
        UUID profileId = technicianRepository.findActiveApprovedProfileId(userId)
                .orElseThrow(TechnicianProfileUnavailableException::new);
        TechnicianEvidenceFile file = evidenceRepository.findEvidenceFile(
                assignmentId, evidenceId, profileId, userId).orElseThrow(AssignmentNotFoundException::new);
        Path path = storageRoot.resolve(file.path()).normalize();
        if (!path.startsWith(storageRoot) || !Files.isRegularFile(path)) {
            throw new TechnicianEvidenceStorageException(new IOException("Evidence object is missing"));
        }
        return new TechnicianEvidenceFile(path, file.originalFilename(), file.contentType());
    }

    @Transactional(readOnly = true)
    public TechnicianEvidenceFile downloadForAdmin(UUID assignmentId, UUID evidenceId) {
        TechnicianEvidenceFile file = evidenceRepository.findEvidenceFileForAdmin(assignmentId, evidenceId)
                .orElseThrow(AssignmentNotFoundException::new);
        Path path = storageRoot.resolve(file.path()).normalize();
        if (!path.startsWith(storageRoot) || !Files.isRegularFile(path)) {
            throw new TechnicianEvidenceStorageException(new IOException("Evidence object is missing"));
        }
        return new TechnicianEvidenceFile(path, file.originalFilename(), file.contentType());
    }

    @Transactional(readOnly = true)
    public List<TechnicianEvidenceResponse> listForAdmin(UUID assignmentId) {
        return evidenceRepository.listEvidenceForAdmin(assignmentId);
    }

    private String validatedExtension(String contentType, byte[] bytes) {
        if (contentType == null) {
            throw new InvalidTechnicianEvidenceException();
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
                && bytes.length >= 12 && new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII)
                .equals("RIFF") && new String(bytes, 8, 4, java.nio.charset.StandardCharsets.US_ASCII)
                .equals("WEBP")) {
            return "webp";
        }
        throw new InvalidTechnicianEvidenceException();
    }

    private String normalizeFilename(String filename, String extension) {
        String normalized = filename == null ? "" : filename.replace('\\', '/');
        String basename = normalized.substring(normalized.lastIndexOf('/') + 1);
        String cleaned = basename.replaceAll("[\\p{Cntrl}]", "").strip();
        if (cleaned.isEmpty()) {
            return "work-evidence." + extension;
        }
        return cleaned.substring(0, Math.min(cleaned.length(), 255));
    }
}
