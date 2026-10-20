package com.platform.reservation;

import com.platform.technician.AssignmentStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/reservations")
public class ReservationAdminController {

    private final ReservationService reservationService;

    public ReservationAdminController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    public ReservationPageResponse listReservations(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) AssignmentStatus assignmentStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reservationService.listAdminReservations(status, assignmentStatus, page, size);
    }

    @PatchMapping("/{id}/schedule")
    public ReservationResponse rescheduleReservation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody RescheduleReservationRequest request
    ) {
        return reservationService.rescheduleAdminReservation(id, request, UUID.fromString(jwt.getSubject()));
    }

    @PostMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelReservation(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        reservationService.cancelAdminReservation(id, UUID.fromString(jwt.getSubject()));
    }
}
