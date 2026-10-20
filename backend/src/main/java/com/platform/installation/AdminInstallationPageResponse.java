package com.platform.installation;

import java.util.List;

public record AdminInstallationPageResponse(
        int page,
        int size,
        long totalElements,
        List<AdminInstallationSummary> content
) {
}
