package com.platform.reservation;

import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ReservationRepository {

    private final ReservationMapper mapper;

    public ReservationRepository(ReservationMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<ReservationRequestState> lockRequest(UUID requestId) {
        return Optional.ofNullable(mapper.lockRequest(requestId));
    }

    public Optional<ReservationRequestState> findRequest(UUID requestId) {
        return Optional.ofNullable(mapper.findRequest(requestId));
    }

    public boolean isLatestAcceptedQuote(UUID requestId, UUID quoteId) {
        return Boolean.TRUE.equals(mapper.isLatestAcceptedQuote(requestId, quoteId));
    }

    public boolean isAvailable(UUID requestId, OffsetDateTime startAt, OffsetDateTime endAt, UUID excludeId) {
        return Boolean.TRUE.equals(mapper.isAvailable(requestId, startAt, endAt, excludeId));
    }

    public Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey) {
        return Optional.ofNullable(mapper.findByIdempotencyKey(idempotencyKey));
    }

    public Optional<UUID> createReservation(
            UUID id,
            String reservationNo,
            UUID requestId,
            UUID quoteId,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            String customerMemo,
            String idempotencyKey,
            String requestHash
    ) {
        ReservationMapper.UuidValue created = mapper.createReservation(
                id, reservationNo, requestId, quoteId, startAt, endAt, customerMemo, idempotencyKey, requestHash);
        return created == null ? Optional.empty() : Optional.of(created.value());
    }

    public Optional<ReservationResponse> findById(UUID reservationId) {
        return Optional.ofNullable(mapper.findById(reservationId));
    }

    public Optional<ReservationResponse> findCustomerReservation(UUID reservationId, UUID customerId) {
        return Optional.ofNullable(mapper.findCustomerReservation(reservationId, customerId));
    }

    public ReservationPageResponse findCustomerReservations(
            UUID customerId,
            UUID requestId,
            int page,
            int size
    ) {
        long total = mapper.countCustomerReservations(customerId, requestId);
        List<ReservationResponse> reservations = mapper.findCustomerReservations(
                customerId, requestId, size, page * size);
        return new ReservationPageResponse(page, size, total, reservations);
    }

    public ReservationPageResponse findAdminReservations(
            String status,
            String assignmentStatus,
            int page,
            int size
    ) {
        long total = mapper.countAdminReservations(status, assignmentStatus);
        List<ReservationResponse> reservations = mapper.findAdminReservations(
                status, assignmentStatus, size, page * size);
        return new ReservationPageResponse(page, size, total, reservations);
    }

    public Optional<ReservationState> lockReservation(UUID reservationId, UUID customerId) {
        return Optional.ofNullable(mapper.lockReservation(reservationId, customerId));
    }

    public Optional<ReservationState> findReservationState(UUID reservationId, UUID customerId) {
        return Optional.ofNullable(mapper.findReservationState(reservationId, customerId));
    }

    public boolean updateSchedule(UUID reservationId, OffsetDateTime startAt, OffsetDateTime endAt) {
        return mapper.updateSchedule(reservationId, startAt, endAt) == 1;
    }

    public boolean hasWorkStartedAssignment(UUID reservationId) {
        return Boolean.TRUE.equals(mapper.hasWorkStartedAssignment(reservationId));
    }

    public boolean hasAcceptedAssignment(UUID reservationId) {
        return Boolean.TRUE.equals(mapper.hasAcceptedAssignment(reservationId));
    }

    public void cancelOpenAssignments(UUID reservationId, UUID actorId, String reason) {
        for (ReservationMapper.UuidValue assignment : mapper.cancelOpenAssignments(reservationId)) {
            mapper.insertCancellationWorkEvent(UUID.randomUUID(), assignment.value(), reason, actorId);
        }
    }

    public boolean markRequestReserved(UUID requestId) {
        return mapper.markRequestReserved(requestId) == 1;
    }

    public boolean cancelReservation(UUID reservationId) {
        return mapper.cancelReservation(reservationId) == 1;
    }

    public void returnRequestToQuoted(UUID requestId) {
        mapper.returnRequestToQuoted(requestId);
    }

    public void recordAudit(
            UUID actorId,
            UUID reservationId,
            String action,
            String beforeStatus,
            String afterStatus,
            OffsetDateTime startAt,
            OffsetDateTime endAt
    ) {
        mapper.recordAudit(UUID.randomUUID(), actorId, action, reservationId, beforeStatus, afterStatus,
                startAt == null ? null : startAt.toString(), endAt == null ? null : endAt.toString());
    }

    public void recordScheduleAudit(
            UUID actorId,
            UUID reservationId,
            String action,
            OffsetDateTime beforeStartAt,
            OffsetDateTime beforeEndAt,
            OffsetDateTime afterStartAt,
            OffsetDateTime afterEndAt
    ) {
        mapper.recordScheduleAudit(UUID.randomUUID(), actorId, action, reservationId,
                beforeStartAt.toString(), beforeEndAt.toString(),
                afterStartAt.toString(), afterEndAt.toString());
    }

    public List<ExpiredReservation> expirePendingReservations(OffsetDateTime cutoff) {
        return mapper.expirePendingReservations(cutoff);
    }

    public record ReservationRequestState(UUID customerId, String status, boolean hasActiveReservation) {
    }

    public record ReservationState(
            UUID requestId,
            String status,
            OffsetDateTime scheduledStartAt,
            OffsetDateTime scheduledEndAt,
            UUID customerId
    ) {
    }

    public record IdempotencyRecord(UUID reservationId, String requestHash) {
    }

    public record ExpiredReservation(UUID reservationId, UUID requestId) {
    }
}
