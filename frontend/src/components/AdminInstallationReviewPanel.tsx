import { useCallback, useEffect, useState } from "react";
import {
  assignTechnician,
  getAdminInstallations,
  getPendingTechnicianReservations,
  getTechnicianCandidates,
} from "../api/installations";
import { ApiError } from "../api/client";
import { RequestPhotoGallery } from "./RequestPhotoGallery";
import type {
  AdminInstallationSummary,
  PendingTechnicianReservation,
  TechnicianCandidate,
} from "../types/installation";

const PAGE_SIZE = 20;

export function AdminInstallationReviewPanel() {
  const [reviewRequired, setReviewRequired] = useState(false);
  const [page, setPage] = useState(0);
  const [installations, setInstallations] = useState<AdminInstallationSummary[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [pendingReservations, setPendingReservations] = useState<PendingTechnicianReservation[]>([]);
  const [pendingTotal, setPendingTotal] = useState(0);
  const [pendingPage, setPendingPage] = useState(0);
  const [pendingLoading, setPendingLoading] = useState(true);
  const [pendingError, setPendingError] = useState<string | null>(null);
  const [pendingMessage, setPendingMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadInstallations = useCallback(async (showLoading = true) => {
    if (showLoading) setLoading(true);
    setError(null);
    try {
      const result = await getAdminInstallations(page, PAGE_SIZE, reviewRequired);
      setInstallations(result.content);
      setTotalElements(result.totalElements);
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, [page, reviewRequired]);

  const loadPendingReservations = useCallback(async (
    requestedPage = pendingPage,
    showLoading = true,
  ) => {
    if (showLoading) setPendingLoading(true);
    setPendingError(null);
    try {
      const result = await getPendingTechnicianReservations(requestedPage, PAGE_SIZE);
      if (result.content.length === 0 && requestedPage > 0 && requestedPage * PAGE_SIZE >= result.totalElements) {
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

  useEffect(() => {
    void loadInstallations();
  }, [loadInstallations]);

  useEffect(() => {
    void loadPendingReservations(pendingPage);
  }, [loadPendingReservations, pendingPage]);

  useEffect(() => {
    function refreshPendingReservations() {
      if (document.visibilityState === "visible") {
        void loadPendingReservations(pendingPage, false);
      }
    }

    const intervalId = window.setInterval(refreshPendingReservations, 15_000);
    window.addEventListener("focus", refreshPendingReservations);
    document.addEventListener("visibilitychange", refreshPendingReservations);
    return () => {
      window.clearInterval(intervalId);
      window.removeEventListener("focus", refreshPendingReservations);
      document.removeEventListener("visibilitychange", refreshPendingReservations);
    };
  }, [loadPendingReservations, pendingPage]);

  async function refreshAll() {
    await Promise.all([
      loadInstallations(),
      loadPendingReservations(pendingPage),
    ]);
  }

  function changeReviewFilter(required: boolean) {
    setReviewRequired(required);
    setPage(0);
  }

  const hasNextPage = (page + 1) * PAGE_SIZE < totalElements;

  return (
    <section aria-labelledby="admin-installation-title" className="admin-installation-review">
      <div className="admin-module-heading">
        <div>
          <span className="step-label">REQUEST &amp; AI REVIEW</span>
          <h2 id="admin-installation-title">설치 요청 및 AI 결과 검토</h2>
          <p>접수 정보와 저장된 최신 분석 결과를 확인합니다. AI 결과는 참고 정보이며 자동으로 요청 상태를 바꾸지 않습니다.</p>
        </div>
        <button className="text-button" disabled={loading || pendingLoading} onClick={() => void refreshAll()} type="button">
          새로고침
        </button>
      </div>

      <div aria-label="설치 요청 검토 필터" className="admin-module-filters" role="tablist">
        <button
          aria-selected={!reviewRequired}
          className={!reviewRequired ? "active" : ""}
          onClick={() => changeReviewFilter(false)}
          role="tab"
          type="button"
        >
          전체 요청
        </button>
        <button
          aria-selected={reviewRequired}
          className={reviewRequired ? "active" : ""}
          onClick={() => changeReviewFilter(true)}
          role="tab"
          type="button"
        >
          검토 필요
        </button>
      </div>

      <section aria-labelledby="pending-assignment-title" className="pending-assignment-section">
        <div className="admin-module-heading">
          <div>
            <span className="step-label">CONFIRMED RESERVATIONS</span>
            <h3 id="pending-assignment-title">확정 예약 기사 배정</h3>
            <p>고객이 수락된 견적에서 예약 일정을 등록하면 확정 예약이 여기에 표시됩니다. 후보를 조회해 적격 기사를 직접 선택하세요.</p>
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
          <p aria-live="polite">확정 예약을 불러오는 중...</p>
        ) : pendingReservations.length === 0 ? (
          <p className="empty-state">기사 배정이 필요한 확정 예약이 없습니다.</p>
        ) : (
          <div className="pending-assignment-list">
            {pendingReservations.map((reservation) => (
              <PendingReservationAssignmentCard
                key={reservation.reservationId}
                onAssigned={async (technicianName, reservationNo, isLastOnPage) => {
                  setPendingMessage(`${reservationNo} 예약을 ${technicianName} 기사에게 배정했습니다. 기사 수락 후 예약 관리에서 확인할 수 있습니다.`);
                  setPendingPage((current) => isLastOnPage && current > 0 ? current - 1 : current);
                  await loadInstallations(false);
                  if (!isLastOnPage || pendingPage === 0) {
                    await loadPendingReservations(pendingPage);
                  }
                }}
                reservation={reservation}
                isLastOnPage={pendingReservations.length === 1}
              />
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

      {error && <p className="form-error" role="alert">{error}</p>}
      {loading ? (
        <p aria-live="polite">설치 요청을 불러오는 중...</p>
      ) : installations.length === 0 ? (
        <p className="empty-state">
          {reviewRequired ? "검토가 필요한 설치 요청이 없습니다." : "등록된 설치 요청이 없습니다."}
        </p>
      ) : (
        <div className="admin-installation-list">
          {installations.map((installation) => (
            <InstallationReviewCard
              installation={installation}
              key={installation.id}
              onAssignmentChanged={() => loadInstallations(false)}
            />
          ))}
        </div>
      )}

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
          disabled={!hasNextPage || loading}
          onClick={() => setPage((current) => current + 1)}
          type="button"
        >
          다음
        </button>
      </div>
    </section>
  );
}

interface PendingReservationAssignmentCardProps {
  reservation: PendingTechnicianReservation;
  isLastOnPage: boolean;
  onAssigned: (technicianName: string, reservationNo: string, isLastOnPage: boolean) => Promise<void>;
}

function PendingReservationAssignmentCard({
  reservation,
  isLastOnPage,
  onAssigned,
}: PendingReservationAssignmentCardProps) {
  const [candidates, setCandidates] = useState<TechnicianCandidate[]>([]);
  const [searched, setSearched] = useState(false);
  const [searching, setSearching] = useState(false);
  const [busyProfileId, setBusyProfileId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function searchCandidates() {
    setSearching(true);
    setSearched(false);
    setCandidates([]);
    setError(null);
    try {
      setCandidates(await getTechnicianCandidates(reservation.reservationId));
      setSearched(true);
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      setSearching(false);
    }
  }

  async function assign(candidate: TechnicianCandidate) {
    setBusyProfileId(candidate.technicianProfileId);
    setError(null);
    try {
      await assignTechnician(reservation.reservationId, candidate.technicianProfileId);
      await onAssigned(candidate.name, reservation.reservationNo, isLastOnPage);
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      setBusyProfileId(null);
    }
  }

  return (
    <article className="pending-assignment-card">
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
      <button
        className="secondary-button"
        disabled={searching || busyProfileId !== null}
        onClick={() => void searchCandidates()}
        type="button"
      >
        {searching ? "후보 조회 중..." : searched ? "후보 다시 조회" : "적격 기사 후보 조회"}
      </button>
      <p className="candidate-eligibility-note">
        승인된 활성 계정, 등록 자격, 요청 품목, 출발·도착 지역, 한국 표준시 작업 가능 시간 및 일정 충돌을 확인합니다.
      </p>
      {error && <p className="form-error" role="alert">{error}</p>}
      {searched && candidates.length === 0 && (
        <p className="empty-state">예약 조건에 맞는 적격 기사가 없습니다.</p>
      )}
      {candidates.map((candidate) => (
        <article className="technician-candidate-row technician-candidate-option" key={candidate.technicianProfileId}>
          <span>
            <strong>{candidate.name} · {candidate.businessName}</strong>
            <small>{candidate.phone} · 진행 중 작업 {candidate.activeAssignmentCount}건</small>
          </span>
          <button
            className="primary-button"
            disabled={busyProfileId !== null}
            onClick={() => void assign(candidate)}
            type="button"
          >
            {busyProfileId === candidate.technicianProfileId ? "배정 중..." : "이 기사 배정"}
          </button>
        </article>
      ))}
    </article>
  );
}

interface InstallationReviewCardProps {
  installation: AdminInstallationSummary;
  onAssignmentChanged: () => Promise<void>;
}

function InstallationReviewCard({ installation, onAssignmentChanged }: InstallationReviewCardProps) {
  const analysis = installation.latestAnalysis;
  const [candidates, setCandidates] = useState<TechnicianCandidate[]>([]);
  const [searched, setSearched] = useState(false);
  const [searching, setSearching] = useState(false);
  const [busyProfileId, setBusyProfileId] = useState<string | null>(null);
  const [assignmentError, setAssignmentError] = useState<string | null>(null);
  const [assignmentMessage, setAssignmentMessage] = useState<string | null>(null);
  const canAssign = installation.reservationStatus === "CONFIRMED"
    && (!installation.technicianAssignmentStatus
      || installation.technicianAssignmentStatus === "REJECTED"
      || installation.technicianAssignmentStatus === "CANCELLED");

  async function searchCandidates() {
    if (!installation.reservationId) return;
    setSearching(true);
    setSearched(false);
    setCandidates([]);
    setAssignmentError(null);
    setAssignmentMessage(null);
    try {
      setCandidates(await getTechnicianCandidates(installation.reservationId));
      setSearched(true);
    } catch (requestError) {
      setAssignmentError(toMessage(requestError));
    } finally {
      setSearching(false);
    }
  }

  async function assign(candidate: TechnicianCandidate) {
    if (!installation.reservationId) return;
    setBusyProfileId(candidate.technicianProfileId);
    setAssignmentError(null);
    setAssignmentMessage(null);
    try {
      const result = await assignTechnician(installation.reservationId, candidate.technicianProfileId);
      setCandidates([]);
      setSearched(false);
      setAssignmentMessage(`${candidate.name} 기사에게 ${result.reservationNo} 예약을 배정했습니다. 기사 수락 후 예약 관리에서 확인할 수 있습니다.`);
      await onAssignmentChanged();
    } catch (requestError) {
      setAssignmentError(toMessage(requestError));
    } finally {
      setBusyProfileId(null);
    }
  }

  return (
    <article className="admin-installation-card">
      <div className="admin-installation-card-heading">
        <div>
          <strong>{installation.requestNo}</strong>
          <span className="admin-status-badge">{formatStatus(installation.status)}</span>
        </div>
        <time dateTime={installation.createdAt}>{formatDate(installation.createdAt)}</time>
      </div>
      <div className="admin-installation-customer">
        <strong>{installation.customerName}</strong>
        <span>{installation.customerEmail}</span>
      </div>
      <dl className="admin-installation-details">
        <div><dt>제품</dt><dd>{installation.applianceSummary ?? "제품 정보 없음"}</dd></div>
        <div><dt>이전 주소</dt><dd>{installation.originAddress}</dd></div>
        <div><dt>설치 주소</dt><dd>{installation.destinationAddress}</dd></div>
        <div><dt>희망 일정</dt><dd>{formatDate(installation.desiredStartAt)}</dd></div>
      </dl>

      <RequestPhotoGallery moveRequestId={installation.id} />

      <section aria-label="예약 및 기사 배정 상태" className="admin-request-assignment">
        <div>
          <strong>예약·기사 배정</strong>
          {installation.reservationId ? (
            <span>
              {installation.reservationNo} · {formatStatus(installation.reservationStatus ?? "UNKNOWN")}
              {installation.technicianAssignmentStatus
                ? ` · ${assignmentStatusLabel(installation.technicianAssignmentStatus)}`
                : " · 기사 미배정"}
              {installation.technicianName ? ` · ${installation.technicianName} 기사` : ""}
            </span>
          ) : (
            <span>예약 확정 후 이 화면에서 적격 기사를 수동 배정할 수 있습니다.</span>
          )}
        </div>
        {canAssign && (
          <>
            <button
              className="secondary-button"
              disabled={searching || busyProfileId !== null}
              onClick={() => void searchCandidates()}
              type="button"
            >
              {searching ? "후보 조회 중..." : installation.technicianAssignmentStatus ? "다른 기사 재배정" : "기사 후보 조회"}
            </button>
            <p className="candidate-eligibility-note">
              후보는 승인·활성 계정, 등록 자격, 요청 품목, 출발·도착 지역, 한국 표준시 작업 가능 시간 및 일정 충돌 여부를 검증해 표시합니다.
            </p>
          </>
        )}
        {assignmentMessage && <p className="success-message" role="status">{assignmentMessage}</p>}
        {assignmentError && <p className="form-error" role="alert">{assignmentError}</p>}
        {searched && candidates.length === 0 && (
          <p className="empty-state">예약 조건에 맞는 기사가 없습니다.</p>
        )}
        {candidates.map((candidate) => (
          <article className="technician-candidate-row technician-candidate-option" key={candidate.technicianProfileId}>
            <span>
              <strong>{candidate.name} · {candidate.businessName}</strong>
              <small>{candidate.phone} · 진행 중 작업 {candidate.activeAssignmentCount}건</small>
            </span>
            <button
              className="primary-button"
              disabled={busyProfileId !== null}
              onClick={() => void assign(candidate)}
              type="button"
            >
              {busyProfileId === candidate.technicianProfileId ? "배정 중..." : "이 기사 배정"}
            </button>
          </article>
        ))}
      </section>

      {analysis ? (
        <details className="admin-ai-analysis">
          <summary>
            최신 AI 분석 · {formatStatus(analysis.status)}
            {analysis.confidence !== null && ` · 신뢰도 ${Math.round(analysis.confidence * 100)}%`}
          </summary>
          <dl className="admin-installation-details">
            <div><dt>제공자 / 모델</dt><dd>{analysis.provider}{analysis.modelName ? ` / ${analysis.modelName}` : ""}</dd></div>
            <div><dt>분석 시각</dt><dd>{formatDate(analysis.createdAt)}</dd></div>
            {analysis.errorCode && <div><dt>오류 코드</dt><dd>{analysis.errorCode}</dd></div>}
            {analysis.errorMessage && <div><dt>오류 설명</dt><dd>{analysis.errorMessage}</dd></div>}
          </dl>
          {analysis.resultJson ? (
            <pre>{formatJson(analysis.resultJson)}</pre>
          ) : (
            <p className="empty-state">저장된 분석 결과가 없습니다.</p>
          )}
        </details>
      ) : (
        <p className="admin-analysis-empty">저장된 AI 분석 결과가 없습니다.</p>
      )}
    </article>
  );
}

function formatStatus(status: string): string {
  const labels: Record<string, string> = {
    SUBMITTED: "접수",
    ANALYZING: "분석 중",
    NEEDS_REVIEW: "검토 필요",
    QUOTED: "견적 발행",
    RESERVED: "예약 완료",
    IN_PROGRESS: "작업 중",
    COMPLETED: "완료",
    CANCELLED: "취소",
    PENDING: "대기",
    RUNNING: "분석 중",
    SUCCEEDED: "분석 완료",
    FAILED: "분석 실패",
  };
  return labels[status] ?? status;
}

function assignmentStatusLabel(status: string): string {
  const labels: Record<string, string> = {
    PENDING: "기사 수락 대기",
    ACCEPTED: "기사 수락 완료",
    REJECTED: "기사 거절",
    ON_THE_WAY: "이동 중",
    WORK_STARTED: "작업 중",
    WORK_COMPLETED: "작업 완료",
    CANCELLED: "배정 취소",
  };
  return labels[status] ?? status;
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat("ko-KR", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function formatJson(value: string): string {
  try {
    return JSON.stringify(JSON.parse(value) as unknown, null, 2);
  } catch {
    return value;
  }
}

function toMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return "설치 요청을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.";
}
