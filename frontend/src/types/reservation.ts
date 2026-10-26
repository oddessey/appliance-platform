export type ReservationStatus =
  | "PENDING"
  | "CONFIRMED"
  | "RESCHEDULE_REQUESTED"
  | "CANCELLED"
  | "EXPIRED"
  | "COMPLETED";

export interface Reservation {
  id: string;
  reservationNo: string;
  moveRequestId: string;
  requestNo: string;
  quoteId: string;
  quoteNo: string;
  status: ReservationStatus;
  scheduledStartAt: string;
  scheduledEndAt: string;
  customerMemo: string | null;
  confirmedAt: string | null;
  cancelledAt: string | null;
  createdAt: string;
  customerName: string;
  customerPhone: string;
  originAddress: string;
  destinationAddress: string;
  quoteTotalAmount: number;
  technicianAssignmentStatus: "PENDING" | "ACCEPTED" | "REJECTED" | "ON_THE_WAY" | "WORK_STARTED" | "WORK_COMPLETED" | "CANCELLED" | null;
  technicianAssignmentId: string | null;
  technicianName: string | null;
}

export interface ReservationPage {
  page: number;
  size: number;
  totalElements: number;
  items: Reservation[];
}

export interface ReservationAvailability {
  moveRequestId: string;
  available: boolean;
  scheduledStartAt: string;
  scheduledEndAt: string;
}

export interface ReservationCreateRequest {
  moveRequestId: string;
  quoteId: string;
  scheduledStartAt: string;
  scheduledEndAt: string;
  customerMemo?: string;
}

export interface ReservationRescheduleRequest {
  scheduledStartAt: string;
  scheduledEndAt: string;
}
