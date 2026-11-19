export interface PaymentPrepareResponse {
  paymentId: string;
  reservationId: string;
  orderId: string;
  orderName: string;
  amount: number;
  clientKey: string;
  customerName: string;
  successUrl: string;
  failUrl: string;
}

export interface ConfirmPaymentRequest {
  paymentKey: string;
  orderId: string;
  amount: number;
}

export interface PaymentResponse {
  id: string;
  reservationId: string;
  provider: string;
  orderId: string;
  paymentKey: string | null;
  amount: number;
  status: string;
  method: string | null;
  approvedAt: string | null;
  paidAt: string | null;
  createdAt: string;
}
