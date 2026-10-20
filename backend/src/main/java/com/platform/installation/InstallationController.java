package com.platform.installation;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/installations")
public class InstallationController {

    private final InstallationService installationService;

    public InstallationController(InstallationService installationService) {
        this.installationService = installationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InstallationResponse create(
            @Valid @RequestBody CreateInstallationRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return installationService.create(UUID.fromString(jwt.getSubject()), request);
    }

    @GetMapping
    public InstallationHistoryResponse list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return installationService.listByCustomer(UUID.fromString(jwt.getSubject()), page, size);
    }
}
