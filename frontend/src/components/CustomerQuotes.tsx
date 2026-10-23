import { useCallback, useEffect, useState } from "react";
import { acceptQuote, getCustomerQuotes } from "../api/pricing";
import { CustomerReservations } from "./CustomerReservations";
import { QuoteAcceptanceDialog } from "./QuoteAcceptanceDialog";
import { ApiError } from "../api/client";
import type { Quote } from "../types/installation";

interface CustomerQuotesProps {
  requestId: string;
}

export function CustomerQuotes({ requestId }: CustomerQuotesProps) {
  const [open, setOpen] = useState(false);
  const [quotes, setQuotes] = useState<Quote[]>([]);
  const [loading, setLoading] = useState(true);
  const [busyQuoteId, setBusyQuoteId] = useState<string | null>(null);
  const [quoteToAccept, setQuoteToAccept] = useState<Quote | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const loadQuotes = useCallback(async (showLoading = true) => {
    if (showLoading) setLoading(true);
    setError(null);
    try {
      const response = await getCustomerQuotes(requestId);
      setQuotes(response.items);
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      if (showLoading) setLoading(false);
    }
  }, [requestId]);

  useEffect(() => {
    void loadQuotes();
  }, [loadQuotes]);

  useEffect(() => {
    if (!open) return;
    void loadQuotes(false);

    function refreshQuotes() {
      if (document.visibilityState === "visible") {
        void loadQuotes(false);
      }
    }

    const intervalId = window.setInterval(refreshQuotes, 15_000);
    window.addEventListener("focus", refreshQuotes);
    document.addEventListener("visibilitychange", refreshQuotes);
    return () => {
      window.clearInterval(intervalId);
      window.removeEventListener("focus", refreshQuotes);
      document.removeEventListener("visibilitychange", refreshQuotes);
    };
  }, [open, loadQuotes]);

  async function accept() {
    if (!quoteToAccept) return;
    const quote = quoteToAccept;
    setBusyQuoteId(quote.id);
    setError(null);
    setMessage(null);
    try {
      await acceptQuote(quote.id);
      setQuoteToAccept(null);
      setMessage("견적 금액에 동의했습니다. 아래에서 희망 일정을 선택해 예약을 진행해 주세요.");
      await loadQuotes();
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      setBusyQuoteId(null);
    }
  }

  const acceptedQuote = quotes.find((quote) => quote.status === "ACCEPTED");
  const latestQuote = quotes[0];

  return (
    <div className="customer-quotes">
      <div className="customer-quotes-heading">
        <button
          aria-expanded={open}
          className="text-button"
          onClick={() => setOpen((current) => !current)}
          type="button"
        >
          {open ? "견적 접기" : "견적·금액 확인"}
        </button>
        {!open && latestQuote && !loading && (
          <span className="customer-quote-summary">
            최신 견적 {formatCurrency(latestQuote.totalAmount)} · {quoteStatusLabel(latestQuote.status)}
          </span>
        )}
      </div>
      {open && (
        <div className="customer-quote-list">
          <ol aria-label="견적 확인 절차" className="quote-process">
            <li className={quotes.length > 0 ? "is-complete" : "is-current"}>
              <span>1</span>
              <span>견적 발행</span>
            </li>
            <li className={acceptedQuote ? "is-complete" : latestQuote?.status === "ISSUED" ? "is-current" : ""}>
              <span>2</span>
              <span>금액 확인 및 동의</span>
            </li>
            <li className={acceptedQuote ? "is-current" : ""}>
              <span>3</span>
              <span>예약 일정 선택</span>
            </li>
          </ol>
          {message && <p className="success-message" role="status">{message}</p>}
          {error && !quoteToAccept && (
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
                  onClick={() => {
                    setQuoteToAccept(quote);
                    setError(null);
                    setMessage(null);
                  }}
                  type="button"
                >
                  견적 금액 확인 후 동의
                </button>
              )}
            </article>
          ))}
          {quoteToAccept && (
            <QuoteAcceptanceDialog
              busy={busyQuoteId !== null}
              error={error}
              onCancel={() => setQuoteToAccept(null)}
              onConfirm={() => void accept()}
              quote={quoteToAccept}
            />
          )}
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
