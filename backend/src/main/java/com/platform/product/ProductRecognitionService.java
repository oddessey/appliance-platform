package com.platform.product;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@Service
public class ProductRecognitionService {

    private static final long MAX_SIZE_BYTES = 10 * 1024 * 1024;

    private final ProductRecognitionProvider provider;
    private final ProductQrDecoder qrDecoder;

    public ProductRecognitionService(ProductRecognitionProvider provider, ProductQrDecoder qrDecoder) {
        this.provider = provider;
        this.qrDecoder = qrDecoder;
    }

    public ProductRecognitionResponse recognize(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_SIZE_BYTES) {
            throw new InvalidProductImageException();
        }

        byte[] image;
        try {
            image = file.getBytes();
        } catch (IOException exception) {
            throw new ProductRecognitionImageReadException(exception);
        }
        String contentType = validateImage(file.getContentType(), image);
        Optional<String> qrCodeValue = qrDecoder.decode(image);
        ProductRecognitionDetails details = provider.recognize(image, contentType, qrCodeValue.orElse(null));

        return new ProductRecognitionResponse(
                details.productName(),
                details.productType(),
                details.manufacturer(),
                details.modelNumber(),
                details.serialNumber(),
                details.capacity(),
                details.confidence(),
                details.keyFeatures(),
                qrCodeValue.orElse(null)
        );
    }

    private String validateImage(String contentType, byte[] image) {
        if (contentType == null) {
            throw new InvalidProductImageException();
        }
        if (contentType.equalsIgnoreCase("image/jpeg")
                && image.length >= 3 && (image[0] & 0xff) == 0xff
                && (image[1] & 0xff) == 0xd8 && (image[2] & 0xff) == 0xff) {
            return "image/jpeg";
        }
        if (contentType.equalsIgnoreCase("image/png")
                && image.length >= 8 && (image[0] & 0xff) == 0x89 && image[1] == 0x50
                && image[2] == 0x4e && image[3] == 0x47
                && image[4] == 0x0d && image[5] == 0x0a
                && image[6] == 0x1a && image[7] == 0x0a) {
            return "image/png";
        }
        if (contentType.equalsIgnoreCase("image/webp")
                && image.length >= 12 && new String(image, 0, 4, java.nio.charset.StandardCharsets.US_ASCII)
                .equals("RIFF") && new String(image, 8, 4, java.nio.charset.StandardCharsets.US_ASCII)
                .equals("WEBP")) {
            return "image/webp";
        }
        throw new InvalidProductImageException();
    }
}
