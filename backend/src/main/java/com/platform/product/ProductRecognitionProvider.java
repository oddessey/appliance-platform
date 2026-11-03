package com.platform.product;

public interface ProductRecognitionProvider {

    ProductRecognitionDetails recognize(byte[] image, String contentType, String qrCodeValue);
}
