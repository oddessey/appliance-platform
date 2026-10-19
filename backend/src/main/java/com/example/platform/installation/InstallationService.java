package com.example.platform.installation;

import com.example.platform.appliance.ApplianceType;
import com.example.platform.appliance.ApplianceTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class InstallationService {

    private final InstallationRepository installationRepository;
    private final ApplianceTypeRepository applianceTypeRepository;

    public InstallationService(
            InstallationRepository installationRepository,
            ApplianceTypeRepository applianceTypeRepository
    ) {
        this.installationRepository = installationRepository;
        this.applianceTypeRepository = applianceTypeRepository;
    }

    @Transactional
    public InstallationResponse create(UUID customerId, CreateInstallationRequest request) {
        ApplianceType applianceType = applianceTypeRepository.findActiveByCode(request.applianceTypeCode())
                .orElseThrow(UnknownApplianceTypeException::new);

        UUID id = UUID.randomUUID();
        String requestNo = "INS-" + id.toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        return installationRepository.create(
                id,
                requestNo,
                customerId,
                request.originAddress().strip(),
                request.destinationAddress().strip(),
                request.desiredStartAt(),
                applianceType.id()
        );
    }
}
