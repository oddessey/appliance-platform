import { useState } from "react";
import type { Quote } from "../types/installation";

interface QuoteAcceptanceDialogProps {
  quote: Quote;
  busy: boolean;
  error: string | null;
  onCancel: () => void;
  onConfirm: () => void;
}

export function QuoteAcceptanceDialog({ quote, busy, error, onCancel, onConfirm }: QuoteAcceptanceDialogProps) {
  const [confirmed, setConfirmed] = useState(false);

  return (
    <div className="popup-backdrop">
      <section
        aria-labelledby="quote-acceptance-title"
        aria-modal="true"
        className="popup-card quote-acceptance-dialog"
        role="dialog"
      >
        <div className="popup-heading">
          <h2 id="quote-acceptance-title">견적 금액 확인</h2>
          <button
            aria-label="견적 확인 닫기"
            className="text-button"
            disabled={busy}
            onClick={onCancel}
            type="button"
          >
            닫기
          </button>
        </div>
        <p>{quote.quoteNo} · {quote.versionNo}차 견적의 최종 금액입니다.</p>
        <div className="quote-acceptance-total">
          <span>동의할 최종 견적 금액</span>
          <strong>{formatCurrency(quote.totalAmount)}</strong>
        </div>
        <p className="quote-acceptance-note">
          견적 동의 후 예약 일정 선택 단계로 이동합니다. 이 단계에서 예약이 자동 확정되거나 결제가 진행되지는 않습니다.
        </p>
        <label className="quote-acceptance-consent">
          <input
            checked={confirmed}
            disabled={busy}
            onChange={(event) => setConfirmed(event.target.checked)}
            type="checkbox"
          />
          위 견적의 항목별 내역과 최종 금액을 확인했으며, 해당 금액에 동의합니다.
        </label>
        {error && <p className="inline-error" role="alert">{error}</p>}
        <div className="quote-acceptance-actions">
          <button
            className="secondary-button"
            disabled={busy}
            onClick={onCancel}
            type="button"
          >
            취소
          </button>
          <button
            className="primary-button"
            disabled={!confirmed || busy}
            onClick={onConfirm}
            type="button"
          >
            {busy ? "처리 중..." : "견적 금액에 동의"}
          </button>
        </div>
      </section>
    </div>
  );
}

function formatCurrency(value: number): string {
  return new Intl.NumberFormat("ko-KR", {
    style: "currency",
    currency: "KRW",
    maximumFractionDigits: 0,
  }).format(value);
}
