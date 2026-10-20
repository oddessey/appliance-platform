package com.platform.technician;

import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.UUID;

@RestController
public class TechnicianAdminController {

    private final TechnicianAdminService service;
    private final TechnicianEvidenceService evidenceService;

    public TechnicianAdminController(TechnicianAdminService service, TechnicianEvidenceService evidenceService) {
        this.service = service;
        this.evidenceService = evidenceService;
    }

    @GetMapping("/api/v1/admin/technicians")
    public TechnicianAdminPageResponse listTechnicians(
            @RequestParam(required = false) TechnicianApprovalStatus approvalStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.listTechnicians(approvalStatus, page, size);
    }

    @PatchMapping("/api/v1/admin/technicians/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateTechnician(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody TechnicianAdminUpdateRequest request
    ) {
        service.updateTechnician(id, request, accountId(jwt));
    }

    @GetMapping("/api/v1/admin/assignments/{assignmentId}/evidence")
    public List<TechnicianEvidenceResponse> listEvidence(@PathVariable UUID assignmentId) {
        return evidenceService.listForAdmin(assignmentId);
    }

    @GetMapping("/api/v1/admin/reservations/{id}/technician-candidates")
    public List<TechnicianCandidateResponse> findCandidates(@PathVariable UUID id) {
        return service.findCandidates(id);
    }

    @GetMapping("/api/v1/admin/reservations/pending-technician-assignment")
    public PendingTechnicianReservationPageResponse listPendingReservations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.listPendingReservations(page, size);
    }

    @PostMapping("/api/v1/admin/reservations/{id}/assignments")
    public TechnicianAssignmentResponse assign(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody TechnicianAssignmentRequest request
    ) {
        return service.assign(id, request.technicianProfileId(), accountId(jwt));
    }

    @GetMapping("/api/v1/admin/assignments/{assignmentId}/evidence/{evidenceId}")
    public ResponseEntity<Resource> downloadEvidence(
            @PathVariable UUID assignmentId,
            @PathVariable UUID evidenceId
    ) {
        TechnicianEvidenceFile file = evidenceService.downloadForAdmin(assignmentId, evidenceId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.originalFilename()).build().toString())
                .body(new FileSystemResource(file.path()));
    }

    private UUID accountId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
