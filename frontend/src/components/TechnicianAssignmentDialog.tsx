import { useEffect, useState } from "react";
import { assignTechnician, getTechnicianCandidates } from "../api/installations";
import { ApiError } from "../api/client";
import type { TechnicianCandidate } from "../types/installation";

interface TechnicianAssignmentDialogProps {
  reservationId: string;
  reservationNo: string;
  scheduleLabel: string;
  addressesLabel: string;
  reassign: boolean;
  onAssigned: (technicianName: string) => void | Promise<void>;
  onClose: () => void;
}

export function TechnicianAssignmentDialog({
  reservationId,
  reservationNo,
  scheduleLabel,
  addressesLabel,
  reassign,
  onAssigned,
  onClose,
}: TechnicianAssignmentDialogProps) {
  const [candidates, setCandidates] = useState<TechnicianCandidate[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [assigning, setAssigning] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    getTechnicianCandidates(reservationId)
      .then((result) => {
        if (!cancelled) setCandidates(result);
      })
      .catch((requestError) => {
        if (!cancelled) setError(toMessage(requestError));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [reservationId]);

  async function confirmAssignment() {
    const candidate = candidates.find((item) => item.technicianProfileId === selectedId);
    if (!candidate || assigning) return;
    setAssigning(true);
    setError(null);
    try {
      await assignTechnician(reservationId, candidate.technicianProfileId);
      await onAssigned(candidate.name);
      return;
    } catch (requestError) {
      setError(toMessage(requestError));
    }
    setAssigning(false);
  }

  return (
    <div className="popup-backdrop">
      <section
        aria-labelledby="technician-assignment-title"
        aria-modal="true"
        className="popup-card assignment-dialog"
        role="dialog"
      >
        <div className="popup-heading">
          <h2 id="technician-assignment-title">{reassign ? "기사 재배정" : "기사 배정"}</h2>
          <button
            aria-label="기사 배정 닫기"
            className="text-button"
            disabled={assigning}
            onClick={onClose}
            type="button"
          >
            닫기
          </button>
        </div>
        <div className="assignment-dialog-summary">
          <strong>{reservationNo}</strong>
          <span>{scheduleLabel}</span>
          <span>{addressesLabel}</span>
        </div>
        <p className="candidate-eligibility-note">
          후보는 승인·활성 계정, 등록 자격, 요청 품목, 출발·도착 지역, 한국 표준시 작업 가능 시간 및 일정 충돌 여부를 검증해 표시합니다.
        </p>
        {loading ? (
          <p aria-live="polite">적격 기사 후보를 조회하는 중...</p>
        ) : candidates.length === 0 && !error ? (
          <p className="empty-state">예약 조건에 맞는 적격 기사가 없습니다.</p>
        ) : (
          candidates.map((candidate) => (
            <article
              className={`technician-candidate-row technician-candidate-option${
                selectedId === candidate.technicianProfileId ? " selected" : ""
              }`}
              key={candidate.technicianProfileId}
            >
              <label className="assignment-dialog-candidate">
                <input
                  checked={selectedId === candidate.technicianProfileId}
                  disabled={assigning}
                  name="technician-candidate"
                  onChange={() => setSelectedId(candidate.technicianProfileId)}
                  type="radio"
                />
                <span>
                  <strong>{candidate.name} · {candidate.businessName}</strong>
                  <small>{candidate.phone} · 진행 중 작업 {candidate.activeAssignmentCount}건</small>
                </span>
              </label>
            </article>
          ))
        )}
        {error && <p className="inline-error" role="alert">{error}</p>}
        <div className="quote-acceptance-actions">
          <button className="secondary-button" disabled={assigning} onClick={onClose} type="button">
            취소
          </button>
          <button
            className="primary-button"
            disabled={!selectedId || assigning || loading}
            onClick={() => void confirmAssignment()}
            type="button"
          >
            {assigning ? "배정 중..." : reassign ? "선택한 기사로 재배정" : "선택한 기사에게 배정"}
          </button>
        </div>
      </section>
    </div>
  );
}

function toMessage(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  if (error instanceof Error) return error.message;
  return "기사 후보를 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.";
}
