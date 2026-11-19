package com.platform.payment;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Mapper
public interface PaymentMapper {
    PaymentState findReservationForPayment(
            @Param("reservationId") UUID reservationId,
            @Param("customerId") UUID customerId);

    PaymentState findPaymentById(
            @Param("paymentId") UUID paymentId,
            @Param("customerId") UUID customerId);

    PaymentState findPaymentByOrderId(@Param("orderId") String orderId);

    int insertPayment(
            @Param("id") UUID id,
            @Param("reservationId") UUID reservationId,
            @Param("orderId") String orderId,
            @Param("amount") BigDecimal amount,
            @Param("idempotencyKey") String idempotencyKey);

    int setPaymentInProgress(
            @Param("id") UUID id,
            @Param("paymentKey") String paymentKey);

    int markPaymentPaid(
            @Param("id") UUID id,
            @Param("paymentKey") String paymentKey,
            @Param("method") String method,
            @Param("approvedAt") OffsetDateTime approvedAt);

    int markPaymentFailed(
            @Param("id") UUID id,
            @Param("reason") String reason);

    int confirmReservation(@Param("reservationId") UUID reservationId);

    int markRequestReserved(@Param("requestId") UUID requestId);

    int markCancelRequested(
            @Param("id") UUID id,
            @Param("cancelledAmount") BigDecimal cancelledAmount);

    int markCancelled(@Param("id") UUID id);

    int recordPaymentEvent(
            @Param("id") UUID id,
            @Param("paymentId") UUID paymentId,
            @Param("providerEventId") String providerEventId,
            @Param("eventType") String eventType,
            @Param("payloadSummary") String payloadSummary);

    PaymentResponse findPaymentResponse(@Param("paymentId") UUID paymentId);

    List<PaymentResponse> findAdminPayments(
            @Param("status") String status,
            @Param("limit") int limit,
            @Param("offset") int offset);

    record PaymentState(
            UUID id,
            UUID reservationId,
            UUID requestId,
            UUID customerId,
            String reservationStatus,
            String paymentStatus,
            BigDecimal amount,
            BigDecimal quoteAmount,
            String orderId,
            String paymentKey,
            String customerName,
            String requestNo
    ) {}
}
