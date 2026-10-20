package com.platform.technician;

import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
public class TechnicianController {

    private final TechnicianService technicianService;
    private final TechnicianProfileService technicianProfileService;
    private final TechnicianEvidenceService evidenceService;

    public TechnicianController(
            TechnicianService technicianService,
            TechnicianProfileService technicianProfileService,
            TechnicianEvidenceService evidenceService
    ) {
        this.technicianService = technicianService;
        this.technicianProfileService = technicianProfileService;
        this.evidenceService = evidenceService;
    }

    @GetMapping("/api/v1/technicians/me/profile")
    public TechnicianProfileResponse getProfile(@AuthenticationPrincipal Jwt jwt) {
        return technicianProfileService.getProfile(accountId(jwt));
    }

    @org.springframework.web.bind.annotation.PutMapping("/api/v1/technicians/me/profile")
    public TechnicianProfileResponse saveProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TechnicianProfileUpdateRequest request
    ) {
        return technicianProfileService.saveProfile(accountId(jwt), request);
    }

    @GetMapping("/api/v1/technicians/me/assignments")
    public TechnicianAssignmentPageResponse listAssignments(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) AssignmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return technicianService.listAssignments(accountId(jwt), status, page, size);
    }

    @PostMapping("/api/v1/assignments/{id}/accept")
    public TechnicianAssignmentResponse accept(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        return technicianService.accept(accountId(jwt), id);
    }

    @PostMapping("/api/v1/assignments/{id}/reject")
    public TechnicianAssignmentResponse reject(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody RejectAssignmentRequest request
    ) {
        return technicianService.reject(accountId(jwt), id, request.reason());
    }

    @PostMapping("/api/v1/assignments/{id}/events")
    public TechnicianAssignmentResponse addEvent(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody AssignmentEventRequest request
    ) {
        return technicianService.addEvent(accountId(jwt), id, request);
    }

    @PostMapping(value = "/api/v1/assignments/{id}/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TechnicianEvidenceResponse uploadEvidence(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestPart("file") MultipartFile file
    ) {
        return evidenceService.upload(accountId(jwt), id, file);
    }

    @GetMapping("/api/v1/assignments/{id}/evidence")
    public List<TechnicianEvidenceResponse> listEvidence(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        return evidenceService.list(accountId(jwt), id);
    }

    @GetMapping("/api/v1/assignments/{id}/evidence/{evidenceId}")
    public ResponseEntity<Resource> downloadEvidence(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @PathVariable UUID evidenceId
    ) {
        TechnicianEvidenceFile file = evidenceService.download(accountId(jwt), id, evidenceId);
        return evidenceResponse(file);
    }

    private ResponseEntity<Resource> evidenceResponse(TechnicianEvidenceFile file) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.originalFilename()).build().toString())
                .body(new org.springframework.core.io.FileSystemResource(file.path()));
    }

    private UUID accountId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
