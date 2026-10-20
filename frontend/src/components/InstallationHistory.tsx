import { useCallback, useEffect, useState } from "react";
import { getInstallations } from "../api/installations";
import { CustomerQuotes } from "./CustomerQuotes";
import { ApiError } from "../api/client";
import type { InstallationHistoryItem } from "../types/installation";

interface InstallationHistoryProps {
  onCreateAnother: () => void;
}

export function InstallationHistory({ onCreateAnother }: InstallationHistoryProps) {
  const [items, setItems] = useState<InstallationHistoryItem[]>([]);
  const [page, setPage] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadPage = useCallback(async (pageToLoad: number) => {
    setLoading(true);
    setError(null);
    try {
      const result = await getInstallations(pageToLoad);
      setItems(result.content);
      setPage(result.page);
      setTotalElements(result.totalElements);
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadPage(0);
  }, [loadPage]);

  return (
    <section className="installation-history" aria-labelledby="installation-history-title">
      <div className="history-heading">
        <div>
          <span className="step-label">MY REQUESTS</span>
          <h2 id="installation-history-title">설치 요청 접수 내역</h2>
          <p>접수된 설치 요청과 희망 일정을 확인할 수 있습니다.</p>
        </div>
        <button className="address-search-button" onClick={onCreateAnother} type="button">
          추가 접수하기
        </button>
      </div>

      {loading ? (
        <p aria-live="polite" className="history-loading">접수 내역을 불러오는 중...</p>
      ) : error ? (
        <div className="load-error" role="alert">
          <p className="inline-error">{error}</p>
          <button className="text-button" onClick={() => void loadPage(page)} type="button">
            다시 불러오기
          </button>
        </div>
      ) : items.length === 0 ? (
        <div className="empty-state history-empty">
          <p>아직 접수된 설치 요청이 없습니다.</p>
          <button className="text-button" onClick={onCreateAnother} type="button">
            설치 요청 접수하기
          </button>
        </div>
      ) : (
        <>
          <div className="history-list">
            {items.map((item) => (
              <article className="history-item" key={item.id}>
                <div className="history-item-heading">
                  <div>
                    <span className={`request-status status-${item.status.toLowerCase()}`}>{formatStatus(item.status)}</span>
                    <h3>{item.applianceTypeName} 이전설치</h3>
                  </div>
                  <span className="request-number">{item.requestNo}</span>
                </div>
                <dl className="history-details">
                  <div>
                    <dt>이전 주소</dt>
                    <dd>{item.originAddress}</dd>
                  </div>
                  <div>
                    <dt>설치 주소</dt>
                    <dd>{item.destinationAddress}</dd>
                  </div>
                  <div>
                    <dt>희망 일정</dt>
                    <dd>{formatDateTime(item.desiredStartAt)}</dd>
                  </div>
                  <div>
                    <dt>접수 일시</dt>
                    <dd>{formatDateTime(item.createdAt)}</dd>
                  </div>
                </dl>
                <CustomerQuotes requestId={item.id} />
              </article>
            ))}
          </div>
          <div className="admin-pagination">
            <button
              className="text-button"
              disabled={page === 0 || loading}
              onClick={() => void loadPage(page - 1)}
              type="button"
            >
              이전
            </button>
            <span>{page + 1} 페이지 · 전체 {totalElements}건</span>
            <button
              className="text-button"
              disabled={(page + 1) * 20 >= totalElements || loading}
              onClick={() => void loadPage(page + 1)}
              type="button"
            >
              다음
            </button>
          </div>
        </>
      )}
    </section>
  );
}

function formatDateTime(value: string): string {
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
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

function toMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return "접수 내역을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.";
}
