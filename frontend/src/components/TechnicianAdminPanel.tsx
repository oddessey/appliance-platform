import { useCallback, useEffect, useState } from "react";
import { PricingAdminPanel } from "./PricingAdminPanel";
import { ReservationAdminPanel } from "./ReservationAdminPanel";
import { AssignmentCancelDialog } from "./AssignmentCancelDialog";
import { TechnicianAssignmentDialog } from "./TechnicianAssignmentDialog";
import {
  cancelTechnicianAssignment,
  getAdminTechnicians,
  getPendingTechnicianReservations,
  updateTechnicianApproval,
} from "../api/installations";
import { getAdminReservations } from "../api/reservations";
import { ApiError } from "../api/client";
import type {
  PendingTechnicianReservation,
  TechnicianAdminSummary,
  TechnicianApprovalStatus,
  TechnicianAssignmentStatus,
} from "../types/installation";
import type { Reservation } from "../types/reservation";

const APPROVAL_FILTERS: Array<{ label: string; value?: TechnicianApprovalStatus }> = [
  { label: "전체" },
  { label: "승인 대기", value: "PENDING" },
  { label: "승인", value: "APPROVED" },
  { label: "반려", value: "REJECTED" },
];

const ASSIGNMENT_STATUS_FILTERS: Array<{ label: string; value: TechnicianAssignmentStatus }> = [
  { label: "기사 수락 대기", value: "PENDING" },
  { label: "기사 수락 완료", value: "ACCEPTED" },
  { label: "이동 중", value: "ON_THE_WAY" },
];

const PAGE_SIZE = 20;

interface TechnicianAdminPanelProps {
  onReviewRequests: () => void;
}

export function TechnicianAdminPanel({ onReviewRequests }: TechnicianAdminPanelProps) {
  const [showPricing, setShowPricing] = useState(false);
  const [showReservations, setShowReservations] = useState(false);
  const [filter, setFilter] = useState<TechnicianApprovalStatus | undefined>("PENDING");
  const [technicians, setTechnicians] = useState<TechnicianAdminSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [busyProfileId, setBusyProfileId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [pendingPage, setPendingPage] = useState(0);
  const [pendingReservations, setPendingReservations] = useState<PendingTechnicianReservation[]>([]);
  const [pendingTotal, setPendingTotal] = useState(0);
  const [pendingLoading, setPendingLoading] = useState(true);
  const [pendingError, setPendingError] = useState<string | null>(null);
  const [pendingMessage, setPendingMessage] = useState<string | null>(null);
  const [assignTarget, setAssignTarget] = useState<PendingTechnicianReservation | null>(null);

  const [assignmentFilter, setAssignmentFilter] = useState<TechnicianAssignmentStatus>("PENDING");
  const [assignmentPage, setAssignmentPage] = useState(0);
  const [assignmentReservations, setAssignmentReservations] = useState<Reservation[]>([]);
  const [assignmentTotal, setAssignmentTotal] = useState(0);
  const [assignmentLoading, setAssignmentLoading] = useState(true);
  const [assignmentError, setAssignmentError] = useState<string | null>(null);
  const [assignmentMessage, setAssignmentMessage] = useState<string | null>(null);
  const [cancelTarget, setCancelTarget] = useState<Reservation | null>(null);
  const [cancelBusy, setCancelBusy] = useState(false);
  const [cancelError, setCancelError] = useState<string | null>(null);

  const loadTechnicians = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const page = await getAdminTechnicians(filter);
      setTechnicians(page.content);
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, [filter]);

  const loadPendingReservations = useCallback(async (requestedPage = pendingPage, showLoading = true) => {
    if (showLoading) setPendingLoading(true);
    setPendingError(null);
    try {
      const result = await getPendingTechnicianReservations(requestedPage, PAGE_SIZE);
      if (result.content.length === 0 && requestedPage > 0) {
        setPendingPage(requestedPage - 1);
        return;
      }
      setPendingReservations(result.content);
      setPendingTotal(result.totalElements);
      setPendingPage(result.page);
    } catch (requestError) {
      setPendingError(toMessage(requestError));
    } finally {
      if (showLoading) setPendingLoading(false);
    }
  }, [pendingPage]);

  const loadAssignments = useCallback(async () => {
    setAssignmentLoading(true);
    setAssignmentError(null);
    try {
      const result = await getAdminReservations("CONFIRMED", assignmentPage, PAGE_SIZE, assignmentFilter);
      if (result.items.length === 0 && assignmentPage > 0) {
        setAssignmentPage(assignmentPage - 1);
        return;
      }
      setAssignmentReservations(result.items);
      setAssignmentTotal(result.totalElements);
    } catch (requestError) {
      setAssignmentError(toMessage(requestError));
    } finally {
      setAssignmentLoading(false);
    }
  }, [assignmentFilter, assignmentPage]);

  useEffect(() => {
    void loadTechnicians();
  }, [loadTechnicians]);

  useEffect(() => {
    void loadPendingReservations(pendingPage);
  }, [loadPendingReservations, pendingPage]);

  useEffect(() => {
    void loadAssignments();
  }, [loadAssignments]);

  async function changeApproval(
    technician: TechnicianAdminSummary,
    approvalStatus: TechnicianApprovalStatus,
    active: boolean,
  ) {
    setBusyProfileId(technician.id);
    setError(null);
    try {
      await updateTechnicianApproval(technician.id, approvalStatus, active);
      await loadTechnicians();
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      setBusyProfileId(null);
    }
  }

  async function handleAssigned(technicianName: string, reservationNo: string) {
    setAssignTarget(null);
    setPendingMessage(`${reservationNo} 예약을 ${technicianName} 기사에게 배정했습니다. 기사 수락 후 배정 진행 현황에서 확인할 수 있습니다.`);
    setAssignmentMessage(null);
    await Promise.all([loadPendingReservations(pendingPage, false), loadAssignments()]);
  }

  async function confirmCancelAssignment() {
    const target = cancelTarget;
    if (!target || !target.technicianAssignmentId || cancelBusy) return;
    setCancelBusy(true);
    setCancelError(null);
    try {
      await cancelTechnicianAssignment(target.technicianAssignmentId);
      setCancelTarget(null);
      setAssignmentMessage(`${target.reservationNo} 기사 배정을 취소했습니다. 배정 대기 목록에서 다시 배정할 수 있습니다.`);
      setPendingMessage(null);
      await Promise.all([loadAssignments(), loadPendingReservations(pendingPage, false)]);
    } catch (requestError) {
      setCancelError(toMessage(requestError));
    } finally {
      setCancelBusy(false);
    }
  }

  if (showPricing) {
    return <PricingAdminPanel onClose={() => setShowPricing(false)} />;
  }
  if (showReservations) {
    return (
      <ReservationAdminPanel
        onReviewAssignments={() => {
          setShowReservations(false);
          onReviewRequests();
        }}
      />
    );
  }

  return (
    <section aria-labelledby="technician-admin-title" className="technician-admin-panel">
      <div className="technician-profile-heading">
        <div>
          <span className="step-label">TECHNICIAN MANAGEMENT</span>
          <h2 id="technician-admin-title">기사 승인 및 배정</h2>
          <p>프로필을 검토·활성화하고, 확정 예약의 기사 배정·재배정·배정 취소를 처리합니다.</p>
        </div>
        <div className="technician-heading-actions">
          <button className="text-button" onClick={() => setShowReservations(true)} type="button">
            예약 관리
          </button>
          <button className="text-button" onClick={() => setShowPricing(true)} type="button">
            가격 규칙·견적 관리
          </button>
          <button className="text-button" onClick={onReviewRequests} type="button">
            요청·AI 검토에서 기사 배정
          </button>
          <button className="text-button" disabled={loading} onClick={() => void loadTechnicians()} type="button">
            기사 새로고침
          </button>
        </div>
      </div>

      <div aria-label="기사 승인 상태 필터" className="technician-filters" role="tablist">
        {APPROVAL_FILTERS.map((item) => (
          <button
            aria-selected={filter === item.value}
            className={filter === item.value ? "active" : ""}
            key={item.label}
            onClick={() => setFilter(item.value)}
            role="tab"
            type="button"
          >
            {item.label}
          </button>
        ))}
      </div>

      {error && <p className="inline-error" role="alert">{error}</p>}
      {loading ? (
        <p aria-live="polite" className="history-loading">기사 프로필을 불러오는 중...</p>
      ) : technicians.length === 0 ? (
        <p className="empty-state">조회할 기사 프로필이 없습니다.</p>
      ) : (
        <div className="technician-admin-list">
          {technicians.map((technician) => (
            <article className="technician-admin-card" key={technician.id}>
              <div>
                <strong>{technician.name} · {technician.businessName || "사업자명 미등록"}</strong>
                <span>{technician.email} · {technician.phone}</span>
                <span>지역: {technician.serviceAreas || "미등록"} · 취급 품목: {technician.applianceTypeCodes || "미등록"}</span>
                <span>자격: {technician.licenseInfo || "미등록"} · 가능 시간: {technician.availability || "미등록"}</span>
                <span>
                  {approvalLabel(technician.approvalStatus)}
                  {technician.active ? " · 배정 활성" : " · 배정 비활성"}
                </span>
              </div>
              {technician.active ? (
                <button
                  className="secondary-button"
                  disabled={busyProfileId === technician.id}
                  onClick={() => void changeApproval(technician, "APPROVED", false)}
                  type="button"
                >
                  배정 비활성화
                </button>
              ) : (
                <>
                  <button
                    className="primary-button"
                    disabled={busyProfileId === technician.id}
                    onClick={() => void changeApproval(technician, "APPROVED", true)}
                    type="button"
                  >
                    {busyProfileId === technician.id ? "처리 중..." : "승인 및 활성화"}
                  </button>
                  {technician.approvalStatus !== "REJECTED" && (
                    <button
                      className="secondary-button"
                      disabled={busyProfileId === technician.id}
                      onClick={() => void changeApproval(technician, "REJECTED", false)}
                      type="button"
                    >
                      반려
                    </button>
                  )}
                </>
              )}
            </article>
          ))}
        </div>
      )}

      <section aria-labelledby="technician-pending-title" className="technician-assignment-manage">
        <div className="admin-module-heading">
          <div>
            <span className="step-label">ASSIGNMENT QUEUE</span>
            <h3 id="technician-pending-title">확정 예약 기사 배정</h3>
            <p>고객이 예약을 확정했지만 담당 기사가 없는 예약입니다. 후보를 조회해 적격 기사를 배정하세요.</p>
          </div>
          <button
            className="text-button"
            disabled={pendingLoading}
            onClick={() => void loadPendingReservations(pendingPage)}
            type="button"
          >
            새로고침
          </button>
        </div>
        {pendingMessage && <p className="success-message" role="status">{pendingMessage}</p>}
        {pendingError && (
          <div className="load-error" role="alert">
            <p className="inline-error">{pendingError}</p>
            <button className="text-button" onClick={() => void loadPendingReservations(pendingPage)} type="button">
              다시 시도
            </button>
          </div>
        )}
        {pendingLoading ? (
          <p aria-live="polite" className="history-loading">배정 대기 예약을 불러오는 중...</p>
        ) : pendingReservations.length === 0 ? (
          <p className="empty-state">기사 배정이 필요한 확정 예약이 없습니다.</p>
        ) : (
          <div className="pending-assignment-list">
            {pendingReservations.map((reservation) => (
              <article className="pending-assignment-card" key={reservation.reservationId}>
                <div className="admin-installation-card-heading">
                  <div>
                    <strong>{reservation.reservationNo}</strong>
                    <span className="admin-status-badge">확정 · 기사 배정 필요</span>
                  </div>
                  <time dateTime={reservation.scheduledStartAt}>{formatDate(reservation.scheduledStartAt)}</time>
                </div>
                <dl className="admin-installation-details">
                  <div><dt>고객</dt><dd>{reservation.customerName} · {reservation.customerPhone}</dd></div>
                  <div><dt>제품</dt><dd>{reservation.applianceTypes}</dd></div>
                  <div><dt>예약 일정</dt><dd>{formatDate(reservation.scheduledStartAt)} – {formatDate(reservation.scheduledEndAt)}</dd></div>
                  <div><dt>이전 주소</dt><dd>{reservation.originAddress}</dd></div>
                  <div><dt>설치 주소</dt><dd>{reservation.destinationAddress}</dd></div>
                </dl>
                <button className="secondary-button" onClick={() => setAssignTarget(reservation)} type="button">
                  기사 후보 조회·배정
                </button>
              </article>
            ))}
          </div>
        )}
        <div className="admin-pagination">
          <button
            className="text-button"
            disabled={pendingPage === 0 || pendingLoading}
            onClick={() => setPendingPage((current) => current - 1)}
            type="button"
          >
            이전
          </button>
          <span>{pendingPage + 1} 페이지 · 전체 {pendingTotal}건</span>
          <button
            className="text-button"
            disabled={(pendingPage + 1) * PAGE_SIZE >= pendingTotal || pendingLoading}
            onClick={() => setPendingPage((current) => current + 1)}
            type="button"
          >
            다음
          </button>
        </div>
      </section>

      <section aria-labelledby="technician-assignment-status-title" className="technician-assignment-manage">
        <div className="admin-module-heading">
          <div>
            <span className="step-label">ASSIGNMENT STATUS</span>
            <h3 id="technician-assignment-status-title">배정 진행 현황</h3>
            <p>기사 수락 대기·수락 완료·이동 중 배정을 확인하고, 작업 시작 전 배정은 관리자가 취소할 수 있습니다.</p>
          </div>
          <button className="text-button" disabled={assignmentLoading} onClick={() => void loadAssignments()} type="button">
            새로고침
          </button>
        </div>
        <div aria-label="배정 상태 필터" className="technician-filters" role="tablist">
          {ASSIGNMENT_STATUS_FILTERS.map((item) => (
            <button
              aria-selected={assignmentFilter === item.value}
              className={assignmentFilter === item.value ? "active" : ""}
              key={item.value}
              onClick={() => {
                setAssignmentFilter(item.value);
                setAssignmentPage(0);
              }}
              role="tab"
              type="button"
            >
              {item.label}
            </button>
          ))}
        </div>
        {assignmentMessage && <p className="success-message" role="status">{assignmentMessage}</p>}
        {assignmentError && (
          <div className="load-error" role="alert">
            <p className="inline-error">{assignmentError}</p>
            <button className="text-button" onClick={() => void loadAssignments()} type="button">다시 시도</button>
          </div>
        )}
        {assignmentLoading ? (
          <p aria-live="polite" className="history-loading">배정된 예약을 불러오는 중...</p>
        ) : assignmentReservations.length === 0 ? (
          <p className="empty-state">해당 상태의 기사 배정 예약이 없습니다.</p>
        ) : (
          <div className="technician-assignment-list">
            {assignmentReservations.map((reservation) => (
              <article className="technician-candidate-row" key={reservation.id}>
                <span>
                  <strong>{reservation.reservationNo} · {reservation.technicianName ?? "기사 정보 없음"} 기사</strong>
                  <small>
                    {assignmentStatusLabel(reservation.technicianAssignmentStatus)} · {formatDate(reservation.scheduledStartAt)}
                    {" ~ "}{formatDate(reservation.scheduledEndAt)}
                  </small>
                  <small>{reservation.originAddress} → {reservation.destinationAddress}</small>
                </span>
                {reservation.technicianAssignmentId && (
                  <button
                    className="text-button reservation-cancel"
                    onClick={() => {
                      setAssignmentMessage(null);
                      setCancelError(null);
                      setCancelTarget(reservation);
                    }}
                    type="button"
                  >
                    배정 취소
                  </button>
                )}
              </article>
            ))}
          </div>
        )}
        <div className="admin-pagination">
          <button
            className="text-button"
            disabled={assignmentPage === 0 || assignmentLoading}
            onClick={() => setAssignmentPage((current) => current - 1)}
            type="button"
          >
            이전
          </button>
          <span>{assignmentPage + 1} 페이지 · 전체 {assignmentTotal}건</span>
          <button
            className="text-button"
            disabled={(assignmentPage + 1) * PAGE_SIZE >= assignmentTotal || assignmentLoading}
            onClick={() => setAssignmentPage((current) => current + 1)}
            type="button"
          >
            다음
          </button>
        </div>
      </section>

      {assignTarget && (
        <TechnicianAssignmentDialog
          addressesLabel={`${assignTarget.originAddress} → ${assignTarget.destinationAddress}`}
          onAssigned={(technicianName) => handleAssigned(technicianName, assignTarget.reservationNo)}
          onClose={() => setAssignTarget(null)}
          reassign={false}
          reservationId={assignTarget.reservationId}
          reservationNo={assignTarget.reservationNo}
          scheduleLabel={`${formatDate(assignTarget.scheduledStartAt)} ~ ${formatDate(assignTarget.scheduledEndAt)}`}
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

function approvalLabel(status: TechnicianApprovalStatus): string {
  const labels = { PENDING: "승인 대기", APPROVED: "승인", REJECTED: "반려" };
  return labels[status];
}

function assignmentStatusLabel(status: TechnicianAssignmentStatus | null): string {
  if (!status) return "기사 미배정";
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

function formatDate(value: string): string {
  return new Intl.DateTimeFormat("ko-KR", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function toMessage(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  if (error instanceof Error) return error.message;
  return "기사 관리 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.";
}
