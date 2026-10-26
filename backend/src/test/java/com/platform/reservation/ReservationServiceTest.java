package com.platform.reservation;

import com.platform.reservation.ReservationRepository.ExpiredReservation;
import com.platform.reservation.ReservationRepository.IdempotencyRecord;
import com.platform.reservation.ReservationRepository.ReservationRequestState;
import com.platform.reservation.ReservationRepository.ReservationState;
import com.platform.technician.AssignmentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    private static final String IDEMPOTENCY_KEY = "reservation-test-key";

    @Mock
    private ReservationRepository repository;
    @InjectMocks
    private ReservationService service;

    @Test
    void createsConfirmedReservationFromLatestAcceptedQuote() {
        UUID customerId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID quoteId = UUID.randomUUID();
        CreateReservationRequest request = request(requestId, quoteId, futureStart(), null);
        when(repository.lockRequest(requestId)).thenReturn(Optional.of(
                new ReservationRequestState(customerId, "QUOTED", false)));
        when(repository.isLatestAcceptedQuote(requestId, quoteId)).thenReturn(true);
        when(repository.isAvailable(eq(requestId), any(), any(), isNull())).thenReturn(true);
        when(repository.createReservation(any(), any(), eq(requestId), eq(quoteId),
                any(), any(), eq(null), eq(IDEMPOTENCY_KEY), any()))
                .thenAnswer(invocation -> Optional.of(invocation.getArgument(0)));
        when(repository.markRequestReserved(requestId)).thenReturn(true);
        when(repository.findCustomerReservation(any(UUID.class), eq(customerId)))
                .thenAnswer(invocation -> Optional.of(response(
                        invocation.getArgument(0), requestId, quoteId, "CONFIRMED")));

        ReservationResponse result = service.createReservation(request, IDEMPOTENCY_KEY, customerId);

        assertEquals("CONFIRMED", result.status());
        verify(repository).createReservation(eq(result.id()), any(), eq(requestId), eq(quoteId),
                eq(request.scheduledStartAt()), eq(request.scheduledEndAt()), eq(null),
                eq(IDEMPOTENCY_KEY), any());
        verify(repository).markRequestReserved(requestId);
        verify(repository).recordAudit(eq(customerId), eq(result.id()), eq("RESERVATION_CONFIRMED"),
                eq(null), eq("CONFIRMED"), any(), any());
    }

    @Test
    void rejectsCreationWhenRequestHasAnActiveReservation() {
        UUID customerId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        CreateReservationRequest request = request(requestId, UUID.randomUUID(), futureStart(), null);
        when(repository.lockRequest(requestId)).thenReturn(Optional.of(
                new ReservationRequestState(customerId, "RESERVED", true)));

        assertThrows(ActiveReservationExistsException.class,
                () -> service.createReservation(request, IDEMPOTENCY_KEY, customerId));
        verify(repository, never()).createReservation(any(), any(), any(), any(), any(), any(),
                any(), any(), any());
    }

    @Test
    void replaysMatchingIdempotencyKeyWithoutCreatingADuplicate() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID quoteId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        CreateReservationRequest request = request(requestId, quoteId, futureStart(), "side door");
        when(repository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.of(new IdempotencyRecord(reservationId, hash(request))));
        when(repository.findCustomerReservation(reservationId, customerId))
                .thenReturn(Optional.of(response(reservationId, requestId, quoteId, "CONFIRMED")));

        ReservationResponse result = service.createReservation(request, IDEMPOTENCY_KEY, customerId);

        assertEquals(reservationId, result.id());
        verify(repository, never()).lockRequest(any());
        verify(repository, never()).createReservation(any(), any(), any(), any(), any(), any(),
                any(), any(), any());
    }

    @Test
    void rejectsReusingIdempotencyKeyForDifferentPayload() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        CreateReservationRequest request = request(requestId, UUID.randomUUID(), futureStart(), "memo");
        when(repository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.of(new IdempotencyRecord(UUID.randomUUID(), hash(
                        request(requestId, UUID.randomUUID(), request.scheduledStartAt(), "different")))));

        assertThrows(IdempotencyKeyConflictException.class,
                () -> service.createReservation(request, IDEMPOTENCY_KEY, customerId));
    }

    @Test
    void reschedulesOwnConfirmedReservationWhenNewIntervalIsAvailable() {
        UUID customerId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID quoteId = UUID.randomUUID();
        OffsetDateTime oldStartAt = futureStart();
        OffsetDateTime startAt = futureStart().plusDays(1);
        OffsetDateTime endAt = startAt.plusHours(2);
        when(repository.lockReservation(reservationId, customerId)).thenReturn(Optional.of(
                new ReservationState(requestId, "CONFIRMED", oldStartAt, oldStartAt.plusHours(2), customerId)));
        when(repository.isAvailable(requestId, startAt, endAt, reservationId)).thenReturn(true);
        when(repository.updateSchedule(reservationId, startAt, endAt)).thenReturn(true);
        when(repository.findById(reservationId))
                .thenReturn(Optional.of(response(reservationId, requestId, quoteId, "CONFIRMED")));

        ReservationResponse result = service.rescheduleCustomerReservation(
                reservationId, new RescheduleReservationRequest(startAt, endAt), customerId);

        assertEquals(reservationId, result.id());
        verify(repository).recordScheduleAudit(customerId, reservationId, "CUSTOMER_RESCHEDULED",
                oldStartAt, oldStartAt.plusHours(2), startAt, endAt);
        verify(repository).cancelOpenAssignments(
                reservationId, customerId, "예약 일정 변경으로 배정이 종료되었습니다.");
    }

    @Test
    void doesNotCancelReservationAfterItsStartForCustomer() {
        UUID customerId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        OffsetDateTime startAt = OffsetDateTime.now().minusMinutes(1);
        when(repository.lockReservation(reservationId, customerId)).thenReturn(Optional.of(
                new ReservationState(requestId, "CONFIRMED", startAt, startAt.plusHours(2), customerId)));

        assertThrows(ReservationNotAcceptableException.class,
                () -> service.cancelCustomerReservation(reservationId, customerId));
        verify(repository, never()).cancelReservation(any());
    }

    @Test
    void cancelsReservationAndOpenTechnicianAssignmentsTogether() {
        UUID customerId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        OffsetDateTime startAt = futureStart();
        when(repository.lockReservation(reservationId, customerId)).thenReturn(Optional.of(
                new ReservationState(requestId, "CONFIRMED", startAt, startAt.plusHours(2), customerId)));
        when(repository.cancelReservation(reservationId)).thenReturn(true);

        service.cancelCustomerReservation(reservationId, customerId);

        verify(repository).cancelOpenAssignments(
                reservationId, customerId, "예약 취소로 배정이 종료되었습니다.");
        verify(repository).returnRequestToQuoted(requestId);
        verify(repository).recordAudit(customerId, reservationId, "CUSTOMER_CANCELLED",
                "CONFIRMED", "CANCELLED", startAt, null);
    }

    @Test
    void expiresOldPendingReservationsAndReturnsRequestsToQuoted() {
        UUID reservationId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        when(repository.expirePendingReservations(any()))
                .thenReturn(List.of(new ExpiredReservation(reservationId, requestId)));

        assertEquals(1, service.expirePendingReservations());

        verify(repository).returnRequestToQuoted(requestId);
        verify(repository).recordAudit(null, reservationId, "RESERVATION_EXPIRED",
                "PENDING", "EXPIRED", null, null);
    }

    @Test
    void filtersAdminReservationsByTechnicianAssignmentStatus() {
        service.listAdminReservations(null, AssignmentStatus.ACCEPTED, 0, 20);

        verify(repository).findAdminReservations(null, "ACCEPTED", 0, 20);
    }

    private CreateReservationRequest request(
            UUID requestId,
            UUID quoteId,
            OffsetDateTime startAt,
            String memo
    ) {
        return new CreateReservationRequest(requestId, quoteId, startAt, startAt.plusHours(2), memo);
    }

    private OffsetDateTime futureStart() {
        return OffsetDateTime.now(ZoneOffset.ofHours(9)).plusDays(2).withHour(9).withMinute(0)
                .withSecond(0).withNano(0);
    }

    private ReservationResponse response(UUID id, UUID requestId, UUID quoteId, String status) {
        OffsetDateTime startAt = futureStart();
        return new ReservationResponse(id, "RSV-20261020-ABCDEF01", requestId, "REQ-TEST",
                quoteId, "QT-TEST", status, startAt, startAt.plusHours(2), null, startAt,
                null, startAt, "Customer", "010-0000-0000", "Origin", "Destination",
                new BigDecimal("110000.00"), null, null, null);
    }

    private String hash(CreateReservationRequest request) throws Exception {
        String value = String.join("\n",
                request.moveRequestId().toString(),
                request.quoteId().toString(),
                request.scheduledStartAt().toInstant().toString(),
                request.scheduledEndAt().toInstant().toString(),
                request.customerMemo() == null ? "" : request.customerMemo());
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
