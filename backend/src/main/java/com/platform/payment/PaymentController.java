package com.platform.payment;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/reservations/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentPrepareResponse prepare(
            @PathVariable UUID id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return paymentService.prepare(id, UUID.fromString(jwt.getSubject()), idempotencyKey);
    }

    @PostMapping("/payments/confirm")
    public PaymentResponse confirm(
            @Valid @RequestBody ConfirmPaymentRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return paymentService.confirm(request, UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/payments/{id}")
    public PaymentResponse get(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        boolean admin = jwt.getClaimAsStringList("roles").contains("ADMIN");
        return paymentService.get(id, UUID.fromString(jwt.getSubject()), admin);
    }

    @PostMapping("/payments/{id}/cancel")
    public PaymentResponse cancel(
            @PathVariable UUID id,
            @Valid @RequestBody CancelPaymentRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return paymentService.cancel(id, request, UUID.fromString(jwt.getSubject()), false);
    }

    @PostMapping("/admin/payments/{id}/cancel")
    public PaymentResponse adminCancel(
            @PathVariable UUID id,
            @Valid @RequestBody CancelPaymentRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return paymentService.cancel(id, request, UUID.fromString(jwt.getSubject()), true);
    }
}
