import { apiRequest } from "./client";
import type {
  Reservation,
  ReservationAvailability,
  ReservationCreateRequest,
  ReservationPage,
  ReservationRescheduleRequest,
  ReservationStatus,
} from "../types/reservation";
import type { TechnicianAssignmentStatus } from "../types/installation";

export function checkReservationAvailability(
  moveRequestId: string,
  scheduledStartAt: string,
  scheduledEndAt: string,
  reservationId?: string,
): Promise<ReservationAvailability> {
  const query = new URLSearchParams({
    moveRequestId,
    startAt: scheduledStartAt,
    endAt: scheduledEndAt,
  });
  if (reservationId) query.set("reservationId", reservationId);
  return apiRequest<ReservationAvailability>(`/reservations/availability?${query.toString()}`);
}

export function createReservation(
  request: ReservationCreateRequest,
  idempotencyKey: string,
): Promise<Reservation> {
  return apiRequest<Reservation>("/reservations", {
    method: "POST",
    headers: { "Idempotency-Key": idempotencyKey },
    body: JSON.stringify(request),
  });
}

export function getReservations(
  moveRequestId?: string,
  page = 0,
  size = 20,
): Promise<ReservationPage> {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  if (moveRequestId) query.set("moveRequestId", moveRequestId);
  return apiRequest<ReservationPage>(`/reservations?${query.toString()}`);
}

export function cancelReservation(reservationId: string): Promise<void> {
  return apiRequest<void>(`/reservations/${encodeURIComponent(reservationId)}/cancel`, {
    method: "POST",
  });
}

export function rescheduleReservation(
  reservationId: string,
  request: ReservationRescheduleRequest,
): Promise<Reservation> {
  return apiRequest<Reservation>(`/reservations/${encodeURIComponent(reservationId)}/reschedule`, {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export function getAdminReservations(
  status?: ReservationStatus,
  page = 0,
  size = 100,
  assignmentStatus?: TechnicianAssignmentStatus,
): Promise<ReservationPage> {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  if (status) query.set("status", status);
  if (assignmentStatus) query.set("assignmentStatus", assignmentStatus);
  return apiRequest<ReservationPage>(`/admin/reservations?${query.toString()}`);
}

export function adminRescheduleReservation(
  reservationId: string,
  request: ReservationRescheduleRequest,
): Promise<Reservation> {
  return apiRequest<Reservation>(
    `/admin/reservations/${encodeURIComponent(reservationId)}/schedule`,
    {
      method: "PATCH",
      body: JSON.stringify(request),
    },
  );
}

export function adminCancelReservation(reservationId: string): Promise<void> {
  return apiRequest<void>(`/admin/reservations/${encodeURIComponent(reservationId)}/cancel`, {
    method: "POST",
  });
}
