package com.platform.technician;

import java.util.List;

public record TechnicianAdminPageResponse(int page, int size, long totalElements, List<TechnicianAdminSummary> content) {
}
