package com.platform.reservation;

import java.util.List;

public record ReservationPageResponse(
        int page,
        int size,
        long totalElements,
        List<ReservationResponse> items
) {
}
