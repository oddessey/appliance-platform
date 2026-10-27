import { useCallback, useEffect, useState } from "react";
import {
  acceptAssignment,
  addAssignmentEvent,
  downloadAssignmentEvidence,
  getAssignmentEvidence,
  getTechnicianAssignments,
  rejectAssignment,
  uploadAssignmentEvidence,
} from "../api/installations";
import { ApiError } from "../api/client";
import { TechnicianProfilePanel } from "./TechnicianProfilePanel";
import type {
  TechnicianAssignment,
  TechnicianAssignmentStatus,
  TechnicianEvidence,
} from "../types/installation";

const FILTERS: Array<{ label: string; value?: TechnicianAssignmentStatus }> = [
  { label: "배정 대기", value: "PENDING" },
  { label: "전체" },
  { label: "수락", value: "ACCEPTED" },
  { label: "이동 중", value: "ON_THE_WAY" },
  { label: "작업 중", value: "WORK_STARTED" },
  { label: "완료", value: "WORK_COMPLETED" },
  { label: "거절", value: "REJECTED" },
  { label: "예약 취소", value: "CANCELLED" },
];

interface TechnicianDashboardProps {
  technicianName: string;
  profileOpen: boolean;
  onOpenProfile: () => void;
  onCloseProfile: () => void;
}

export function TechnicianDashboard({
  technicianName,
  profileOpen,
  onOpenProfile,
  onCloseProfile,
}: TechnicianDashboardProps) {
  const [assignments, setAssignments] = useState<TechnicianAssignment[]>([]);
  const [status, setStatus] = useState<TechnicianAssignmentStatus | undefined>("PENDING");
  const [page, setPage] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [busyAssignmentId, setBusyAssignmentId] = useState<string | null>(null);
  const [rejectTarget, setRejectTarget] = useState<TechnicianAssignment | null>(null);
  const [rejectReason, setRejectReason] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [actionMessage, setActionMessage] = useState<string | null>(null);
  const [uploadedEvidence, setUploadedEvidence] = useState<Record<string, TechnicianEvidence[]>>({});

  const loadAssignments = useCallback(async (
    requestedPage: number,
    requestedStatus = status,
    showLoading = true,
  ) => {
    if (showLoading) setLoading(true);
    setError(null);
    try {
      const result = await getTechnicianAssignments(requestedPage, 20, requestedStatus);
      setAssignments(result.content);
      setPage(result.page);
      setTotalElements(result.totalElements);
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      if (showLoading) setLoading(false);
    }
  }, [status]);

  useEffect(() => {
    void loadAssignments(0);
  }, [loadAssignments]);

  useEffect(() => {
    function refreshAssignments() {
      if (document.visibilityState === "visible") {
        void loadAssignments(page, status, false);
      }
    }

    const intervalId = window.setInterval(refreshAssignments, 15_000);
    window.addEventListener("focus", refreshAssignments);
    document.addEventListener("visibilitychange", refreshAssignments);
    return () => {
      window.clearInterval(intervalId);
      window.removeEventListener("focus", refreshAssignments);
      document.removeEventListener("visibilitychange", refreshAssignments);
    };
  }, [loadAssignments, page, status]);

  async function runAction(
    assignment: TechnicianAssignment,
    action: () => Promise<TechnicianAssignment>,
    message: string,
  ) {
    setBusyAssignmentId(assignment.id);
    setError(null);
    setActionMessage(null);
    try {
      await action();
      setActionMessage(message);
      await loadAssignments(page);
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      setBusyAssignmentId(null);
    }
  }

  async function submitRejection() {
    if (!rejectTarget || !rejectReason.trim()) {
      return;
    }
    const target = rejectTarget;
    await runAction(
      target,
      () => rejectAssignment(target.id, rejectReason.trim()),
      "배정을 거절하고 사유를 기록했습니다.",
    );
    setRejectTarget(null);
    setRejectReason("");
  }

  async function submitEvidence(assignment: TechnicianAssignment, file: File) {
    setBusyAssignmentId(assignment.id);
    setError(null);
    setActionMessage(null);
    try {
      const evidence = await uploadAssignmentEvidence(assignment.id, file);
      setUploadedEvidence((current) => ({
        ...current,
        [assignment.id]: [...(current[assignment.id] ?? []), evidence],
      }));
      setActionMessage(`작업 증빙 이미지 '${evidence.originalFilename}'를 등록했습니다.`);
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      setBusyAssignmentId(null);
    }
  }

  async function downloadEvidence(assignmentId: string, evidence: TechnicianEvidence) {
    setError(null);
    try {
      const blob = await downloadAssignmentEvidence(assignmentId, evidence.id);
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = evidence.originalFilename;
      link.click();
      window.setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (requestError) {
      setError(toMessage(requestError));
    }
  }

  function changeFilter(nextStatus?: TechnicianAssignmentStatus) {
    setStatus(nextStatus);
    setPage(0);
    setAssignments([]);
  }

  if (profileOpen) {
    return <TechnicianProfilePanel onClose={onCloseProfile} />;
  }

  return (
    <section aria-labelledby="technician-dashboard-title" className="technician-dashboard">
      <div className="technician-heading">
        <div>
          <span className="step-label">TECHNICIAN PORTAL</span>
          <h2 id="technician-dashboard-title">{technicianName} 기사님, 안녕하세요.</h2>
          <p>배정 대기 작업부터 표시합니다. 새 관리 배정은 15초마다 자동으로 반영됩니다.</p>
        </div>
        <div className="technician-heading-actions">
          <button className="text-button" onClick={onOpenProfile} type="button">
            기사 프로필·일정
          </button>
          <button className="address-search-button" disabled={loading} onClick={() => void loadAssignments(page)} type="button">
            새로고침
          </button>
        </div>
      </div>

      <div aria-label="작업 상태 필터" className="technician-filters" role="tablist">
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

      {actionMessage && <p className="success-message" role="status">{actionMessage}</p>}
      {error && (
        <div className="load-error" role="alert">
          <p className="inline-error">{error}</p>
          <button className="text-button" onClick={() => void loadAssignments(page)} type="button">다시 시도</button>
        </div>
      )}

      {loading ? (
        <p aria-live="polite" className="history-loading">배정된 작업을 불러오는 중...</p>
      ) : assignments.length === 0 ? (
        <div className="technician-empty">
          <span aria-hidden="true">✓</span>
          <h3>{status ? `${statusLabel(status)} 작업이 없습니다.` : "배정된 작업이 없습니다."}</h3>
          <p>새로운 작업이 배정되면 이 화면에 표시됩니다.</p>
        </div>
      ) : (
        <>
          <div className="technician-assignment-list">
            {assignments.map((assignment) => (
              <TechnicianAssignmentCard
                assignment={assignment}
                busy={busyAssignmentId === assignment.id}
                key={assignment.id}
                onAccept={() => void runAction(
                  assignment,
                  () => acceptAssignment(assignment.id),
                  "작업 배정을 수락했습니다.",
                )}
                onEvent={(eventType) => void runAction(
                  assignment,
                  () => addAssignmentEvent(assignment.id, eventType, eventNote(eventType)),
                  `${statusLabel(eventType)} 상태로 변경했습니다.`,
                )}
                onReject={() => {
                  setRejectTarget(assignment);
                  setRejectReason("");
                }}
                evidence={uploadedEvidence[assignment.id]}
                onUploadEvidence={(file) => void submitEvidence(assignment, file)}
                onDownloadEvidence={(item) => void downloadEvidence(assignment.id, item)}
              />
            ))}
          </div>
          <div className="admin-pagination">
            <button className="text-button" disabled={page === 0 || loading} onClick={() => void loadAssignments(page - 1)} type="button">
              이전
            </button>
            <span>{page + 1} 페이지 · 전체 {totalElements}건</span>
            <button
              className="text-button"
              disabled={(page + 1) * 20 >= totalElements || loading}
              onClick={() => void loadAssignments(page + 1)}
              type="button"
            >
              다음
            </button>
          </div>
        </>
      )}

      {rejectTarget && (
        <div className="popup-backdrop" onMouseDown={(event) => {
          if (event.target === event.currentTarget && !busyAssignmentId) {
            setRejectTarget(null);
          }
        }}>
          <section aria-labelledby="reject-assignment-title" aria-modal="true" className="popup-card reject-assignment-dialog" role="dialog">
            <div className="popup-heading">
              <h2 id="reject-assignment-title">배정 거절</h2>
              <button className="text-button" disabled={Boolean(busyAssignmentId)} onClick={() => setRejectTarget(null)} type="button">닫기</button>
            </div>
            <p>{rejectTarget.reservationNo} 작업의 거절 사유를 입력해 주세요.</p>
            <label className="reject-reason-label">
              거절 사유
              <textarea
                maxLength={500}
                onChange={(event) => setRejectReason(event.target.value)}
                placeholder="예: 해당 일정에 작업이 어렵습니다."
                required
                rows={4}
                value={rejectReason}
              />
            </label>
            <button
              className="primary-button"
              disabled={!rejectReason.trim() || Boolean(busyAssignmentId)}
              onClick={() => void submitRejection()}
              type="button"
            >
              {busyAssignmentId ? "처리 중..." : "사유 제출 후 거절"}
            </button>
          </section>
        </div>
      )}
    </section>
  );
}

interface TechnicianAssignmentCardProps {
  assignment: TechnicianAssignment;
  busy: boolean;
  onAccept: () => void;
  onReject: () => void;
  onEvent: (status: Exclude<TechnicianAssignmentStatus, "PENDING" | "REJECTED" | "CANCELLED">) => void;
  onUploadEvidence: (file: File) => void;
  onDownloadEvidence: (evidence: TechnicianEvidence) => void;
  evidence?: TechnicianEvidence[];
}

function TechnicianAssignmentCard({
  assignment,
  busy,
  onAccept,
  onReject,
  onEvent,
  onUploadEvidence,
  onDownloadEvidence,
  evidence,
}: TechnicianAssignmentCardProps) {
  const [evidenceItems, setEvidenceItems] = useState<TechnicianEvidence[]>(evidence ?? []);
  const [evidenceLoading, setEvidenceLoading] = useState(false);
  const [evidenceError, setEvidenceError] = useState<string | null>(null);
  const [evidenceLoaded, setEvidenceLoaded] = useState(false);

  useEffect(() => {
    if (evidence) {
      setEvidenceItems(evidence);
      setEvidenceLoaded(true);
    }
  }, [evidence]);

  async function loadEvidence() {
    setEvidenceLoading(true);
    setEvidenceError(null);
    try {
      setEvidenceItems(await getAssignmentEvidence(assignment.id));
      setEvidenceLoaded(true);
    } catch (requestError) {
      setEvidenceError(toMessage(requestError));
    } finally {
      setEvidenceLoading(false);
    }
  }

  return (
    <article className="technician-assignment-card">
      <div className="technician-assignment-heading">
        <div>
          <span className={`request-status status-${assignment.status.toLowerCase()}`}>
            {statusLabel(assignment.status)}
          </span>
          <h3>{assignment.applianceTypes} 설치</h3>
        </div>
        <span className="request-number">{assignment.reservationNo}</span>
      </div>

      <dl className="history-details">
        {assignment.quoteNo && assignment.quoteTotalAmount !== null && (
          <div>
            <dt>발행 견적</dt>
            <dd>{assignment.quoteNo} · {formatCurrency(assignment.quoteTotalAmount)}</dd>
          </div>
        )}
        <div>
          <dt>작업 일정</dt>
          <dd>{formatDateTime(assignment.scheduledStartAt)} – {formatTime(assignment.scheduledEndAt)}</dd>
        </div>
        <div>
          <dt>고객</dt>
          <dd>{assignment.customerName} · <a href={`tel:${assignment.customerPhone}`}>{assignment.customerPhone}</a></dd>
        </div>
        <div>
          <dt>이전 주소</dt>
          <dd>{assignment.originAddress}</dd>
        </div>
        <div>
          <dt>설치 주소</dt>
          <dd>{assignment.destinationAddress}</dd>
        </div>
        {assignment.customerMemo && (
          <div>
            <dt>고객 요청</dt>
            <dd>{assignment.customerMemo}</dd>
          </div>
        )}
      </dl>

      <div className="technician-actions">
        {assignment.status === "PENDING" && (
          <>
            <button className="primary-button" disabled={busy} onClick={onAccept} type="button">
              {busy ? "처리 중..." : "작업 수락"}
            </button>
            <button className="secondary-button" disabled={busy} onClick={onReject} type="button">
              배정 거절
            </button>
          </>
        )}
        {assignment.status === "ACCEPTED" && (
          <button className="primary-button" disabled={busy} onClick={() => onEvent("ON_THE_WAY")} type="button">
            {busy ? "처리 중..." : "출발 처리"}
          </button>
        )}
        {assignment.status === "ON_THE_WAY" && (
          <button className="primary-button" disabled={busy} onClick={() => onEvent("WORK_STARTED")} type="button">
            {busy ? "처리 중..." : "작업 시작"}
          </button>
        )}
        {assignment.status === "WORK_STARTED" && (
          <button className="primary-button" disabled={busy} onClick={() => onEvent("WORK_COMPLETED")} type="button">
            {busy ? "처리 중..." : "작업 완료"}
          </button>
        )}
        {assignment.status === "WORK_COMPLETED" && (
          <p className="completed-note">작업이 완료되었습니다.</p>
        )}
        {(assignment.status === "WORK_STARTED" || assignment.status === "WORK_COMPLETED") && (
          <div className="technician-evidence">
            <label className="secondary-button">
              {busy ? "업로드 중..." : "완료 증빙 이미지 등록"}
              <input
                accept="image/jpeg,image/png,image/webp"
                aria-label={`${assignment.reservationNo} 완료 증빙 이미지`}
                disabled={busy}
                onChange={(event) => {
                  const file = event.currentTarget.files?.[0];
                  if (file) onUploadEvidence(file);
                  event.currentTarget.value = "";
                }}
                type="file"
              />
            </label>
            <button className="text-button" disabled={evidenceLoading} onClick={() => void loadEvidence()} type="button">
              {evidenceLoading ? "불러오는 중..." : "등록 증빙 조회"}
            </button>
            {evidenceError && <p className="inline-error" role="alert">{evidenceError}</p>}
            {evidenceLoaded && evidenceItems.length === 0 && <span>등록된 증빙이 없습니다.</span>}
            {evidenceItems.map((item) => (
              <button
                className="text-button"
                key={item.id}
                onClick={() => onDownloadEvidence(item)}
                type="button"
              >
                {item.originalFilename} 내려받기
              </button>
            ))}
          </div>
        )}
        {assignment.status === "REJECTED" && (
          <p className="completed-note">거절한 배정입니다.</p>
        )}
        {assignment.status === "CANCELLED" && (
          <p className="completed-note">예약 일정 변경 또는 취소로 배정이 종료되었습니다.</p>
        )}
      </div>
    </article>
  );
}

function statusLabel(status: TechnicianAssignmentStatus): string {
  const labels: Record<TechnicianAssignmentStatus, string> = {
    PENDING: "배정 대기",
    ACCEPTED: "수락",
    REJECTED: "거절",
    ON_THE_WAY: "이동 중",
    WORK_STARTED: "작업 중",
    WORK_COMPLETED: "작업 완료",
    CANCELLED: "예약 취소",
  };
  return labels[status];
}

function eventNote(status: Exclude<TechnicianAssignmentStatus, "PENDING" | "REJECTED" | "CANCELLED">): string {
  const notes: Record<typeof status, string> = {
    ACCEPTED: "작업 배정을 수락했습니다.",
    ON_THE_WAY: "작업 장소로 이동을 시작했습니다.",
    WORK_STARTED: "현장 작업을 시작했습니다.",
    WORK_COMPLETED: "현장 작업을 완료했습니다.",
  };
  return notes[status];
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

function formatTime(value: string): string {
  return new Intl.DateTimeFormat("ko-KR", {
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(new Date(value));
}

function formatCurrency(value: number): string {
  return new Intl.NumberFormat("ko-KR", {
    style: "currency",
    currency: "KRW",
    maximumFractionDigits: 0,
  }).format(value);
}

function toMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 403 && error.code === "TECHNICIAN_PROFILE_UNAVAILABLE") {
      return "기사 프로필이 아직 승인되지 않았거나 비활성 상태입니다. 프로필·일정 메뉴에서 정보를 등록하고 승인을 기다려 주세요.";
    }
    return error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return "작업 목록을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.";
}
