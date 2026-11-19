package com.platform.payment;

import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public class PaymentRepository {
    private final PaymentMapper mapper;

    public PaymentRepository(PaymentMapper mapper) {
        this.mapper = mapper;
    }

    public PaymentMapper.PaymentState findReservationForPayment(UUID reservationId, UUID customerId) {
        return mapper.findReservationForPayment(reservationId, customerId);
    }

    public PaymentMapper.PaymentState findPaymentByOrderId(String orderId) {
        return mapper.findPaymentByOrderId(orderId);
    }

    public PaymentMapper.PaymentState findPaymentById(UUID paymentId, UUID customerId) {
        return mapper.findPaymentById(paymentId, customerId);
    }

    public void insertPayment(UUID id, UUID reservationId, String orderId,
                              BigDecimal amount, String idempotencyKey) {
        mapper.insertPayment(id, reservationId, orderId, amount, idempotencyKey);
    }

    public void setPaymentInProgress(UUID id, String paymentKey) {
        mapper.setPaymentInProgress(id, paymentKey);
    }

    public void markPaymentPaid(UUID id, String paymentKey, String method, OffsetDateTime approvedAt) {
        mapper.markPaymentPaid(id, paymentKey, method, approvedAt);
    }

    public void markPaymentFailed(UUID id) {
        mapper.markPaymentFailed(id, "PAYMENT_CONFIRM_FAILED");
    }

    public void confirmReservation(UUID reservationId) {
        mapper.confirmReservation(reservationId);
    }

    public void markRequestReserved(UUID requestId) {
        mapper.markRequestReserved(requestId);
    }

    public void markCancelRequested(UUID id, BigDecimal cancelledAmount) {
        mapper.markCancelRequested(id, cancelledAmount);
    }

    public void markCancelled(UUID id) {
        mapper.markCancelled(id);
    }

    public void recordPaymentEvent(UUID paymentId, String providerEventId, String eventType,
                                   String payloadSummary) {
        mapper.recordPaymentEvent(UUID.randomUUID(), paymentId, providerEventId, eventType, payloadSummary);
    }
}
