package com.platform.product;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/product-recognition")
public class ProductRecognitionController {

    private final ProductRecognitionService recognitionService;

    public ProductRecognitionController(ProductRecognitionService recognitionService) {
        this.recognitionService = recognitionService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductRecognitionResponse recognize(@RequestPart("file") MultipartFile file) {
        return recognitionService.recognize(file);
    }
}
