import { apiRequest } from "./client";
import type {
  ConfirmPaymentRequest,
  PaymentPrepareResponse,
  PaymentResponse,
} from "../types/payment";

export function prepareReservationPayment(
  reservationId: string,
  idempotencyKey: string,
): Promise<PaymentPrepareResponse> {
  return apiRequest<PaymentPrepareResponse>(`/reservations/${encodeURIComponent(reservationId)}/payments`, {
    method: "POST",
    headers: { "Idempotency-Key": idempotencyKey },
  });
}

export function confirmPayment(request: ConfirmPaymentRequest): Promise<PaymentResponse> {
  return apiRequest<PaymentResponse>("/payments/confirm", {
    method: "POST",
    body: JSON.stringify(request),
  });
}
