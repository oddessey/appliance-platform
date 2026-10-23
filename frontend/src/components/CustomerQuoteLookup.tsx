import { useCallback, useEffect, useState } from "react";
import { getInstallations } from "../api/installations";
import { acceptQuote, getCustomerQuotes } from "../api/pricing";
import { ApiError } from "../api/client";
import { CustomerReservations } from "./CustomerReservations";
import { QuoteAcceptanceDialog } from "./QuoteAcceptanceDialog";
import type { InstallationHistoryItem, Quote } from "../types/installation";

interface CustomerQuoteLookupProps {
  refreshKey: number;
  onViewHistory: () => void;
}

interface QuoteLookupEntry {
  request: InstallationHistoryItem;
  quote: Quote | null;
  quoteError: string | null;
}

interface QuoteToAccept {
  request: InstallationHistoryItem;
  quote: Quote;
}

const LOOKUP_PAGE_SIZE = 10;

export function CustomerQuoteLookup({ refreshKey, onViewHistory }: CustomerQuoteLookupProps) {
  const [entries, setEntries] = useState<QuoteLookupEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [retryCount, setRetryCount] = useState(0);
  const [quoteToAccept, setQuoteToAccept] = useState<QuoteToAccept | null>(null);
  const [busyQuoteId, setBusyQuoteId] = useState<string | null>(null);
  const [acceptError, setAcceptError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const loadQuotes = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const page = await getInstallations(0, LOOKUP_PAGE_SIZE);
      const rows = await Promise.all(page.content.map(async (request) => {
        try {
          const quotePage = await getCustomerQuotes(request.id, 0, 1);
          return { request, quote: quotePage.items[0] ?? null, quoteError: null };
        } catch (quoteError) {
          return { request, quote: null, quoteError: toMessage(quoteError) };
        }
      }));
      setEntries(rows);
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadQuotes();
  }, [loadQuotes, refreshKey, retryCount]);

  async function accept() {
    if (!quoteToAccept) return;
    const { request, quote } = quoteToAccept;
    setBusyQuoteId(quote.id);
    setAcceptError(null);
    setMessage(null);
    try {
      await acceptQuote(quote.id);
      setQuoteToAccept(null);
      setMessage(`${request.requestNo} 견적 금액에 동의했습니다. 아래에서 희망 일정을 선택해 예약을 진행해 주세요.`);
      await loadQuotes();
    } catch (requestError) {
      setAcceptError(toMessage(requestError));
    } finally {
      setBusyQuoteId(null);
    }
  }

  return (
    <section aria-labelledby="customer-quote-lookup-title" className="customer-quote-lookup">
      <div className="customer-quote-lookup-heading">
        <div>
          <span className="step-label">MY QUOTES</span>
          <h2 id="customer-quote-lookup-title">가격 견적 조회</h2>
          <p>접수한 설치 요청의 최신 견적 금액과 상태를 확인할 수 있습니다.</p>
        </div>
        <div className="customer-quote-lookup-actions">
          <button className="text-button" disabled={loading} onClick={() => void loadQuotes()} type="button">
            새로고침
          </button>
          <button className="text-button" onClick={onViewHistory} type="button">
            접수 내역 전체 보기
          </button>
        </div>
      </div>

      {message && <p className="success-message" role="status">{message}</p>}

      {loading ? (
        <p aria-live="polite" className="history-loading">가격 견적을 불러오는 중...</p>
      ) : error ? (
        <div className="load-error" role="alert">
          <p className="inline-error">{error}</p>
          <button className="text-button" onClick={() => setRetryCount((count) => count + 1)} type="button">
            다시 불러오기
          </button>
        </div>
      ) : entries.length === 0 ? (
        <p className="empty-state">
          아직 접수된 설치 요청이 없습니다. 설치 요청을 접수하면 발행된 견적 금액을 이곳에서 확인할 수 있습니다.
        </p>
      ) : (
        <div className="customer-quote-lookup-list">
          {entries.map(({ request, quote, quoteError }) => (
            <article className="customer-quote-lookup-card" key={request.id}>
              <div className="customer-quote-lookup-card-heading">
                <div>
                  <span className="request-number">{request.requestNo}</span>
                  <strong>{request.applianceTypeName} 이전설치</strong>
                  <small>
                    희망 일정 {formatDateTime(request.desiredStartAt)} · {formatStatus(request.status)}
                  </small>
                </div>
                {quote ? (
                  <div className="customer-quote-lookup-amount">
                    <strong>{formatCurrency(quote.totalAmount)}</strong>
                    <small>{quoteStatusLabel(quote.status)} · {quote.versionNo}차 견적</small>
                  </div>
                ) : (
                  <span className="customer-quote-lookup-waiting">
                    {quoteError ? "견적 조회 실패" : "견적 준비 중"}
                  </span>
                )}
              </div>
              {quote && (
                <dl className="quote-totals">
                  <div><dt>소계</dt><dd>{formatCurrency(quote.subtotalAmount)}</dd></div>
                  <div><dt>할인</dt><dd>-{formatCurrency(quote.discountAmount)}</dd></div>
                  <div><dt>세금</dt><dd>{formatCurrency(quote.taxAmount)}</dd></div>
                  <div className="quote-total"><dt>합계</dt><dd>{formatCurrency(quote.totalAmount)}</dd></div>
                </dl>
              )}
              {quoteError && <p className="inline-error">{quoteError}</p>}
              {quote?.status === "ISSUED" && (
                <button
                  className="primary-button"
                  disabled={busyQuoteId !== null}
                  onClick={() => {
                    setQuoteToAccept({ request, quote });
                    setAcceptError(null);
                    setMessage(null);
                  }}
                  type="button"
                >
                  {busyQuoteId === quote.id ? "처리 중..." : "견적 금액 확인 후 동의"}
                </button>
              )}
              {quote?.status === "ACCEPTED" && (
                <CustomerReservations requestId={request.id} quoteId={quote.id} />
              )}
            </article>
          ))}
        </div>
      )}
      {quoteToAccept && (
        <QuoteAcceptanceDialog
          busy={busyQuoteId !== null}
          error={acceptError}
          onCancel={() => setQuoteToAccept(null)}
          onConfirm={() => void accept()}
          quote={quoteToAccept.quote}
        />
      )}
      <p className="customer-quote-lookup-note">
        견적 금액에 동의하면 희망 일정을 선택해 예약을 진행할 수 있습니다. 견적 동의만으로 예약이 확정되거나 결제가 진행되지는 않습니다.
      </p>
    </section>
  );
}

function formatCurrency(value: number): string {
  return new Intl.NumberFormat("ko-KR", {
    style: "currency",
    currency: "KRW",
    maximumFractionDigits: 0,
  }).format(value);
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

function formatStatus(status: string): string {
  const labels: Record<string, string> = {
    SUBMITTED: "접수 완료",
    DRAFT: "작성 중",
    ANALYZING: "요청 분석 중",
    NEEDS_REVIEW: "확인 필요",
    QUOTED: "견적 완료",
    RESERVED: "예약 완료",
    IN_PROGRESS: "작업 중",
    COMPLETED: "설치 완료",
    CANCELLED: "취소",
  };
  return labels[status] ?? status;
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
  return "가격 견적을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.";
}
