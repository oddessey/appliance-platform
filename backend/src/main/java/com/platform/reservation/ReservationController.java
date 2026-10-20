package com.platform.reservation;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/reservations/availability")
    public ReservationAvailabilityResponse checkAvailability(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID moveRequestId,
            @RequestParam OffsetDateTime startAt,
            @RequestParam OffsetDateTime endAt,
            @RequestParam(required = false) UUID reservationId
    ) {
        return reservationService.checkAvailability(moveRequestId, accountId(jwt),
                startAt, endAt, reservationId);
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse createReservation(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateReservationRequest request
    ) {
        return reservationService.createReservation(request, idempotencyKey, accountId(jwt));
    }

    @GetMapping("/reservations")
    public ReservationPageResponse listReservations(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) UUID moveRequestId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reservationService.listCustomerReservations(accountId(jwt), moveRequestId, page, size);
    }

    @GetMapping("/reservations/{id}")
    public ReservationResponse getReservation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        return reservationService.getCustomerReservation(id, accountId(jwt));
    }

    @PostMapping("/reservations/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelReservation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        reservationService.cancelCustomerReservation(id, accountId(jwt));
    }

    @PostMapping("/reservations/{id}/reschedule")
    public ReservationResponse rescheduleReservation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody RescheduleReservationRequest request
    ) {
        return reservationService.rescheduleCustomerReservation(id, request, accountId(jwt));
    }

    private UUID accountId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
