package com.platform.payment;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class PaymentService {
    private final PaymentRepository repository;
    private final TossPaymentsClient tossPaymentsClient;
    private final String clientKey;
    private final String successUrl;
    private final String failUrl;

    public PaymentService(
            PaymentRepository repository,
            TossPaymentsClient tossPaymentsClient,
            @Value("${toss.payments.client-key:}") String clientKey,
            @Value("${app.base-url:http://localhost:5173}") String baseUrl
    ) {
        this.repository = repository;
        this.tossPaymentsClient = tossPaymentsClient;
        this.clientKey = clientKey;
        this.successUrl = baseUrl + "/customer/payment/success";
        this.failUrl = baseUrl + "/customer/payment/fail";
    }

    @Transactional
    public PaymentPrepareResponse prepare(UUID reservationId, UUID customerId, String idempotencyKey) {
        PaymentMapper.PaymentState state = repository.findReservationForPayment(reservationId, customerId);
        if (state == null || !"PENDING".equals(state.reservationStatus())) {
            throw new PaymentException("결제할 수 있는 결제 대기 예약이 없습니다.");
        }
        if (state.id() != null) {
            throw new PaymentException("이미 결제 정보가 생성된 예약입니다.");
        }
        if (!tossPaymentsClient.isConfigured() || clientKey.isBlank()) {
            throw new PaymentProviderException("토스페이먼츠 연동 설정이 완료되지 않았습니다.");
        }
        UUID paymentId = UUID.randomUUID();
        String orderId = "APP-" + state.requestNo().replace("-", "")
                + "-" + reservationId.toString().replace("-", "").substring(0, 8).toUpperCase();
        repository.insertPayment(paymentId, reservationId, orderId, state.quoteAmount(),
                idempotencyKey == null || idempotencyKey.isBlank()
                        ? "payment-" + paymentId : idempotencyKey);
        return new PaymentPrepareResponse(paymentId, reservationId, orderId,
                "가전 이전 설치 서비스", state.quoteAmount(), clientKey,
                state.customerName(), successUrl, failUrl);
    }

    @Transactional
    public PaymentResponse confirm(ConfirmPaymentRequest request, UUID customerId) {
        PaymentMapper.PaymentState state = repository.findPaymentByOrderId(request.orderId());
        if (state == null || !state.customerId().equals(customerId)
                || state.reservationStatus() == null
                || !"PENDING".equals(state.reservationStatus())) {
            throw new PaymentException("결제 대상 예약을 찾을 수 없거나 이미 처리되었습니다.");
        }
        if (state.amount().compareTo(request.amount()) != 0
                || state.quoteAmount().compareTo(request.amount()) != 0) {
            throw new PaymentException("결제 금액이 예약 견적 금액과 일치하지 않습니다.");
        }
        repository.setPaymentInProgress(state.id(), request.paymentKey());

        JsonNode response;
        try {
            response = tossPaymentsClient.confirm(request.paymentKey(), request.orderId(), request.amount());
        } catch (RuntimeException exception) {
            repository.markPaymentFailed(state.id());
            throw exception;
        }
        if (!request.orderId().equals(response.path("orderId").asText())
                || response.path("totalAmount").decimalValue().compareTo(state.quoteAmount()) != 0
                || !"DONE".equals(response.path("status").asText())) {
            repository.markPaymentFailed(state.id());
            throw new PaymentException("토스페이먼츠 승인 응답이 유효하지 않습니다.");
        }

        OffsetDateTime approvedAt = response.path("approvedAt").isMissingNode()
                ? OffsetDateTime.now() : OffsetDateTime.parse(response.path("approvedAt").asText());
        repository.markPaymentPaid(state.id(), request.paymentKey(),
                response.path("method").asText(null), approvedAt);
        repository.confirmReservation(state.reservationId());
        repository.markRequestReserved(state.requestId());
        repository.recordPaymentEvent(state.id(), response.path("paymentKey").asText(),
                "PAYMENT_DONE", "{\"source\":\"confirm-api\"}");
        return new PaymentResponse(state.id(), state.reservationId(), "TOSS_PAYMENTS",
                state.orderId(), request.paymentKey(), state.amount(), "PAID",
                response.path("method").asText(null), approvedAt, approvedAt, OffsetDateTime.now());
    }

    @Transactional(readOnly = true)
    public PaymentResponse get(UUID paymentId, UUID actorId, boolean admin) {
        PaymentMapper.PaymentState state = repository.findPaymentById(paymentId, admin ? null : actorId);
        if (state == null) {
            throw new PaymentException("결제 내역을 찾을 수 없습니다.");
        }
        return new PaymentResponse(state.id(), state.reservationId(), "TOSS_PAYMENTS",
                state.orderId(), state.paymentKey(), state.amount(), state.paymentStatus(),
                null, null, null, null);
    }

    @Transactional
    public PaymentResponse cancel(UUID paymentId, CancelPaymentRequest request, UUID actorId,
                                  boolean admin) {
        PaymentMapper.PaymentState state = repository.findPaymentById(paymentId, admin ? null : actorId);
        if (state == null) {
            throw new PaymentException("결제 내역을 찾을 수 없습니다.");
        }
        if (!"PAID".equals(state.paymentStatus())) {
            throw new PaymentException("결제 완료 상태에서만 취소할 수 있습니다.");
        }
        BigDecimal cancelAmount = request.cancelAmount() == null ? state.amount() : request.cancelAmount();
        if (cancelAmount.compareTo(BigDecimal.ZERO) <= 0
                || cancelAmount.compareTo(state.amount()) > 0) {
            throw new PaymentException("취소 금액이 결제 금액을 초과할 수 없습니다.");
        }
        JsonNode response = tossPaymentsClient.cancel(state.paymentKey(),
                request.cancelReason(), cancelAmount);
        if (!"CANCELED".equals(response.path("status").asText())
                && !"PARTIAL_CANCELED".equals(response.path("status").asText())) {
            throw new PaymentException("토스페이먼츠 취소 응답이 유효하지 않습니다.");
        }
        repository.markCancelRequested(state.id(), cancelAmount);
        if (cancelAmount.compareTo(state.amount()) == 0) {
            repository.markCancelled(state.id());
        }
        repository.recordPaymentEvent(state.id(), response.path("paymentKey").asText(),
                response.path("status").asText(), "{\"source\":\"cancel-api\"}");
        return new PaymentResponse(state.id(), state.reservationId(), "TOSS_PAYMENTS",
                state.orderId(), state.paymentKey(), state.amount(),
                cancelAmount.compareTo(state.amount()) == 0 ? "CANCELLED" : "PARTIALLY_REFUNDED",
                null, null, OffsetDateTime.now(), OffsetDateTime.now());
    }
}
