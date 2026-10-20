import { useCallback, useEffect, useState } from "react";
import { PricingAdminPanel } from "./PricingAdminPanel";
import { ReservationAdminPanel } from "./ReservationAdminPanel";
import {
  getAdminTechnicians,
  updateTechnicianApproval,
} from "../api/installations";
import type {
  TechnicianAdminSummary,
  TechnicianApprovalStatus,
} from "../types/installation";

const APPROVAL_FILTERS: Array<{ label: string; value?: TechnicianApprovalStatus }> = [
  { label: "전체" },
  { label: "승인 대기", value: "PENDING" },
  { label: "승인", value: "APPROVED" },
  { label: "반려", value: "REJECTED" },
];

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

  useEffect(() => {
    void loadTechnicians();
  }, [loadTechnicians]);

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
          <p>프로필을 검토하고 확정 예약에 적격한 기사를 배정합니다.</p>
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

      <section aria-labelledby="technician-assignment-flow-title" className="technician-candidate-search">
        <h3 id="technician-assignment-flow-title">기사 배정 처리 순서</h3>
        <ol className="technician-assignment-flow">
          <li>요청·AI 검토 탭에서 확정 예약의 적격 후보를 확인하고 기사를 수동 배정합니다.</li>
          <li>기사가 작업 배정을 수락하면 상태가 예약 관리에 반영됩니다.</li>
          <li>예약 관리에서 기사 수락 완료 건을 확인하고 일정 변경 등 후속 업무를 처리합니다.</li>
        </ol>
        <button className="secondary-button" onClick={onReviewRequests} type="button">
          요청·AI 검토로 이동
        </button>
      </section>
    </section>
  );
}

function approvalLabel(status: TechnicianApprovalStatus): string {
  const labels = { PENDING: "승인 대기", APPROVED: "승인", REJECTED: "반려" };
  return labels[status];
}

function toMessage(error: unknown): string {
  if (error instanceof Error) return error.message;
  return "기사 관리 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.";
}
