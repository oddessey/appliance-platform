package com.platform.technician;

import java.util.List;

public record TechnicianAssignmentPageResponse(
        int page,
        int size,
        long totalElements,
        List<TechnicianAssignmentResponse> content
) {
}
