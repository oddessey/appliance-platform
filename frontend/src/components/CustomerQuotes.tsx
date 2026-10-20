import { useEffect, useState } from "react";
import { acceptQuote, getCustomerQuotes } from "../api/pricing";
import { CustomerReservations } from "./CustomerReservations";
import { ApiError } from "../api/client";
import type { Quote } from "../types/installation";

interface CustomerQuotesProps {
  requestId: string;
}

export function CustomerQuotes({ requestId }: CustomerQuotesProps) {
  const [open, setOpen] = useState(false);
  const [quotes, setQuotes] = useState<Quote[]>([]);
  const [loading, setLoading] = useState(false);
  const [busyQuoteId, setBusyQuoteId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  async function loadQuotes() {
    setLoading(true);
    setError(null);
    try {
      const response = await getCustomerQuotes(requestId);
      setQuotes(response.items);
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (open) void loadQuotes();
  }, [open, requestId]);

  async function accept(quote: Quote) {
    setBusyQuoteId(quote.id);
    setError(null);
    setMessage(null);
    try {
      await acceptQuote(quote.id);
      setMessage("견적을 수락했습니다. 예약은 별도로 진행해야 합니다.");
      await loadQuotes();
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      setBusyQuoteId(null);
    }
  }

  const acceptedQuote = quotes.find((quote) => quote.status === "ACCEPTED");

  return (
    <div className="customer-quotes">
      <button
        aria-expanded={open}
        className="text-button"
        onClick={() => setOpen((current) => !current)}
        type="button"
      >
        {open ? "견적 접기" : "견적·금액 확인"}
      </button>
      {open && (
        <div className="customer-quote-list">
          {message && <p className="success-message" role="status">{message}</p>}
          {error && (
            <div className="load-error" role="alert">
              <p className="inline-error">{error}</p>
              <button className="text-button" onClick={() => void loadQuotes()} type="button">다시 시도</button>
            </div>
          )}
          {loading ? (
            <p aria-live="polite" className="history-loading">견적을 불러오는 중...</p>
          ) : quotes.length === 0 && !error ? (
            <p className="empty-state">관리자가 견적을 발행하면 이곳에서 금액을 확인할 수 있습니다.</p>
          ) : quotes.map((quote, index) => (
            <article className="customer-quote-card" key={quote.id}>
              <div className="customer-quote-heading">
                <div>
                  <strong>{quote.quoteNo} · {quote.versionNo}차 견적</strong>
                  <small>{quoteStatusLabel(quote.status)} · 만료 {formatDate(quote.expiresAt)}</small>
                </div>
                <strong>{formatCurrency(quote.totalAmount)}</strong>
              </div>
              <div className="quote-lines">
                {quote.lines.map((line, lineIndex) => (
                  <div className="quote-line" key={`${line.lineCode}-${lineIndex}`}>
                    <span>{line.description} · {line.quantity}</span>
                    <span>{formatCurrency(line.lineAmount)}</span>
                  </div>
                ))}
              </div>
              <dl className="quote-totals">
                <div><dt>소계</dt><dd>{formatCurrency(quote.subtotalAmount)}</dd></div>
                <div><dt>할인</dt><dd>-{formatCurrency(quote.discountAmount)}</dd></div>
                <div><dt>세금</dt><dd>{formatCurrency(quote.taxAmount)}</dd></div>
                <div className="quote-total"><dt>합계</dt><dd>{formatCurrency(quote.totalAmount)}</dd></div>
              </dl>
              {quote.status === "ISSUED" && index === 0 && (
                <button
                  className="primary-button"
                  disabled={busyQuoteId !== null || loading}
                  onClick={() => void accept(quote)}
                  type="button"
                >
                  {busyQuoteId === quote.id ? "처리 중..." : "견적 수락"}
                </button>
              )}
            </article>
          ))}
          {acceptedQuote && (
            <CustomerReservations
              requestId={requestId}
              quoteId={acceptedQuote.id}
            />
          )}
        </div>
      )}
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

function formatDate(value: string | null): string {
  if (!value) return "미정";
  return new Intl.DateTimeFormat("ko-KR", { dateStyle: "medium", timeStyle: "short" })
    .format(new Date(value));
}

function quoteStatusLabel(status: Quote["status"]): string {
  const labels: Record<Quote["status"], string> = {
    DRAFT: "작성 중",
    ISSUED: "수락 대기",
    ACCEPTED: "수락 완료",
    EXPIRED: "만료",
    CANCELLED: "취소",
  };
  return labels[status];
}

function toMessage(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  if (error instanceof Error) return error.message;
  return "견적 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.";
}
