package com.platform.pricing;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record QuoteResponse(
        UUID id,
        String quoteNo,
        int versionNo,
        String status,
        String currency,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        OffsetDateTime issuedAt,
        OffsetDateTime expiresAt,
        List<QuoteLineResponse> lines
) {
}
