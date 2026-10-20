package com.platform.reservation;

import com.platform.reservation.ReservationRepository.ExpiredReservation;
import com.platform.reservation.ReservationRepository.IdempotencyRecord;
import com.platform.reservation.ReservationRepository.ReservationRequestState;
import com.platform.reservation.ReservationRepository.ReservationState;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.platform.technician.AssignmentStatus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class ReservationService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

    private final ReservationRepository repository;

    public ReservationService(ReservationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ReservationAvailabilityResponse checkAvailability(
            UUID requestId,
            UUID customerId,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            UUID reservationId
    ) {
        validateInterval(startAt, endAt);
        ReservationRequestState request = repository.findRequest(requestId)
                .orElseThrow(ReservationNotFoundException::new);
        requireCustomerOwner(request, customerId);

        UUID excludedReservationId = null;
        if (reservationId != null) {
            ReservationState reservation = repository.findReservationState(reservationId, customerId)
                    .orElseThrow(ReservationNotFoundException::new);
            if (!reservation.requestId().equals(requestId)) {
                throw new ReservationNotFoundException();
            }
            if (!"CONFIRMED".equals(reservation.status())) {
                throw new ReservationNotAcceptableException();
            }
            excludedReservationId = reservationId;
        } else if (request.hasActiveReservation()) {
            return new ReservationAvailabilityResponse(requestId, false, startAt, endAt);
        }

        if (!"QUOTED".equals(request.status()) && !"RESERVED".equals(request.status())) {
            throw new ReservationNotAcceptableException();
        }
        boolean available = repository.isAvailable(requestId, startAt, endAt, excludedReservationId);
        return new ReservationAvailabilityResponse(requestId, available, startAt, endAt);
    }

    @Transactional
    public ReservationResponse createReservation(
            CreateReservationRequest request,
            String idempotencyKey,
            UUID customerId
    ) {
        validateIdempotencyKey(idempotencyKey);
        String requestHash = hashRequest(request);
        ReservationResponse replay = findIdempotentReservation(idempotencyKey, requestHash, customerId);
        if (replay != null) {
            return replay;
        }
        validateInterval(request.scheduledStartAt(), request.scheduledEndAt());

        ReservationRequestState requestState = repository.lockRequest(request.moveRequestId())
                .orElseThrow(ReservationNotFoundException::new);
        requireCustomerOwner(requestState, customerId);

        replay = findIdempotentReservation(idempotencyKey, requestHash, customerId);
        if (replay != null) {
            return replay;
        }
        if (requestState.hasActiveReservation()) {
            throw new ActiveReservationExistsException();
        }
        if (!"QUOTED".equals(requestState.status())
                || !repository.isLatestAcceptedQuote(request.moveRequestId(), request.quoteId())) {
            throw new ReservationNotAcceptableException();
        }
        if (!repository.isAvailable(request.moveRequestId(), request.scheduledStartAt(),
                request.scheduledEndAt(), null)) {
            throw new ActiveReservationExistsException();
        }

        UUID reservationId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        String reservationNo = "RSV-" + now.toLocalDate().toString().replace("-", "")
                + "-" + reservationId.toString().replace("-", "").substring(0, 8).toUpperCase();
        try {
            UUID createdId = repository.createReservation(
                            reservationId, reservationNo, request.moveRequestId(), request.quoteId(),
                            request.scheduledStartAt(), request.scheduledEndAt(), request.customerMemo(),
                            idempotencyKey, requestHash)
                    .orElse(null);
            if (createdId == null) {
                ReservationResponse existing = findIdempotentReservation(idempotencyKey, requestHash, customerId);
                if (existing == null) {
                    throw new ReservationConflictException();
                }
                return existing;
            }
            if (!repository.markRequestReserved(request.moveRequestId())) {
                throw new ReservationNotAcceptableException();
            }
        } catch (DataIntegrityViolationException exception) {
            throw new ReservationConflictException();
        }

        repository.recordAudit(customerId, reservationId, "RESERVATION_CONFIRMED", null,
                "CONFIRMED", request.scheduledStartAt(), request.scheduledEndAt());
        return repository.findCustomerReservation(reservationId, customerId)
                .orElseThrow(ReservationNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public ReservationPageResponse listCustomerReservations(
            UUID customerId,
            UUID requestId,
            int page,
            int size
    ) {
        validatePage(page, size);
        if (requestId != null) {
            ReservationRequestState request = repository.findRequest(requestId)
                    .orElseThrow(ReservationNotFoundException::new);
            requireCustomerOwner(request, customerId);
        }
        return repository.findCustomerReservations(customerId, requestId, page, size);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getCustomerReservation(UUID reservationId, UUID customerId) {
        return repository.findCustomerReservation(reservationId, customerId)
                .orElseThrow(ReservationNotFoundException::new);
    }

    @Transactional
    public ReservationResponse rescheduleCustomerReservation(
            UUID reservationId,
            RescheduleReservationRequest request,
            UUID customerId
    ) {
        return reschedule(reservationId, request, customerId, false);
    }

    @Transactional
    public ReservationResponse rescheduleAdminReservation(
            UUID reservationId,
            RescheduleReservationRequest request,
            UUID adminId
    ) {
        return reschedule(reservationId, request, adminId, true);
    }

    @Transactional
    public void cancelCustomerReservation(UUID reservationId, UUID customerId) {
        cancel(reservationId, customerId, false);
    }

    @Transactional
    public void cancelAdminReservation(UUID reservationId, UUID adminId) {
        cancel(reservationId, adminId, true);
    }

    @Transactional(readOnly = true)
    public ReservationPageResponse listAdminReservations(
            ReservationStatus status,
            AssignmentStatus assignmentStatus,
            int page,
            int size
    ) {
        validatePage(page, size);
        return repository.findAdminReservations(
                status == null ? null : status.name(),
                assignmentStatus == null ? null : assignmentStatus.name(),
                page,
                size);
    }

    @Transactional
    public int expirePendingReservations() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusHours(24);
        var expiredReservations = repository.expirePendingReservations(cutoff);
        for (ExpiredReservation expired : expiredReservations) {
            repository.returnRequestToQuoted(expired.requestId());
            repository.recordAudit(null, expired.reservationId(), "RESERVATION_EXPIRED",
                    "PENDING", "EXPIRED", null, null);
        }
        return expiredReservations.size();
    }

    private ReservationResponse reschedule(
            UUID reservationId,
            RescheduleReservationRequest request,
            UUID actorId,
            boolean admin
    ) {
        validateInterval(request.scheduledStartAt(), request.scheduledEndAt());
        ReservationState state = repository.lockReservation(reservationId, admin ? null : actorId)
                .orElseThrow(ReservationNotFoundException::new);
        if (!"CONFIRMED".equals(state.status())) {
            throw new ReservationNotAcceptableException();
        }
        if (repository.hasWorkStartedAssignment(reservationId)) {
            throw new ReservationNotAcceptableException();
        }
        if (!admin && !state.scheduledStartAt().isAfter(OffsetDateTime.now())) {
            throw new ReservationNotAcceptableException();
        }
        if (!repository.isAvailable(state.requestId(), request.scheduledStartAt(),
                request.scheduledEndAt(), reservationId)) {
            throw new ActiveReservationExistsException();
        }
        try {
            if (!repository.updateSchedule(reservationId, request.scheduledStartAt(), request.scheduledEndAt())) {
                throw new ReservationNotAcceptableException();
            }
            repository.cancelOpenAssignments(reservationId, actorId, "예약 일정 변경으로 배정이 종료되었습니다.");
        } catch (DataIntegrityViolationException exception) {
            throw new ReservationConflictException();
        }
        repository.recordScheduleAudit(actorId, reservationId,
                admin ? "ADMIN_RESCHEDULED" : "CUSTOMER_RESCHEDULED",
                state.scheduledStartAt(), state.scheduledEndAt(),
                request.scheduledStartAt(), request.scheduledEndAt());
        return repository.findById(reservationId).orElseThrow(ReservationNotFoundException::new);
    }

    private void cancel(UUID reservationId, UUID actorId, boolean admin) {
        ReservationState state = repository.lockReservation(reservationId, admin ? null : actorId)
                .orElseThrow(ReservationNotFoundException::new);
        if (!"CONFIRMED".equals(state.status())) {
            throw new ReservationNotAcceptableException();
        }
        if (repository.hasWorkStartedAssignment(reservationId)) {
            throw new ReservationNotAcceptableException();
        }
        if (!admin && !state.scheduledStartAt().isAfter(OffsetDateTime.now())) {
            throw new ReservationNotAcceptableException();
        }
        if (!repository.cancelReservation(reservationId)) {
            throw new ReservationNotAcceptableException();
        }
        repository.cancelOpenAssignments(reservationId, actorId, "예약 취소로 배정이 종료되었습니다.");
        repository.returnRequestToQuoted(state.requestId());
        repository.recordAudit(actorId, reservationId, admin ? "ADMIN_CANCELLED" : "CUSTOMER_CANCELLED",
                state.status(), "CANCELLED", state.scheduledStartAt(), null);
    }

    private ReservationResponse findIdempotentReservation(
            String idempotencyKey,
            String requestHash,
            UUID customerId
    ) {
        IdempotencyRecord existing = repository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing == null) {
            return null;
        }
        if (!existing.requestHash().equals(requestHash)) {
            throw new IdempotencyKeyConflictException();
        }
        return repository.findCustomerReservation(existing.reservationId(), customerId)
                .orElseThrow(ReservationNotFoundException::new);
    }

    private String hashRequest(CreateReservationRequest request) {
        String canonicalRequest = String.join("\n",
                request.moveRequestId().toString(),
                request.quoteId().toString(),
                request.scheduledStartAt().toInstant().toString(),
                request.scheduledEndAt().toInstant().toString(),
                request.customerMemo() == null ? "" : request.customerMemo());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonicalRequest.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw new InvalidReservationException();
        }
    }

    private void validateInterval(OffsetDateTime startAt, OffsetDateTime endAt) {
        if (startAt == null || endAt == null || !startAt.isAfter(OffsetDateTime.now())
                || !endAt.isAfter(startAt)
                || !startAt.atZoneSameInstant(BUSINESS_ZONE).toLocalDate()
                        .equals(endAt.atZoneSameInstant(BUSINESS_ZONE).toLocalDate())) {
            throw new InvalidReservationException();
        }
    }

    private void requireCustomerOwner(ReservationRequestState request, UUID customerId) {
        if (!request.customerId().equals(customerId)) {
            throw new ReservationNotFoundException();
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE || (long) page * size > Integer.MAX_VALUE) {
            throw new InvalidReservationPageException();
        }
    }
}
