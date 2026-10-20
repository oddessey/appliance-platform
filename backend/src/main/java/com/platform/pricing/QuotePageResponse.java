package com.platform.pricing;

import java.util.List;

public record QuotePageResponse(int page, int size, long totalElements, List<QuoteResponse> items) {
}
