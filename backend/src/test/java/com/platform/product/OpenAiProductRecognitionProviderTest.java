package com.platform.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAiProductRecognitionProviderTest {

    @Test
    void sendsImageAndQrDataToOpenAiAndParsesProductResult() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Decoded QR content")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("image/png;base64,")))
                .andRespond(withSuccess("""
                        {
                          "choices": [{
                            "message": {
                              "content": "{\\"productName\\":\\"세탁기\\",\\"productType\\":\\"WASHING_MACHINE\\",\\"manufacturer\\":\\"제조사\\",\\"modelNumber\\":\\"WM-1234\\",\\"serialNumber\\":null,\\"capacity\\":\\"20kg\\",\\"confidence\\":0.95,\\"keyFeatures\\":[\\"드럼형\\"]}"
                            }
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));
        OpenAiProductRecognitionProvider provider = new OpenAiProductRecognitionProvider(
                builder.build(), new ObjectMapper(), "test-key", "gpt-4o-mini");

        ProductRecognitionDetails result = provider.recognize(
                "image bytes".getBytes(StandardCharsets.UTF_8), "image/png", "decoded-qr");

        assertEquals("WM-1234", result.modelNumber());
        assertEquals(0.95, result.confidence());
        assertEquals("드럼형", result.keyFeatures().getFirst());
        server.verify();
    }

    @Test
    void failsClearlyWhenOpenAiKeyIsNotConfigured() {
        OpenAiProductRecognitionProvider provider = new OpenAiProductRecognitionProvider(
                RestClient.builder().build(), new ObjectMapper(), " ", "gpt-4o-mini");

        assertThrows(ProductRecognitionUnavailableException.class,
                () -> provider.recognize(new byte[]{1}, "image/png", null));
    }
}
