package com.platform.product;

import java.util.List;

public record ProductRecognitionResponse(
        String productName,
        String productType,
        String manufacturer,
        String modelNumber,
        String serialNumber,
        String capacity,
        Double confidence,
        List<String> keyFeatures,
        String qrCodeValue
) {
}
