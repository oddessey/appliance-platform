package com.platform.product;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductRecognitionServiceTest {

    @Mock
    private ProductRecognitionProvider provider;

    @Mock
    private ProductQrDecoder qrDecoder;

    @Test
    void decodesQrAndReturnsRecognizedProductDetails() {
        byte[] png = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1, 2, 3, 4};
        ProductRecognitionDetails details = new ProductRecognitionDetails(
                "세탁기", "WASHING_MACHINE", "제조사", "WM-1234", null, "20kg", 0.9, List.of("드럼형"));
        when(qrDecoder.decode(png)).thenReturn(Optional.of("https://example.test/WM-1234"));
        when(provider.recognize(eq(png), eq("image/png"), eq("https://example.test/WM-1234")))
                .thenReturn(details);
        ProductRecognitionService service = new ProductRecognitionService(provider, qrDecoder);

        ProductRecognitionResponse response = service.recognize(
                new MockMultipartFile("file", "label.png", "image/png", png));

        assertEquals(details.productName(), response.productName());
        assertEquals(details.modelNumber(), response.modelNumber());
        assertEquals("https://example.test/WM-1234", response.qrCodeValue());
    }

    @Test
    void rejectsUnsupportedOrSpoofedImageBeforeCallingAi() {
        ProductRecognitionService service = new ProductRecognitionService(provider, qrDecoder);
        MockMultipartFile fakePng = new MockMultipartFile("file", "fake.png", "image/png", new byte[]{1, 2, 3});

        assertThrows(InvalidProductImageException.class, () -> service.recognize(fakePng));
        verify(qrDecoder, never()).decode(org.mockito.ArgumentMatchers.any());
        verify(provider, never()).recognize(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsFilesOverTenMegabytes() {
        ProductRecognitionService service = new ProductRecognitionService(provider, qrDecoder);
        MockMultipartFile oversized = new MockMultipartFile(
                "file", "large.png", "image/png", new byte[10 * 1024 * 1024 + 1]);

        assertThrows(InvalidProductImageException.class, () -> service.recognize(oversized));
        verify(provider, never()).recognize(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }
}
