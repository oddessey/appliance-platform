interface AssignmentCancelDialogProps {
  reservationNo: string;
  technicianName: string | null;
  statusLabel: string;
  busy: boolean;
  error: string | null;
  onClose: () => void;
  onConfirm: () => void;
}

export function AssignmentCancelDialog({
  reservationNo,
  technicianName,
  statusLabel,
  busy,
  error,
  onClose,
  onConfirm,
}: AssignmentCancelDialogProps) {
  return (
    <div className="popup-backdrop">
      <section
        aria-labelledby="assignment-cancel-title"
        aria-modal="true"
        className="popup-card assignment-cancel-dialog"
        role="dialog"
      >
        <div className="popup-heading">
          <h2 id="assignment-cancel-title">기사 배정 취소</h2>
          <button
            aria-label="배정 취소 닫기"
            className="text-button"
            disabled={busy}
            onClick={onClose}
            type="button"
          >
            닫기
          </button>
        </div>
        <div className="assignment-dialog-summary">
          <strong>{reservationNo}</strong>
          <span>
            {technicianName ? `${technicianName} 기사` : "담당 기사"}
            {statusLabel ? ` · ${statusLabel}` : ""}
          </span>
        </div>
        <p className="quote-acceptance-note">
          이 배정을 취소하면 기사에게 더 이상 작업이 전달되지 않으며, 예약은 확정 상태로 유지되어 재배정할 수 있습니다.
          작업이 이미 시작된 배정은 취소할 수 없습니다.
        </p>
        {error && <p className="inline-error" role="alert">{error}</p>}
        <div className="quote-acceptance-actions">
          <button className="secondary-button" disabled={busy} onClick={onClose} type="button">
            닫기
          </button>
          <button className="text-button reservation-cancel" disabled={busy} onClick={onConfirm} type="button">
            {busy ? "취소 중..." : "기사 배정 취소"}
          </button>
        </div>
      </section>
    </div>
  );
}
