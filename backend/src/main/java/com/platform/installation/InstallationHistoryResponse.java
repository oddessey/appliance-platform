package com.platform.installation;

import java.util.List;

public record InstallationHistoryResponse(
        int page,
        int size,
        long totalElements,
        List<InstallationHistoryItem> content
) {
}
