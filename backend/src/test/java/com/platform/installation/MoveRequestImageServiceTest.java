package com.platform.installation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoveRequestImageServiceTest {

    @Mock
    private MoveRequestImageRepository imageRepository;

    @Test
    void storesImageAndMetadataForAnOwnedRequest(@TempDir Path storageRoot) throws Exception {
        UUID userId = UUID.randomUUID();
        UUID moveRequestId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();
        byte[] png = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1, 2, 3, 4};
        when(imageRepository.isOwnedBy(moveRequestId, userId)).thenReturn(true);
        when(imageRepository.insert(
                eq(moveRequestId),
                isNull(),
                anyString(),
                eq("washing-machine.png"),
                eq("image/png"),
                eq((long) png.length),
                eq(MoveRequestImagePurpose.APPLIANCE),
                eq(userId)))
                .thenReturn(new MoveRequestImageResponse(
                        imageId, moveRequestId, null, "APPLIANCE", "washing-machine.png",
                        "image/png", png.length, OffsetDateTime.now()));
        MoveRequestImageService service = new MoveRequestImageService(imageRepository, storageRoot.toString());

        service.upload(userId, false, moveRequestId, "APPLIANCE", null,
                new MockMultipartFile("file", "washing-machine.png", "image/png", png));

        ArgumentCaptor<String> objectKey = ArgumentCaptor.forClass(String.class);
        verify(imageRepository).insert(
                eq(moveRequestId),
                isNull(),
                objectKey.capture(),
                eq("washing-machine.png"),
                eq("image/png"),
                eq((long) png.length),
                eq(MoveRequestImagePurpose.APPLIANCE),
                eq(userId));
        assertArrayEquals(png, Files.readAllBytes(storageRoot.resolve(objectKey.getValue())));
    }

    @Test
    void allowsAnAdminToUploadToAnyRequest(@TempDir Path storageRoot) {
        UUID adminId = UUID.randomUUID();
        UUID moveRequestId = UUID.randomUUID();
        byte[] png = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1, 2, 3, 4};
        when(imageRepository.exists(moveRequestId)).thenReturn(true);
        when(imageRepository.insert(
                eq(moveRequestId), isNull(), anyString(), anyString(), eq("image/png"),
                anyLong(), any(MoveRequestImagePurpose.class), eq(adminId)))
                .thenReturn(new MoveRequestImageResponse(
                        UUID.randomUUID(), moveRequestId, null, "INSTALLATION_ENV", "env.png",
                        "image/png", png.length, OffsetDateTime.now()));
        MoveRequestImageService service = new MoveRequestImageService(imageRepository, storageRoot.toString());

        service.upload(adminId, true, moveRequestId, "INSTALLATION_ENV", null,
                new MockMultipartFile("file", "env.png", "image/png", png));

        verify(imageRepository).exists(moveRequestId);
        verify(imageRepository, never()).isOwnedBy(moveRequestId, adminId);
    }

    @Test
    void rejectsARequestThatDoesNotBelongToTheCustomer(@TempDir Path storageRoot) {
        UUID userId = UUID.randomUUID();
        UUID moveRequestId = UUID.randomUUID();
        byte[] png = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1, 2, 3, 4};
        when(imageRepository.isOwnedBy(moveRequestId, userId)).thenReturn(false);
        MoveRequestImageService service = new MoveRequestImageService(imageRepository, storageRoot.toString());

        assertThrows(MoveRequestImageNotFoundException.class, () -> service.upload(
                userId, false, moveRequestId, "APPLIANCE", null,
                new MockMultipartFile("file", "photo.png", "image/png", png)));
        verify(imageRepository, never()).insert(
                any(), any(), anyString(), anyString(), anyString(), anyLong(),
                any(MoveRequestImagePurpose.class), any());
    }

    @Test
    void rejectsContentThatDoesNotMatchTheDeclaredMimeType(@TempDir Path storageRoot) {
        UUID userId = UUID.randomUUID();
        UUID moveRequestId = UUID.randomUUID();
        when(imageRepository.isOwnedBy(moveRequestId, userId)).thenReturn(true);
        MoveRequestImageService service = new MoveRequestImageService(imageRepository, storageRoot.toString());

        assertThrows(InvalidMoveRequestImageException.class, () -> service.upload(
                userId, false, moveRequestId, "APPLIANCE", null,
                new MockMultipartFile("file", "fake.png", "image/png", new byte[]{1, 2, 3})));
        verify(imageRepository, never()).insert(
                any(), any(), anyString(), anyString(), anyString(), anyLong(),
                any(MoveRequestImagePurpose.class), any());
    }

    @Test
    void rejectsAnUnsupportedPurpose(@TempDir Path storageRoot) {
        MoveRequestImageService service = new MoveRequestImageService(imageRepository, storageRoot.toString());

        assertThrows(InvalidMoveRequestImageException.class, () -> service.upload(
                UUID.randomUUID(), false, UUID.randomUUID(), "WORK_PROOF", null,
                new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1, 2, 3})));
    }

    @Test
    void listsCustomerImagesForAnAssignedTechnician(@TempDir Path storageRoot) {
        UUID technicianUserId = UUID.randomUUID();
        UUID moveRequestId = UUID.randomUUID();
        List<MoveRequestImageResponse> images = List.of(new MoveRequestImageResponse(
                UUID.randomUUID(), moveRequestId, null, "APPLIANCE", "washing-machine.png",
                "image/png", 12, OffsetDateTime.now()));
        when(imageRepository.hasAccess(moveRequestId, technicianUserId)).thenReturn(true);
        when(imageRepository.findCustomerImages(moveRequestId)).thenReturn(images);
        MoveRequestImageService service = new MoveRequestImageService(imageRepository, storageRoot.toString());

        assertEquals(images, service.list(technicianUserId, false, moveRequestId));
        verify(imageRepository, never()).exists(moveRequestId);
    }

    @Test
    void allowsAnAdminToListAnyExistingRequest(@TempDir Path storageRoot) {
        UUID adminId = UUID.randomUUID();
        UUID moveRequestId = UUID.randomUUID();
        when(imageRepository.exists(moveRequestId)).thenReturn(true);
        when(imageRepository.findCustomerImages(moveRequestId)).thenReturn(List.of());
        MoveRequestImageService service = new MoveRequestImageService(imageRepository, storageRoot.toString());

        assertEquals(List.of(), service.list(adminId, true, moveRequestId));
        verify(imageRepository, never()).hasAccess(moveRequestId, adminId);
    }

    @Test
    void hidesImagesFromAUserWhoIsNotTheOwnerOrAssignedTechnician(@TempDir Path storageRoot) {
        UUID userId = UUID.randomUUID();
        UUID moveRequestId = UUID.randomUUID();
        when(imageRepository.hasAccess(moveRequestId, userId)).thenReturn(false);
        MoveRequestImageService service = new MoveRequestImageService(imageRepository, storageRoot.toString());

        assertThrows(MoveRequestImageNotFoundException.class,
                () -> service.list(userId, false, moveRequestId));
        verify(imageRepository, never()).findCustomerImages(moveRequestId);
    }

    @Test
    void downloadsAnAccessibleCustomerImage(@TempDir Path storageRoot) throws Exception {
        UUID userId = UUID.randomUUID();
        UUID moveRequestId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();
        byte[] png = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1, 2, 3, 4};
        Files.write(storageRoot.resolve("stored.png"), png);
        when(imageRepository.hasAccess(moveRequestId, userId)).thenReturn(true);
        when(imageRepository.findCustomerImageFile(moveRequestId, imageId)).thenReturn(Optional.of(
                new MoveRequestImageFile(Path.of("stored.png"), "washing-machine.png", "image/png")));
        MoveRequestImageService service = new MoveRequestImageService(imageRepository, storageRoot.toString());

        MoveRequestImageFile file = service.download(userId, false, moveRequestId, imageId);

        assertEquals("washing-machine.png", file.originalFilename());
        assertEquals("image/png", file.contentType());
        assertArrayEquals(png, Files.readAllBytes(file.path()));
    }

    @Test
    void rejectsADownloadWhenTheImageMetadataIsMissing(@TempDir Path storageRoot) {
        UUID userId = UUID.randomUUID();
        UUID moveRequestId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();
        when(imageRepository.hasAccess(moveRequestId, userId)).thenReturn(true);
        when(imageRepository.findCustomerImageFile(moveRequestId, imageId)).thenReturn(Optional.empty());
        MoveRequestImageService service = new MoveRequestImageService(imageRepository, storageRoot.toString());

        assertThrows(MoveRequestImageAssetNotFoundException.class,
                () -> service.download(userId, false, moveRequestId, imageId));
    }
}
