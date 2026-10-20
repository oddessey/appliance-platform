package com.platform.pricing;

import java.math.BigDecimal;

public record QuoteLineResponse(
        String lineCode,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineAmount
) {
}
