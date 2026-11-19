package com.platform.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component
public class TossPaymentsClient {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String secretKey;

    public TossPaymentsClient(
            @Value("${toss.payments.base-url:https://api.tosspayments.com}") String baseUrl,
            @Value("${toss.payments.secret-key:}") String secretKey,
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
        this.secretKey = secretKey;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public JsonNode confirm(String paymentKey, String orderId, BigDecimal amount) {
        requireSecret();
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "paymentKey", paymentKey,
                    "orderId", orderId,
                    "amount", amount.longValueExact()));
            return restClient.post()
                    .uri("/v1/payments/confirm")
                    .header("Authorization", "Basic " + basicAuth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (HttpStatusCodeException exception) {
            throw new PaymentProviderException("결제 승인에 실패했습니다.", exception);
        } catch (Exception exception) {
            throw new PaymentProviderException("결제 승인 요청을 생성하지 못했습니다.", exception);
        }
    }

    public JsonNode cancel(String paymentKey, String reason, BigDecimal amount) {
        requireSecret();
        try {
            Map<String, Object> body = amount == null
                    ? Map.of("cancelReason", reason)
                    : Map.of("cancelReason", reason, "cancelAmount", amount.longValueExact());
            return restClient.post()
                    .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
                    .header("Authorization", "Basic " + basicAuth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (HttpStatusCodeException exception) {
            throw new PaymentProviderException("결제 취소에 실패했습니다.", exception);
        }
    }

    public boolean isConfigured() {
        return secretKey != null && !secretKey.isBlank();
    }

    private void requireSecret() {
        if (!isConfigured()) {
            throw new PaymentProviderException("토스페이먼츠 시크릿 키가 설정되지 않았습니다.");
        }
    }

    private String basicAuth() {
        return Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
    }
}
