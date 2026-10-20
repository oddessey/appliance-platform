package com.platform.installation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/installations")
public class AdminInstallationController {

    private final AdminInstallationService service;

    public AdminInstallationController(AdminInstallationService service) {
        this.service = service;
    }

    @GetMapping
    public AdminInstallationPageResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean reviewRequired
    ) {
        return service.list(page, size, reviewRequired);
    }
}
