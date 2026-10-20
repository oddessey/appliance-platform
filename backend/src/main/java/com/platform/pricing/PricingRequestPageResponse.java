package com.platform.pricing;

import java.util.List;

public record PricingRequestPageResponse(
        int page,
        int size,
        long totalElements,
        List<PricingRequestSummary> items
) {
}
