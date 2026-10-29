package com.platform.installation;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/move-requests")
public class MoveRequestImageController {

    private final MoveRequestImageService imageService;

    public MoveRequestImageController(MoveRequestImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MoveRequestImageResponse upload(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "purpose", required = false) String purpose,
            @RequestParam(value = "moveItemId", required = false) String moveItemId
    ) {
        return imageService.upload(
                UUID.fromString(jwt.getSubject()),
                isAdmin(jwt),
                id,
                purpose,
                moveItemId,
                file
        );
    }

    @GetMapping("/{id}/images")
    public List<MoveRequestImageResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        return imageService.list(UUID.fromString(jwt.getSubject()), isAdmin(jwt), id);
    }

    @GetMapping("/{id}/images/{imageId}")
    public ResponseEntity<Resource> download(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @PathVariable UUID imageId
    ) {
        MoveRequestImageFile file = imageService.download(
                UUID.fromString(jwt.getSubject()), isAdmin(jwt), id, imageId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(file.originalFilename(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300")
                .body(new FileSystemResource(file.path()));
    }

    private boolean isAdmin(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains("ADMIN");
    }
}
