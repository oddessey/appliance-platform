package com.platform.product;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductQrDecoderTest {

    @Test
    void readsQrCodeFromPng() throws Exception {
        String payload = "https://example.test/product/model-123";
        var matrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 240, 240);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(MatrixToImageWriter.toBufferedImage(matrix), "png", output);

        assertEquals(Optional.of(payload), new ProductQrDecoder().decode(output.toByteArray()));
    }
}
