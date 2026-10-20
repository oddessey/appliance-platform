package com.platform.technician;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TechnicianEvidenceServiceTest {

    @Mock
    private TechnicianRepository technicianRepository;
    @Mock
    private TechnicianEvidenceRepository evidenceRepository;

    @Test
    void storesImageAndLinksItsMetadataToTheAssignment(@TempDir Path storageRoot) throws Exception {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();
        byte[] png = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1, 2, 3, 4};
        when(technicianRepository.findActiveApprovedProfileId(userId)).thenReturn(Optional.of(profileId));
        when(evidenceRepository.lockAssignmentStatus(assignmentId, profileId))
                .thenReturn(Optional.of("WORK_STARTED"));
        when(evidenceRepository.insertEvidence(
                org.mockito.ArgumentMatchers.eq(assignmentId),
                org.mockito.ArgumentMatchers.eq(profileId),
                org.mockito.ArgumentMatchers.eq(userId),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq("proof.png"),
                org.mockito.ArgumentMatchers.eq("image/png"),
                org.mockito.ArgumentMatchers.eq((long) png.length)))
                .thenReturn(new TechnicianEvidenceResponse(
                        imageId, "proof.png", "image/png", png.length, OffsetDateTime.now()));
        TechnicianEvidenceService service = new TechnicianEvidenceService(
                technicianRepository, evidenceRepository, storageRoot.toString());

        service.upload(userId, assignmentId, new MockMultipartFile("file", "proof.png", "image/png", png));

        ArgumentCaptor<String> objectKey = ArgumentCaptor.forClass(String.class);
        verify(evidenceRepository).insertEvidence(
                org.mockito.ArgumentMatchers.eq(assignmentId),
                org.mockito.ArgumentMatchers.eq(profileId),
                org.mockito.ArgumentMatchers.eq(userId),
                objectKey.capture(),
                org.mockito.ArgumentMatchers.eq("proof.png"),
                org.mockito.ArgumentMatchers.eq("image/png"),
                org.mockito.ArgumentMatchers.eq((long) png.length));
        assertArrayEquals(png, Files.readAllBytes(storageRoot.resolve(objectKey.getValue())));
        verify(evidenceRepository).recordEvidenceEvent(assignmentId, userId, imageId);
    }

    @Test
    void rejectsAnImageWhoseContentDoesNotMatchItsMimeType(@TempDir Path storageRoot) {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        when(technicianRepository.findActiveApprovedProfileId(userId)).thenReturn(Optional.of(profileId));
        when(evidenceRepository.lockAssignmentStatus(assignmentId, profileId))
                .thenReturn(Optional.of("WORK_STARTED"));
        TechnicianEvidenceService service = new TechnicianEvidenceService(
                technicianRepository, evidenceRepository, storageRoot.toString());

        assertThrows(InvalidTechnicianEvidenceException.class, () -> service.upload(
                userId, assignmentId, new MockMultipartFile("file", "bad.png", "image/png", new byte[]{1, 2, 3})));
    }
}
