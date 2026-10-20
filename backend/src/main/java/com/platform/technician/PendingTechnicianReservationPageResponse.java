package com.platform.technician;

import java.util.List;

public record PendingTechnicianReservationPageResponse(
        int page,
        int size,
        long totalElements,
        List<PendingTechnicianReservationResponse> content
) {
}
