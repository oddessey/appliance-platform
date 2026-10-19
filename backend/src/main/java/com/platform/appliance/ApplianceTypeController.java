package com.platform.appliance;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/appliance-types")
public class ApplianceTypeController {

    private final ApplianceTypeRepository applianceTypeRepository;

    public ApplianceTypeController(ApplianceTypeRepository applianceTypeRepository) {
        this.applianceTypeRepository = applianceTypeRepository;
    }

    @GetMapping
    public List<ApplianceTypeResponse> list() {
        return applianceTypeRepository.findAllActive();
    }
}
