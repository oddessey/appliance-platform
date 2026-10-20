package com.platform.installation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminInstallationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final InstallationRepository installationRepository;

    public AdminInstallationService(InstallationRepository installationRepository) {
        this.installationRepository = installationRepository;
    }

    @Transactional(readOnly = true)
    public AdminInstallationPageResponse list(int page, int size, boolean reviewRequired) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE || (long) page * size > Integer.MAX_VALUE) {
            throw new InvalidInstallationPageException();
        }

        int offset = page * size;
        List<AdminInstallationSummary> installations = installationRepository.findForAdmin(
                size, offset, reviewRequired);
        long totalElements = installationRepository.countForAdmin(reviewRequired);
        return new AdminInstallationPageResponse(page, size, totalElements, installations);
    }
}
