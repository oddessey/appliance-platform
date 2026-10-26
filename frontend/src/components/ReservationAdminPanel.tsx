import { useCallback, useEffect, useState } from "react";
import type { FormEvent } from "react";
import {
  adminCancelReservation,
  adminRescheduleReservation,
  getAdminReservations,
} from "../api/reservations";
import { cancelTechnicianAssignment } from "../api/installations";
import { ApiError } from "../api/client";
import { AssignmentCancelDialog } from "./AssignmentCancelDialog";
import { TechnicianAssignmentDialog } from "./TechnicianAssignmentDialog";
import type { Reservation, ReservationStatus } from "../types/reservation";
import type { TechnicianAssignmentStatus } from "../types/installation";

interface ReservationAdminPanelProps {
  onReviewAssignments: () => void;
}

const FILTERS: Array<{ label: string; value?: ReservationStatus }> = [
  { label: "전체" },
  { label: "확정", value: "CONFIRMED" },
  { label: "취소", value: "CANCELLED" },
  { label: "만료", value: "EXPIRED" },
  { label: "완료", value: "COMPLETED" },
];
const PAGE_SIZE = 20;

export function ReservationAdminPanel({ onReviewAssignments }: ReservationAdminPanelProps) {
  const [status, setStatus] = useState<ReservationStatus | undefined>("CONFIRMED");
  const [assignmentStatus, setAssignmentStatus] = useState<TechnicianAssignmentStatus | undefined>();
  const [page, setPage] = useState(0);
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [startAt, setStartAt] = useState("");
  const [endAt, setEndAt] = useState("");
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [assignTarget, setAssignTarget] = useState<Reservation | null>(null);
  const [cancelTarget, setCancelTarget] = useState<Reservation | null>(null);
  const [cancelBusy, setCancelBusy] = useState(false);
  const [cancelError, setCancelError] = useState<string | null>(null);

  const loadReservations = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await getAdminReservations(status, page, PAGE_SIZE, assignmentStatus);
      setReservations(result.items);
      setTotalElements(result.totalElements);
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, [assignmentStatus, page, status]);

  useEffect(() => {
    void loadReservations();
  }, [loadReservations]);

  function startEditing(reservation: Reservation) {
    setEditingId(reservation.id);
    setStartAt(toLocalDateTime(reservation.scheduledStartAt));
    setEndAt(toLocalDateTime(reservation.scheduledEndAt));
    setError(null);
    setMessage(null);
  }

  function changeFilter(nextStatus?: ReservationStatus) {
    setStatus(nextStatus);
    setAssignmentStatus(undefined);
    setPage(0);
    setEditingId(null);
  }

  function changeAssignmentFilter(nextStatus?: TechnicianAssignmentStatus) {
    setAssignmentStatus(nextStatus);
    if (nextStatus) setStatus(undefined);
    setPage(0);
    setEditingId(null);
  }

  async function saveSchedule(event: FormEvent<HTMLFormElement>, reservation: Reservation) {
    event.preventDefault();
    const start = new Date(startAt);
    const end = new Date(endAt);
    if (!startAt || !endAt || Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) {
      setError("변경할 시작과 종료 시각을 입력해 주세요.");
      return;
    }
    setBusyId(reservation.id);
    setError(null);
    setMessage(null);
    try {
      await adminRescheduleReservation(reservation.id, {
        scheduledStartAt: start.toISOString(),
        scheduledEndAt: end.toISOString(),
      });
      setEditingId(null);
      setMessage(`${reservation.reservationNo} 예약 일정을 변경했습니다.`);
      await loadReservations();
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      setBusyId(null);
    }
  }

  async function cancel(reservation: Reservation) {
    if (!window.confirm(`${reservation.reservationNo} 예약을 취소할까요?`)) return;
    setBusyId(reservation.id);
    setError(null);
    setMessage(null);
    try {
      await adminCancelReservation(reservation.id);
      setMessage(`${reservation.reservationNo} 예약을 취소했습니다.`);
      await loadReservations();
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      setBusyId(null);
    }
  }

  async function handleAssigned(reservation: Reservation, technicianName: string) {
    setAssignTarget(null);
    setError(null);
    setMessage(`${reservation.reservationNo} 예약을 ${technicianName} 기사에게 배정했습니다. 기사 수락 후 상태가 갱신됩니다.`);
    await loadReservations();
  }

  async function confirmCancelAssignment() {
    const target = cancelTarget;
    if (!target || !target.technicianAssignmentId || cancelBusy) return;
    setCancelBusy(true);
    setCancelError(null);
    try {
      await cancelTechnicianAssignment(target.technicianAssignmentId);
      setCancelTarget(null);
      setMessage(`${target.reservationNo} 기사 배정을 취소했습니다. 재배정할 수 있습니다.`);
      await loadReservations();
    } catch (requestError) {
      setCancelError(toMessage(requestError));
    } finally {
      setCancelBusy(false);
    }
  }

  return (
    <section aria-labelledby="reservation-admin-title" className="reservation-admin-panel">
      <div className="technician-profile-heading">
        <div>
          <span className="step-label">RESERVATION MANAGEMENT</span>
          <h2 id="reservation-admin-title">예약 관리</h2>
          <p>예약 일정을 관리하고 기사 배정·재배정·배정 취소를 처리합니다.</p>
        </div>
        <div className="technician-heading-actions">
          <button className="text-button" disabled={loading} onClick={() => void loadReservations()} type="button">
            새로고침
          </button>
          <button className="text-button" onClick={onReviewAssignments} type="button">요청·AI 검토로</button>
        </div>
      </div>

      <div aria-label="예약 상태 필터" className="technician-filters" role="tablist">
        {FILTERS.map((filter) => (
          <button
            aria-selected={status === filter.value}
            className={status === filter.value ? "active" : ""}
            key={filter.label}
            onClick={() => changeFilter(filter.value)}
            role="tab"
            type="button"
          >
            {filter.label}
          </button>
        ))}
      </div>
      <div aria-label="기사 배정 상태 필터" className="technician-filters" role="tablist">
        {([
          { label: "모든 배정 상태", value: undefined },
          { label: "기사 수락 대기", value: "PENDING" as const },
          { label: "기사 수락 완료", value: "ACCEPTED" as const },
          { label: "이동 중", value: "ON_THE_WAY" as const },
          { label: "작업 중", value: "WORK_STARTED" as const },
          { label: "작업 완료", value: "WORK_COMPLETED" as const },
          { label: "기사 거절", value: "REJECTED" as const },
          { label: "배정 취소", value: "CANCELLED" as const },
        ]).map((filter) => (
          <button
            aria-selected={assignmentStatus === filter.value}
            className={assignmentStatus === filter.value ? "active" : ""}
            key={filter.label}
            onClick={() => changeAssignmentFilter(filter.value)}
            role="tab"
            type="button"
          >
            {filter.label}
          </button>
        ))}
      </div>

      {message && <p className="success-message" role="status">{message}</p>}
      {error && (
        <div className="load-error" role="alert">
          <p className="inline-error">{error}</p>
          <button className="text-button" onClick={() => void loadReservations()} type="button">다시 시도</button>
        </div>
      )}
      {loading ? (
        <p aria-live="polite" className="history-loading">예약을 불러오는 중...</p>
      ) : reservations.length === 0 ? (
        <p className="empty-state">조회할 예약이 없습니다.</p>
      ) : (
        <>
          <div className="reservation-admin-list">
            {reservations.map((reservation) => {
              const canAssign = canAssignTechnician(reservation);
              const canCancelAssignment = canCancelTechnicianAssignment(reservation);
              return (
              <article className="reservation-admin-card" key={reservation.id}>
                <div className="reservation-admin-heading">
                  <div>
                    <span className={`request-status status-${reservation.status.toLowerCase()}`}>
                      {reservationStatusLabel(reservation.status)}
                    </span>
                    <h3>{reservation.reservationNo} · {reservation.requestNo}</h3>
                  </div>
                  <strong>{formatCurrency(reservation.quoteTotalAmount)}</strong>
                </div>
                <dl className="history-details">
                  <div><dt>고객</dt><dd>{reservation.customerName} · {reservation.customerPhone}</dd></div>
                  <div><dt>예약 일정</dt><dd>{formatDateTime(reservation.scheduledStartAt)} ~ {formatDateTime(reservation.scheduledEndAt)}</dd></div>
                  <div><dt>이전 주소</dt><dd>{reservation.originAddress}</dd></div>
                  <div><dt>설치 주소</dt><dd>{reservation.destinationAddress}</dd></div>
                  <div><dt>견적</dt><dd>{reservation.quoteNo}</dd></div>
                  <div>
                    <dt>기사 배정</dt>
                    <dd>
                      {reservation.technicianAssignmentStatus
                        ? assignmentStatusLabel(reservation.technicianAssignmentStatus)
                        : "기사 미배정"}
                      {reservation.technicianName ? ` · ${reservation.technicianName} 기사` : ""}
                    </dd>
                  </div>
                  {reservation.customerMemo && <div><dt>고객 메모</dt><dd>{reservation.customerMemo}</dd></div>}
                </dl>
                {canAssign || canCancelAssignment ? (
                  <div className="reservation-actions reservation-assignment-actions">
                    {canAssign && (
                      <button
                        className="secondary-button"
                        disabled={busyId !== null}
                        onClick={() => {
                          setError(null);
                          setMessage(null);
                          setAssignTarget(reservation);
                        }}
                        type="button"
                      >
                        {reservation.technicianAssignmentStatus ? "다른 기사 재배정" : "기사 배정"}
                      </button>
                    )}
                    {canCancelAssignment && (
                      <button
                        className="text-button reservation-cancel"
                        disabled={busyId !== null}
                        onClick={() => {
                          setError(null);
                          setMessage(null);
                          setCancelError(null);
                          setCancelTarget(reservation);
                        }}
                        type="button"
                      >
                        배정 취소
                      </button>
                    )}
                  </div>
                ) : null}
                {editingId === reservation.id ? (
                  <form className="reservation-form" onSubmit={(event) => void saveSchedule(event, reservation)}>
                    <label>
                      시작 시각
                      <input required type="datetime-local" value={startAt} onChange={(event) => setStartAt(event.target.value)} />
                    </label>
                    <label>
                      종료 시각
                      <input required type="datetime-local" value={endAt} onChange={(event) => setEndAt(event.target.value)} />
                    </label>
                    <div className="reservation-actions">
                      <button className="primary-button" disabled={busyId !== null} type="submit">
                        {busyId === reservation.id ? "저장 중..." : "일정 변경 저장"}
                      </button>
                      <button className="secondary-button" onClick={() => setEditingId(null)} type="button">취소</button>
                    </div>
                  </form>
                ) : reservation.status === "CONFIRMED" ? (
                  <div className="reservation-actions">
                    <button className="secondary-button" disabled={busyId !== null} onClick={() => startEditing(reservation)} type="button">
                      일정 변경
                    </button>
                    <button className="text-button reservation-cancel" disabled={busyId !== null} onClick={() => void cancel(reservation)} type="button">
                      예약 취소
                    </button>
                  </div>
                ) : null}
              </article>
              );
            })}
          </div>
          <div className="admin-pagination">
            <button
              className="text-button"
              disabled={page === 0 || loading}
              onClick={() => setPage((current) => current - 1)}
              type="button"
            >
              이전
            </button>
            <span>{page + 1} 페이지 · 전체 {totalElements}건</span>
            <button
              className="text-button"
              disabled={(page + 1) * PAGE_SIZE >= totalElements || loading}
              onClick={() => setPage((current) => current + 1)}
              type="button"
            >
              다음
            </button>
          </div>
        </>
      )}

      {assignTarget && (
        <TechnicianAssignmentDialog
          addressesLabel={`${assignTarget.originAddress} → ${assignTarget.destinationAddress}`}
          onAssigned={(technicianName) => handleAssigned(assignTarget, technicianName)}
          onClose={() => setAssignTarget(null)}
          reassign={assignTarget.technicianAssignmentStatus === "REJECTED"
            || assignTarget.technicianAssignmentStatus === "CANCELLED"}
          reservationId={assignTarget.id}
          reservationNo={assignTarget.reservationNo}
          scheduleLabel={`${formatDateTime(assignTarget.scheduledStartAt)} ~ ${formatDateTime(assignTarget.scheduledEndAt)}`}
        />
      )}
      {cancelTarget && (
        <AssignmentCancelDialog
          busy={cancelBusy}
          error={cancelError}
          onClose={() => {
            if (!cancelBusy) {
              setCancelTarget(null);
              setCancelError(null);
            }
          }}
          onConfirm={() => void confirmCancelAssignment()}
          reservationNo={cancelTarget.reservationNo}
          statusLabel={cancelTarget.technicianAssignmentStatus
            ? assignmentStatusLabel(cancelTarget.technicianAssignmentStatus)
            : ""}
          technicianName={cancelTarget.technicianName}
        />
      )}
    </section>
  );
}

const CANCELLABLE_ASSIGNMENT_STATUSES: TechnicianAssignmentStatus[] = ["PENDING", "ACCEPTED", "ON_THE_WAY"];

function canAssignTechnician(reservation: Reservation): boolean {
  return reservation.status === "CONFIRMED"
    && (!reservation.technicianAssignmentStatus
      || reservation.technicianAssignmentStatus === "REJECTED"
      || reservation.technicianAssignmentStatus === "CANCELLED");
}

function canCancelTechnicianAssignment(reservation: Reservation): boolean {
  return reservation.status === "CONFIRMED"
    && reservation.technicianAssignmentId !== null
    && reservation.technicianAssignmentStatus !== null
    && CANCELLABLE_ASSIGNMENT_STATUSES.includes(reservation.technicianAssignmentStatus);
}

function toLocalDateTime(value: string): string {
  const date = new Date(value);
  return new Date(date.getTime() - date.getTimezoneOffset() * 60_000).toISOString().slice(0, 16);
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

function reservationStatusLabel(status: ReservationStatus): string {
  const labels: Record<ReservationStatus, string> = {
    PENDING: "확정 대기",
    CONFIRMED: "확정",
    RESCHEDULE_REQUESTED: "변경 요청",
    CANCELLED: "취소",
    EXPIRED: "만료",
    COMPLETED: "완료",
  };
  return labels[status];
}

function assignmentStatusLabel(status: TechnicianAssignmentStatus): string {
  const labels: Record<TechnicianAssignmentStatus, string> = {
    PENDING: "기사 수락 대기",
    ACCEPTED: "기사 수락 완료",
    REJECTED: "기사 거절",
    ON_THE_WAY: "이동 중",
    WORK_STARTED: "작업 중",
    WORK_COMPLETED: "작업 완료",
    CANCELLED: "배정 취소",
  };
  return labels[status];
}

function toMessage(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  if (error instanceof Error) return error.message;
  return "예약을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.";
}
