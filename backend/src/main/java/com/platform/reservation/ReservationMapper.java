package com.platform.reservation;

import com.platform.reservation.ReservationRepository.ExpiredReservation;
import com.platform.reservation.ReservationRepository.IdempotencyRecord;
import com.platform.reservation.ReservationRepository.ReservationRequestState;
import com.platform.reservation.ReservationRepository.ReservationState;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Mapper
public interface ReservationMapper {

    ReservationRequestState lockRequest(@Param("requestId") UUID requestId);

    ReservationRequestState findRequest(@Param("requestId") UUID requestId);

    Boolean isLatestAcceptedQuote(@Param("requestId") UUID requestId, @Param("quoteId") UUID quoteId);

    Boolean isAvailable(
            @Param("requestId") UUID requestId,
            @Param("startAt") OffsetDateTime startAt,
            @Param("endAt") OffsetDateTime endAt,
            @Param("excludeId") UUID excludeId);

    IdempotencyRecord findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    UuidValue createReservation(
            @Param("id") UUID id,
            @Param("reservationNo") String reservationNo,
            @Param("requestId") UUID requestId,
            @Param("quoteId") UUID quoteId,
            @Param("startAt") OffsetDateTime startAt,
            @Param("endAt") OffsetDateTime endAt,
            @Param("customerMemo") String customerMemo,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("requestHash") String requestHash);

    ReservationResponse findById(@Param("reservationId") UUID reservationId);

    ReservationResponse findCustomerReservation(
            @Param("reservationId") UUID reservationId,
            @Param("customerId") UUID customerId);

    long countCustomerReservations(
            @Param("customerId") UUID customerId,
            @Param("requestId") UUID requestId);

    List<ReservationResponse> findCustomerReservations(
            @Param("customerId") UUID customerId,
            @Param("requestId") UUID requestId,
            @Param("limit") int limit,
            @Param("offset") int offset);

    long countAdminReservations(
            @Param("status") String status,
            @Param("assignmentStatus") String assignmentStatus);

    List<ReservationResponse> findAdminReservations(
            @Param("status") String status,
            @Param("assignmentStatus") String assignmentStatus,
            @Param("limit") int limit,
            @Param("offset") int offset);

    ReservationState lockReservation(
            @Param("reservationId") UUID reservationId,
            @Param("customerId") UUID customerId);

    ReservationState findReservationState(
            @Param("reservationId") UUID reservationId,
            @Param("customerId") UUID customerId);

    int updateSchedule(
            @Param("reservationId") UUID reservationId,
            @Param("startAt") OffsetDateTime startAt,
            @Param("endAt") OffsetDateTime endAt);

    Boolean hasWorkStartedAssignment(@Param("reservationId") UUID reservationId);

    Boolean hasAcceptedAssignment(@Param("reservationId") UUID reservationId);

    List<UuidValue> cancelOpenAssignments(@Param("reservationId") UUID reservationId);

    int insertCancellationWorkEvent(
            @Param("id") UUID id,
            @Param("assignmentId") UUID assignmentId,
            @Param("reason") String reason,
            @Param("actorId") UUID actorId);

    int markRequestReserved(@Param("requestId") UUID requestId);

    int confirmReservation(@Param("reservationId") UUID reservationId);

    int cancelReservation(@Param("reservationId") UUID reservationId);

    int returnRequestToQuoted(@Param("requestId") UUID requestId);

    int recordAudit(
            @Param("id") UUID id,
            @Param("actorId") UUID actorId,
            @Param("action") String action,
            @Param("reservationId") UUID reservationId,
            @Param("beforeStatus") String beforeStatus,
            @Param("afterStatus") String afterStatus,
            @Param("startAt") String startAt,
            @Param("endAt") String endAt);

    int recordScheduleAudit(
            @Param("id") UUID id,
            @Param("actorId") UUID actorId,
            @Param("action") String action,
            @Param("reservationId") UUID reservationId,
            @Param("beforeStartAt") String beforeStartAt,
            @Param("beforeEndAt") String beforeEndAt,
            @Param("afterStartAt") String afterStartAt,
            @Param("afterEndAt") String afterEndAt);

    List<ExpiredReservation> expirePendingReservations(@Param("cutoff") OffsetDateTime cutoff);

    record UuidValue(UUID value) {
    }
}
