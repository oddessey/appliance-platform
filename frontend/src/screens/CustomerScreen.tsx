import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { createInstallation, getApplianceTypes } from "../api/installations";
import { ApiError } from "../api/client";
import { AddressSearchInput } from "../components/AddressSearchInput";
import { DesiredDateTimeInput } from "../components/DesiredDateTimeInput";
import { InstallationHistory } from "../components/InstallationHistory";
import type { ApplianceType, InstallationResponse } from "../types/installation";

interface CustomerScreenProps {
  showHistory: boolean;
  onViewHistory: () => void;
  onViewHome: () => void;
}

export function CustomerScreen({ showHistory, onViewHistory, onViewHome }: CustomerScreenProps) {
  const [applianceTypes, setApplianceTypes] = useState<ApplianceType[]>([]);
  const [typesLoading, setTypesLoading] = useState(true);
  const [typesError, setTypesError] = useState<string | null>(null);
  const [retryCount, setRetryCount] = useState(0);
  const [requestLoading, setRequestLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<InstallationResponse | null>(null);
  const [showAdditionalPrompt, setShowAdditionalPrompt] = useState(false);
  const [formGeneration, setFormGeneration] = useState(0);

  useEffect(() => {
    let active = true;
    setTypesLoading(true);
    setTypesError(null);
    getApplianceTypes()
      .then((types) => {
        if (active) setApplianceTypes(types);
      })
      .catch((requestError: unknown) => {
        if (active) setTypesError(toMessage(requestError));
      })
      .finally(() => {
        if (active) setTypesLoading(false);
      });
    return () => {
      active = false;
    };
  }, [retryCount]);

  async function submitInstallation(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setSuccess(null);
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    const originAddress = String(form.get("originAddress") ?? "").trim();
    const destinationAddress = String(form.get("destinationAddress") ?? "").trim();
    const desiredDateTime = String(form.get("desiredStartAt") ?? "");
    const desiredStart = new Date(desiredDateTime.replace(" ", "T"));
    if (!originAddress || !destinationAddress) {
      setError("이전 주소와 설치 주소를 입력하거나 주소 검색에서 선택해 주세요.");
      return;
    }
    if (!desiredDateTime || Number.isNaN(desiredStart.getTime()) || desiredStart.getTime() <= Date.now()) {
      setError("현재 시각 이후의 희망 날짜와 시간을 선택해 주세요.");
      return;
    }

    setRequestLoading(true);
    try {
      const response = await createInstallation({
        applianceTypeCode: String(form.get("applianceTypeCode") ?? ""),
        originAddress,
        destinationAddress,
        desiredStartAt: desiredStart.toISOString(),
        carryingDistanceMeters: Number(form.get("carryingDistanceMeters") ?? 0),
        floorCount: Number(form.get("floorCount") ?? 0),
        additionalPipingMeters: Number(form.get("additionalPipingMeters") ?? 0),
      });
      setSuccess(response);
      setShowAdditionalPrompt(true);
      setFormGeneration((generation) => generation + 1);
      formElement.reset();
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      setRequestLoading(false);
    }
  }

  if (showHistory) {
    return (
      <>
        <InstallationHistory onCreateAnother={onViewHome} />
        <CustomerInformationPanel />
      </>
    );
  }

  return (
    <>
      <div className="form-card installation-card customer-request-card">
        <div className="card-heading">
          <span className="step-label">REQUEST · 01</span>
          <h2>설치 요청하기</h2>
          <p>이전할 제품과 주소, 희망 일정을 입력해 주세요.</p>
        </div>
        <form
          className="form-stack"
          key={formGeneration}
          onInvalidCapture={() => setError("필수 항목을 입력하고 추가 작업 조건의 입력 범위를 확인해 주세요.")}
          onSubmit={(event) => void submitInstallation(event)}
        >
          <label>
            가전제품 종류 <span className="required">*</span>
            <select
              name="applianceTypeCode"
              required
              disabled={typesLoading || applianceTypes.length === 0}
              defaultValue=""
            >
              <option value="" disabled>
                {typesLoading ? "제품 종류를 불러오는 중..." : "제품을 선택해 주세요"}
              </option>
              {applianceTypes.map((type) => (
                <option key={type.code} value={type.code}>{type.name}</option>
              ))}
            </select>
          </label>

          {typesError && (
            <div className="load-error" role="alert">
              <p className="inline-error">{typesError}</p>
              <button
                className="text-button"
                onClick={() => setRetryCount((count) => count + 1)}
                type="button"
              >
                다시 불러오기
              </button>
            </div>
          )}
          {!typesLoading && !typesError && applianceTypes.length === 0 && (
            <p className="empty-state">선택 가능한 제품 종류가 없습니다.</p>
          )}

          <AddressSearchInput label="이전 주소" name="originAddress" />
          <AddressSearchInput label="설치 주소" name="destinationAddress" />
          <DesiredDateTimeInput name="desiredStartAt" />
          <div className="installation-conditions">
            <strong>추가 작업 조건 <small>(선택)</small></strong>
            <p>입력한 조건은 관리자가 발행하는 견적 산출에 사용됩니다.</p>
            <div className="installation-condition-grid">
              <label>
                운반 거리 (m)
                <input defaultValue={0} min={0} max={10000} name="carryingDistanceMeters" type="number" />
              </label>
              <label>
                작업 층수 (층)
                <input defaultValue={0} min={0} max={200} name="floorCount" type="number" />
              </label>
              <label>
                추가 배관 길이 (m)
                <input
                  defaultValue={0}
                  min={0}
                  max={1000}
                  name="additionalPipingMeters"
                  step="0.01"
                  type="number"
                />
              </label>
            </div>
          </div>

          {error && <p className="form-error" role="alert">{error}</p>}
          {success && (
            <div className="success-message" role="status">
              <strong>설치 요청이 접수되었습니다.</strong>
              <span>요청 번호: {success.requestNo}</span>
            </div>
          )}
          <button
            className="primary-button"
            disabled={requestLoading || typesLoading || applianceTypes.length === 0}
            type="submit"
          >
            {requestLoading ? "요청을 접수하고 있어요..." : "설치 요청 접수"}
            {!requestLoading && <span aria-hidden="true">→</span>}
          </button>
          <p className="privacy-note">입력하신 정보는 설치 상담과 서비스 제공을 위해 사용됩니다.</p>
        </form>
      </div>

      <CustomerInformationPanel />

      {showAdditionalPrompt && success && (
        <div className="popup-backdrop">
          <section
            aria-labelledby="additional-request-title"
            aria-modal="true"
            className="popup-card additional-request-dialog"
            role="dialog"
          >
            <div className="card-heading">
              <span className="step-label">REQUEST RECEIVED</span>
              <h2 id="additional-request-title">설치 요청이 접수되었습니다.</h2>
              <p>요청 번호: {success.requestNo}</p>
              <p>추가로 설치 요청을 접수하시겠습니까?</p>
            </div>
            <div className="additional-request-actions">
              <button
                className="primary-button"
                onClick={() => {
                  setShowAdditionalPrompt(false);
                  setSuccess(null);
                  setError(null);
                }}
                type="button"
              >
                추가 접수하기
              </button>
              <button
                className="secondary-button"
                onClick={() => {
                  setShowAdditionalPrompt(false);
                  onViewHistory();
                }}
                type="button"
              >
                아니요, 접수 내역 확인
              </button>
            </div>
          </section>
        </div>
      )}
    </>
  );
}

function CustomerInformationPanel() {
  return (
    <aside className="info-panel customer-info-panel">
      <span className="step-label">HOW IT WORKS</span>
      <h2>요청은 간단하게,<br />이전은 안심하고.</h2>
      <ol className="process-list">
        <li><span>1</span><div><strong>제품 선택</strong><small>이전할 가전 종류를 알려주세요.</small></div></li>
        <li><span>2</span><div><strong>이동 정보 입력</strong><small>출발지와 도착지를 입력해 주세요.</small></div></li>
        <li><span>3</span><div><strong>일정 선택</strong><small>편하신 날짜와 시간을 선택해 주세요.</small></div></li>
      </ol>
      <div className="support-note">
        <span aria-hidden="true">✦</span>
        <p>요청 접수 후 담당자가<br />일정을 확인해 안내드릴게요.</p>
      </div>
    </aside>
  );
}

function toMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.fieldErrors?.length) {
      return error.fieldErrors.map((fieldError) => fieldError.reason).join(" ");
    }
    return error.message;
  }
  if (error instanceof Error) return error.message;
  return "요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.";
}
