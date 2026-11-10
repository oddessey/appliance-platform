package com.platform.appliance;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ApplianceTypeRepository {

    private final ApplianceTypeMapper applianceTypeMapper;

    public ApplianceTypeRepository(ApplianceTypeMapper applianceTypeMapper) {
        this.applianceTypeMapper = applianceTypeMapper;
    }

    public Optional<ApplianceType> findActiveByCode(String code) {
        return applianceTypeMapper.findActiveByCode(code).stream().findFirst();
    }

    public List<ApplianceTypeResponse> findAllActive() {
        return applianceTypeMapper.findAllActive();
    }
}
