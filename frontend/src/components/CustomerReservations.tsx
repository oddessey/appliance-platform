import { useCallback, useEffect, useState } from "react";
import type { FormEvent } from "react";
import {
  cancelReservation,
  checkReservationAvailability,
  createReservation,
  getReservations,
} from "../api/reservations";
import { ApiError } from "../api/client";
import type { Reservation } from "../types/reservation";

interface CustomerReservationsProps {
  requestId: string;
  quoteId: string;
}

interface ScheduleForm {
  startAt: string;
  endAt: string;
  memo: string;
}

export function CustomerReservations({ requestId, quoteId }: CustomerReservationsProps) {
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [form, setForm] = useState<ScheduleForm>(() => initialSchedule());
  const [idempotencyKey, setIdempotencyKey] = useState(() => createIdempotencyKey());
  const [availability, setAvailability] = useState<boolean | null>(null);
  const [loading, setLoading] = useState(true);
  const [reservationsLoaded, setReservationsLoaded] = useState(false);
  const [checking, setChecking] = useState(false);
  const [saving, setSaving] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const loadReservations = useCallback(async (showLoading = true) => {
    if (showLoading) {
      setLoading(true);
      setReservationsLoaded(false);
      setError(null);
    }
    try {
      const page = await getReservations(requestId);
      setReservations(page.items);
      setReservationsLoaded(true);
      setError(null);
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      if (showLoading) setLoading(false);
    }
  }, [requestId]);

  useEffect(() => {
    void loadReservations();
  }, [loadReservations]);

  const activeReservation = reservations.find(
    (reservation) => ["CONFIRMED", "PENDING", "RESCHEDULE_REQUESTED"].includes(reservation.status),
  );
  const canCancel = activeReservation ? canCustomerCancel(activeReservation) : false;
  const technicianAccepted = activeReservation?.technicianAssignmentStatus
    ? ACCEPTED_ASSIGNMENT_STATUSES.includes(activeReservation.technicianAssignmentStatus)
    : false;

  useEffect(() => {
    if (!activeReservation) return;
    function refresh() {
      if (document.visibilityState === "visible") void loadReservations(false);
    }
    const intervalId = window.setInterval(refresh, 15_000);
    window.addEventListener("focus", refresh);
    document.addEventListener("visibilitychange", refresh);
    return () => {
      window.clearInterval(intervalId);
      window.removeEventListener("focus", refresh);
      document.removeEventListener("visibilitychange", refresh);
    };
  }, [activeReservation?.id, loadReservations]);

  function changeField<K extends keyof ScheduleForm>(key: K, value: ScheduleForm[K]) {
    setForm((current) => ({ ...current, [key]: value }));
    setAvailability(null);
    setError(null);
    setMessage(null);
    setIdempotencyKey(createIdempotencyKey());
  }

  function scheduleInterval(): { scheduledStartAt: string; scheduledEndAt: string } | null {
    const start = new Date(form.startAt);
    const end = new Date(form.endAt);
    if (!form.startAt || !form.endAt || Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) {
      setError("예약 시작과 종료 시각을 모두 입력해 주세요.");
      return null;
    }
    return { scheduledStartAt: start.toISOString(), scheduledEndAt: end.toISOString() };
  }

  async function checkAvailability() {
    const interval = scheduleInterval();
    if (!interval) return;
    setChecking(true);
    setError(null);
    setMessage(null);
    try {
      const result = await checkReservationAvailability(
        requestId,
        interval.scheduledStartAt,
        interval.scheduledEndAt,
      );
      setAvailability(result.available);
      setMessage(result.available
        ? "선택한 일정으로 예약할 수 있습니다."
        : "이 설치 요청에는 이미 활성 예약이 있습니다.");
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      setChecking(false);
    }
  }

  async function submitReservation(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const interval = scheduleInterval();
    if (!interval) return;
    if (availability !== true) {
      setError("먼저 예약 가능 여부를 확인해 주세요.");
      return;
    }
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      await createReservation({
        moveRequestId: requestId,
        quoteId,
        ...interval,
        customerMemo: form.memo.trim() || undefined,
      }, idempotencyKey);
      setMessage("예약이 확정되었습니다.");
      setIdempotencyKey(createIdempotencyKey());
      await loadReservations();
      setAvailability(null);
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      setSaving(false);
    }
  }

  async function cancel() {
    if (!activeReservation || !canCancel || !window.confirm("이 예약을 취소할까요?")) return;
    setCancelling(true);
    setError(null);
    setMessage(null);
    try {
      await cancelReservation(activeReservation.id);
      setMessage("예약을 취소했습니다. 수락한 견적으로 다시 예약할 수 있습니다.");
      setAvailability(null);
      setIdempotencyKey(createIdempotencyKey());
      await loadReservations();
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      setCancelling(false);
    }
  }

  return (
    <section aria-label="설치 예약" className="customer-reservations">
      <h4>예약 일정</h4>
      {loading ? (
        <p aria-live="polite" className="history-loading">예약 내역을 불러오는 중...</p>
      ) : error && !reservationsLoaded ? (
        <div className="load-error" role="alert">
          <p className="inline-error">{error}</p>
          <button className="text-button" onClick={() => void loadReservations()} type="button">다시 시도</button>
        </div>
      ) : (
        <>
          {reservations.map((reservation) => (
            <article className="reservation-summary" key={reservation.id}>
              <div>
                <strong>{reservation.reservationNo} · {reservationStatusLabel(reservation.status)}</strong>
                <span>{formatDateTime(reservation.scheduledStartAt)} ~ {formatDateTime(reservation.scheduledEndAt)}</span>
              </div>
              <strong>{formatCurrency(reservation.quoteTotalAmount)}</strong>
            </article>
          ))}
          {reservations.length === 0 && (
            <p className="reservation-note">희망 시간대를 지정하고 가능 여부를 확인하면 예약이 즉시 확정됩니다.</p>
          )}
          {activeReservation?.status === "CONFIRMED" && (
            <div className="reservation-confirmed">
              <p className="reservation-note">
                {technicianAccepted
                  ? `${activeReservation.technicianName ? `${activeReservation.technicianName} 기사님이 ` : "기사님이 "}예약을 수락하여 취소할 수 없습니다.`
                  : isFuture(activeReservation.scheduledStartAt)
                    ? "예약이 확정되었습니다. 기사 수락 전까지만 취소할 수 있습니다."
                    : "예약 시작 후에는 예약을 취소할 수 없습니다."}
              </p>
              <button
                className="text-button reservation-cancel"
                disabled={cancelling || !canCancel}
                onClick={() => void cancel()}
                type="button"
              >
                {cancelling ? "취소 중..." : "예약 취소"}
              </button>
            </div>
          )}
          {reservationsLoaded && !activeReservation && (
            <form className="reservation-form" onSubmit={(event) => void submitReservation(event)}>
              <label>
                예약 시작 시각
                <input
                  required
                  type="datetime-local"
                  value={form.startAt}
                  onChange={(event) => changeField("startAt", event.target.value)}
                />
              </label>
              <label>
                예약 종료 시각
                <input
                  required
                  type="datetime-local"
                  value={form.endAt}
                  onChange={(event) => changeField("endAt", event.target.value)}
                />
              </label>
              <label>
                요청 메모 (선택)
                <textarea
                  maxLength={2000}
                  rows={2}
                  value={form.memo}
                  onChange={(event) => changeField("memo", event.target.value)}
                />
              </label>
              <div className="reservation-actions">
                <button
                  className="secondary-button"
                  disabled={checking || saving || !form.startAt || !form.endAt}
                  onClick={() => void checkAvailability()}
                  type="button"
                >
                  {checking ? "확인 중..." : "예약 가능 여부 확인"}
                </button>
                <button
                  className="primary-button"
                  disabled={saving || checking || availability !== true}
                  type="submit"
                >
                  {saving ? "저장 중..." : "예약 확정"}
                </button>
              </div>
            </form>
          )}
        </>
      )}
      {message && <p className="success-message" role="status">{message}</p>}
      {error && <p className="inline-error" role="alert">{error}</p>}
    </section>
  );
}

function initialSchedule(): ScheduleForm {
  const start = new Date(Date.now() + 24 * 60 * 60 * 1000);
  start.setHours(9, 0, 0, 0);
  const end = new Date(start.getTime() + 4 * 60 * 60 * 1000);
  return { startAt: toLocalDateTime(start.toISOString()), endAt: toLocalDateTime(end.toISOString()), memo: "" };
}

function createIdempotencyKey(): string {
  return crypto.randomUUID();
}

function toLocalDateTime(value: string): string {
  const date = new Date(value);
  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return localDate.toISOString().slice(0, 16);
}

function isFuture(value: string): boolean {
  return new Date(value).getTime() > Date.now();
}

const ACCEPTED_ASSIGNMENT_STATUSES: NonNullable<Reservation["technicianAssignmentStatus"]>[] = [
  "ACCEPTED",
  "ON_THE_WAY",
  "WORK_STARTED",
  "WORK_COMPLETED",
];

function canCustomerCancel(reservation: Reservation): boolean {
  return reservation.status === "CONFIRMED"
    && isFuture(reservation.scheduledStartAt)
    && !(reservation.technicianAssignmentStatus
      && ACCEPTED_ASSIGNMENT_STATUSES.includes(reservation.technicianAssignmentStatus));
}

function formatDateTime(value: string): string {
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(new Date(value));
}

function formatCurrency(value: number): string {
  return new Intl.NumberFormat("ko-KR", { style: "currency", currency: "KRW", maximumFractionDigits: 0 })
    .format(value);
}

function reservationStatusLabel(status: Reservation["status"]): string {
  const labels: Record<Reservation["status"], string> = {
    PENDING: "확정 대기",
    CONFIRMED: "확정",
    RESCHEDULE_REQUESTED: "변경 요청",
    CANCELLED: "취소",
    EXPIRED: "만료",
    COMPLETED: "완료",
  };
  return labels[status];
}

function toMessage(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  if (error instanceof Error) return error.message;
  return "예약 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.";
}
