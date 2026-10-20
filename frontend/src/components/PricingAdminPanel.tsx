import { useCallback, useEffect, useState } from "react";
import type { FormEvent } from "react";
import {
  createPriceRule,
  getPriceRules,
  getPricingRequests,
  issueQuote,
  updatePriceRule,
} from "../api/pricing";
import { getApplianceTypes } from "../api/installations";
import { ApiError } from "../api/client";
import type {
  ApplianceType,
  PriceCalculationType,
  PriceConditionCode,
  PriceRule,
  PriceRuleRequest,
  PricingRequestSummary,
} from "../types/installation";

interface PriceRuleForm {
  ruleCode: string;
  name: string;
  description: string;
  applianceTypeCode: string;
  conditionCode: PriceConditionCode;
  amount: string;
  priority: string;
  active: boolean;
  validFrom: string;
  validTo: string;
}

const INITIAL_FORM: PriceRuleForm = {
  ruleCode: "",
  name: "",
  description: "",
  applianceTypeCode: "",
  conditionCode: "BASE",
  amount: "",
  priority: "100",
  active: true,
  validFrom: "",
  validTo: "",
};

const CONDITION_LABELS: Record<PriceConditionCode, string> = {
  BASE: "기본 설치 금액",
  CARRYING_DISTANCE: "운반 거리 단가 (m당)",
  FLOOR: "작업 층수 단가 (층당)",
  ADDITIONAL_PIPING: "추가 배관 단가 (m당)",
  DISCOUNT: "할인율 (%)",
};

interface PricingAdminPanelProps {
  onClose: () => void;
}

export function PricingAdminPanel({ onClose }: PricingAdminPanelProps) {
  const [rules, setRules] = useState<PriceRule[]>([]);
  const [requests, setRequests] = useState<PricingRequestSummary[]>([]);
  const [applianceTypes, setApplianceTypes] = useState<ApplianceType[]>([]);
  const [form, setForm] = useState<PriceRuleForm>(INITIAL_FORM);
  const [editingRule, setEditingRule] = useState<PriceRule | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [busyRequestId, setBusyRequestId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [ruleList, requestPage, applianceList] = await Promise.all([
        getPriceRules(),
        getPricingRequests(),
        getApplianceTypes(),
      ]);
      setRules(ruleList);
      setRequests(requestPage.items);
      setApplianceTypes(applianceList);
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  function changeField<K extends keyof PriceRuleForm>(key: K, value: PriceRuleForm[K]) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  function editRule(rule: PriceRule) {
    setEditingRule(rule);
    setForm({
      ruleCode: rule.ruleCode,
      name: rule.name,
      description: rule.description ?? "",
      applianceTypeCode: rule.applianceTypeCode ?? "",
      conditionCode: rule.conditionCode,
      amount: String(rule.amount),
      priority: String(rule.priority),
      active: rule.active,
      validFrom: toLocalDateTime(rule.validFrom),
      validTo: toLocalDateTime(rule.validTo),
    });
    setError(null);
    setMessage(null);
    document.getElementById("price-rule-form-title")?.scrollIntoView({ behavior: "smooth", block: "start" });
  }

  function resetForm() {
    setEditingRule(null);
    setForm(INITIAL_FORM);
  }

  async function submitRule(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      const request = toRuleRequest(form);
      if (editingRule) {
        await updatePriceRule(editingRule.id, request);
        setMessage("가격 규칙을 수정하고 버전을 갱신했습니다.");
      } else {
        await createPriceRule(request);
        setMessage("가격 규칙을 등록했습니다.");
      }
      resetForm();
      await loadData();
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      setSaving(false);
    }
  }

  async function toggleRule(rule: PriceRule) {
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      await updatePriceRule(rule.id, toRuleRequest({
        ruleCode: rule.ruleCode,
        name: rule.name,
        description: rule.description ?? "",
        applianceTypeCode: rule.applianceTypeCode ?? "",
        conditionCode: rule.conditionCode,
        amount: String(rule.amount),
        priority: String(rule.priority),
        active: !rule.active,
        validFrom: toLocalDateTime(rule.validFrom),
        validTo: toLocalDateTime(rule.validTo),
      }));
      setMessage(`'${rule.name}' 규칙을 ${rule.active ? "비활성화" : "활성화"}했습니다.`);
      await loadData();
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      setSaving(false);
    }
  }

  async function createQuote(request: PricingRequestSummary) {
    setBusyRequestId(request.id);
    setError(null);
    setMessage(null);
    try {
      const quote = await issueQuote(request.id);
      setMessage(`${request.requestNo} 요청에 ${quote.quoteNo} 견적을 발행했습니다.`);
      await loadData();
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      setBusyRequestId(null);
    }
  }

  const calculationType = getCalculationType(form.conditionCode);

  return (
    <section aria-labelledby="pricing-admin-title" className="pricing-admin-panel">
      <div className="technician-profile-heading">
        <div>
          <span className="step-label">PRICING & QUOTES</span>
          <h2 id="pricing-admin-title">가격 규칙 및 견적 관리</h2>
          <p>서버 가격 규칙을 관리하고 접수 요청에 견적을 발행합니다.</p>
        </div>
        <div className="technician-heading-actions">
          <button className="text-button" disabled={loading} onClick={() => void loadData()} type="button">
            새로고침
          </button>
          <button className="text-button" onClick={onClose} type="button">기사 관리로 돌아가기</button>
        </div>
      </div>

      {error && (
        <div className="load-error" role="alert">
          <p className="inline-error">{error}</p>
          <button className="text-button" onClick={() => void loadData()} type="button">다시 시도</button>
        </div>
      )}
      {message && <p className="success-message" role="status">{message}</p>}

      <section aria-labelledby="price-rule-form-title" className="price-rule-editor">
        <div className="price-section-heading">
          <div>
            <span className="step-label">PRICE POLICY</span>
            <h3 id="price-rule-form-title">{editingRule ? "가격 규칙 수정" : "가격 규칙 등록"}</h3>
            <p>규칙 변경 시 새 버전이 저장되며, 이미 발행된 견적 금액은 유지됩니다.</p>
          </div>
          {editingRule && (
            <button className="text-button" onClick={resetForm} type="button">새 규칙 작성</button>
          )}
        </div>

        <form className="form-stack price-rule-form" onSubmit={(event) => void submitRule(event)}>
          <div className="price-rule-grid">
            <label>
              규칙 코드 <span className="required">*</span>
              <input
                maxLength={60}
                onChange={(event) => changeField("ruleCode", event.target.value.toUpperCase())}
                pattern="[A-Z][A-Z0-9_]{1,59}"
                required
                value={form.ruleCode}
              />
            </label>
            <label>
              규칙명 <span className="required">*</span>
              <input maxLength={150} onChange={(event) => changeField("name", event.target.value)} required value={form.name} />
            </label>
            <label>
              규칙 유형 <span className="required">*</span>
              <select
                onChange={(event) => changeField("conditionCode", event.target.value as PriceConditionCode)}
                value={form.conditionCode}
              >
                {Object.entries(CONDITION_LABELS).map(([code, label]) => (
                  <option key={code} value={code}>{label}</option>
                ))}
              </select>
            </label>
            <label>
              적용 가전
              <select onChange={(event) => changeField("applianceTypeCode", event.target.value)} value={form.applianceTypeCode}>
                <option value="">전체 가전</option>
                {applianceTypes.map((type) => <option key={type.code} value={type.code}>{type.name}</option>)}
              </select>
            </label>
            <label>
              금액 / 단가 <span className="required">*</span>
              <input
                min="0.01"
                onChange={(event) => changeField("amount", event.target.value)}
                required
                step="0.01"
                type="number"
                value={form.amount}
              />
              <small className="field-hint">{calculationHint(form.conditionCode)}</small>
            </label>
            <label>
              우선순위 <span className="required">*</span>
              <input
                max={10000}
                min={-10000}
                onChange={(event) => changeField("priority", event.target.value)}
                required
                type="number"
                value={form.priority}
              />
            </label>
            <label>
              적용 시작
              <input onChange={(event) => changeField("validFrom", event.target.value)} type="datetime-local" value={form.validFrom} />
            </label>
            <label>
              적용 종료
              <input onChange={(event) => changeField("validTo", event.target.value)} type="datetime-local" value={form.validTo} />
            </label>
          </div>
          <label>
            규칙 설명
            <textarea
              maxLength={2000}
              onChange={(event) => changeField("description", event.target.value)}
              rows={2}
              value={form.description}
            />
          </label>
          <label className="price-rule-active">
            <input checked={form.active} onChange={(event) => changeField("active", event.target.checked)} type="checkbox" />
            견적 계산에 활성 규칙으로 사용
          </label>
          <div className="price-form-actions">
            <button className="primary-button" disabled={saving} type="submit">
              {saving ? "저장 중..." : editingRule ? "규칙 수정 저장" : "가격 규칙 등록"}
            </button>
            {editingRule && <button className="secondary-button" disabled={saving} onClick={resetForm} type="button">취소</button>}
          </div>
        </form>
      </section>

      <section aria-labelledby="price-rules-title" className="price-rules-section">
        <div className="price-section-heading">
          <div>
            <span className="step-label">RULES</span>
            <h3 id="price-rules-title">등록된 가격 규칙</h3>
          </div>
        </div>
        {loading ? (
          <p aria-live="polite" className="history-loading">가격 규칙을 불러오는 중...</p>
        ) : rules.length === 0 ? (
          <p className="empty-state">가격 규칙이 없습니다. 각 가전의 기본 설치 금액 규칙을 먼저 등록해 주세요.</p>
        ) : (
          <div className="price-rule-list">
            {rules.map((rule) => (
              <article className="price-rule-card" key={rule.id}>
                <div className="price-rule-summary">
                  <span className={`request-status ${rule.active ? "status-quoted" : "status-cancelled"}`}>
                    {rule.active ? "활성" : "비활성"}
                  </span>
                  <strong>{rule.name}</strong>
                  <small>{rule.ruleCode} · v{rule.version} · {rule.applianceTypeCode ?? "전체 가전"}</small>
                  <small>{CONDITION_LABELS[rule.conditionCode]} · {formatCurrency(rule.amount)}</small>
                </div>
                <div className="price-rule-actions">
                  <button className="text-button" disabled={saving} onClick={() => editRule(rule)} type="button">수정</button>
                  <button className="text-button" disabled={saving} onClick={() => void toggleRule(rule)} type="button">
                    {rule.active ? "비활성화" : "활성화"}
                  </button>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>

      <section aria-labelledby="pricing-requests-title" className="pricing-requests-section">
        <div className="price-section-heading">
          <div>
            <span className="step-label">QUOTE QUEUE</span>
            <h3 id="pricing-requests-title">견적 발행 대기 요청</h3>
            <p>기본 규칙이 등록된 가전 요청에 견적을 발행하거나 재산출할 수 있습니다.</p>
          </div>
        </div>
        {loading ? (
          <p aria-live="polite" className="history-loading">견적 대상을 불러오는 중...</p>
        ) : requests.length === 0 ? (
          <p className="empty-state">현재 견적 발행 대상 요청이 없습니다.</p>
        ) : (
          <div className="pricing-request-list">
            {requests.map((request) => (
              <article className="pricing-request-card" key={request.id}>
                <div>
                  <div className="history-item-heading">
                    <strong>{request.applianceTypeName} · {request.requestNo}</strong>
                    <span>{request.latestQuoteStatus
                      ? `${quoteStatusLabel(request.latestQuoteStatus)}${request.latestQuoteTotalAmount !== null
                        ? ` · ${formatCurrency(request.latestQuoteTotalAmount)}` : ""}`
                      : "견적 미발행"}</span>
                  </div>
                  <small>{request.customerName} · {request.customerEmail}</small>
                  <small>{request.originAddress} → {request.destinationAddress}</small>
                  <small>희망 일정 {formatDate(request.desiredStartAt)}</small>
                </div>
                <button
                  className="primary-button"
                  disabled={busyRequestId !== null}
                  onClick={() => void createQuote(request)}
                  type="button"
                >
                  {busyRequestId === request.id ? "산출 중..." : request.latestQuoteStatus ? "견적 재산출" : "견적 발행"}
                </button>
              </article>
            ))}
          </div>
        )}
      </section>
    </section>
  );
}

function toRuleRequest(form: PriceRuleForm): PriceRuleRequest {
  return {
    ruleCode: form.ruleCode.trim(),
    name: form.name.trim(),
    description: form.description.trim() || null,
    applianceTypeCode: form.applianceTypeCode || null,
    conditionCode: form.conditionCode,
    calculationType: getCalculationType(form.conditionCode),
    amount: Number(form.amount),
    priority: Number(form.priority),
    active: form.active,
    validFrom: form.validFrom ? new Date(form.validFrom).toISOString() : null,
    validTo: form.validTo ? new Date(form.validTo).toISOString() : null,
  };
}

function getCalculationType(conditionCode: PriceConditionCode): PriceCalculationType {
  if (conditionCode === "BASE") return "FIXED";
  if (conditionCode === "DISCOUNT") return "PERCENTAGE";
  return "PER_UNIT";
}

function calculationHint(conditionCode: PriceConditionCode): string {
  if (conditionCode === "BASE") return "가전 1대당 고정 금액 (원)";
  if (conditionCode === "DISCOUNT") return "소계에 적용할 할인율 (%)";
  if (conditionCode === "FLOOR") return "작업 층수 1층당 금액 (원)";
  if (conditionCode === "CARRYING_DISTANCE") return "운반 거리 1m당 금액 (원)";
  return "추가 배관 1m당 금액 (원)";
}

function formatCurrency(amount: number): string {
  return new Intl.NumberFormat("ko-KR", { style: "currency", currency: "KRW", maximumFractionDigits: 0 })
    .format(amount);
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat("ko-KR", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}

function toLocalDateTime(value: string | null): string {
  if (!value) return "";
  const date = new Date(value);
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
}

function quoteStatusLabel(status: string): string {
  if (status === "ISSUED") return "수락 대기";
  if (status === "EXPIRED") return "만료";
  if (status === "ACCEPTED") return "수락 완료";
  return status;
}

function toMessage(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  if (error instanceof Error) return error.message;
  return "가격 규칙 또는 견적 요청을 처리하지 못했습니다.";
}
