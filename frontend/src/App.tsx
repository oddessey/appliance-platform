import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { logIn, logOut, signUp } from "./api/auth";
import { ApiError } from "./api/client";
import { createInstallation, getApplianceTypes } from "./api/installations";
import type {
  ApplianceType,
  AuthUser,
  InstallationResponse,
} from "./types/installation";
import "./App.css";

type AuthMode = "login" | "signup";

export function App() {
  const [authUser, setAuthUser] = useState<AuthUser | null>(null);
  const [authMode, setAuthMode] = useState<AuthMode>("signup");
  const [applianceTypes, setApplianceTypes] = useState<ApplianceType[]>([]);
  const [typesLoading, setTypesLoading] = useState(true);
  const [typesError, setTypesError] = useState<string | null>(null);
  const [typesRetryCount, setTypesRetryCount] = useState(0);
  const [authLoading, setAuthLoading] = useState(false);
  const [requestLoading, setRequestLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [success, setSuccess] = useState<InstallationResponse | null>(null);

  useEffect(() => {
    let active = true;
    getApplianceTypes()
      .then((types) => {
        if (active) {
          setApplianceTypes(types);
        }
      })
      .catch((error: unknown) => {
        if (active) {
          setTypesError(toMessage(error));
        }
      })
      .finally(() => {
        if (active) {
          setTypesLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [typesRetryCount]);

  async function handleAuthSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage(null);
    setAuthLoading(true);
    const form = new FormData(event.currentTarget);
    const email = String(form.get("email") ?? "");
    const password = String(form.get("password") ?? "");

    try {
      const response =
        authMode === "signup"
          ? await signUp({
              email,
              password,
              name: String(form.get("name") ?? ""),
              phone: String(form.get("phone") ?? ""),
            })
          : await logIn({ email, password });
      setAuthUser(response.user);
    } catch (error) {
      setErrorMessage(toMessage(error));
    } finally {
      setAuthLoading(false);
    }
  }

  async function handleInstallationSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage(null);
    setSuccess(null);
    setRequestLoading(true);
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    const desiredDateTime = String(form.get("desiredStartAt") ?? "");

    try {
      const response = await createInstallation({
        applianceTypeCode: String(form.get("applianceTypeCode") ?? ""),
        originAddress: String(form.get("originAddress") ?? "").trim(),
        destinationAddress: String(form.get("destinationAddress") ?? "").trim(),
        desiredStartAt: new Date(desiredDateTime).toISOString(),
      });
      setSuccess(response);
      formElement.reset();
    } catch (error) {
      setErrorMessage(toMessage(error));
    } finally {
      setRequestLoading(false);
    }
  }

  function handleLogOut() {
    logOut();
    setAuthUser(null);
    setSuccess(null);
    setErrorMessage(null);
  }

  return (
    <main className="page-shell">
      <header className="site-header">
        <a className="brand" href="/" aria-label="가전 이전설치 홈">
          <span className="brand-mark" aria-hidden="true">S</span>
          <span>스마트무브</span>
        </a>
        {authUser ? (
          <div className="account-bar">
            <span>{authUser.name}님</span>
            <button className="text-button" onClick={handleLogOut} type="button">
              로그아웃
            </button>
          </div>
        ) : (
          <span className="header-caption">안전하고 간편한 가전 이전설치</span>
        )}
      </header>

      <section className="hero">
        <div className="hero-copy">
          <span className="eyebrow">SMART INSTALLATION</span>
          <h1>새로운 공간으로,<br />가전은 편안하게.</h1>
          <p>제품과 이동 정보를 입력하고 원하는 일정을 알려주세요.</p>
          <div className="hero-benefits">
            <span><b>01</b> 간편한 요청</span>
            <span><b>02</b> 일정 맞춤 상담</span>
            <span><b>03</b> 믿을 수 있는 설치</span>
          </div>
        </div>
        <div className="hero-art" aria-hidden="true">
          <div className="sun-orb" />
          <div className="appliance appliance-fridge"><i /><i /></div>
          <div className="appliance appliance-washer"><i /></div>
          <div className="art-ground" />
          <span className="floating-dot dot-one" />
          <span className="floating-dot dot-two" />
        </div>
      </section>

      <section className="content-grid">
        <div className="form-card installation-card">
          <div className="card-heading">
            <span className="step-label">REQUEST · 01</span>
            <h2>설치 요청하기</h2>
            <p>이전할 제품과 주소, 희망 일정을 입력해 주세요.</p>
          </div>

          {authUser ? (
            <form className="form-stack" onSubmit={handleInstallationSubmit}>
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
                    onClick={() => {
                      setTypesLoading(true);
                      setTypesError(null);
                      setTypesRetryCount((count) => count + 1);
                    }}
                    type="button"
                  >
                    다시 불러오기
                  </button>
                </div>
              )}
              {!typesLoading && !typesError && applianceTypes.length === 0 && (
                <p className="empty-state">선택 가능한 제품 종류가 없습니다.</p>
              )}

              <label>
                이전 주소 <span className="required">*</span>
                <input
                  autoComplete="street-address"
                  maxLength={500}
                  name="originAddress"
                  placeholder="가전을 가져올 주소를 입력해 주세요"
                  required
                />
              </label>

              <label>
                설치 주소 <span className="required">*</span>
                <input
                  autoComplete="street-address"
                  maxLength={500}
                  name="destinationAddress"
                  placeholder="가전을 설치할 주소를 입력해 주세요"
                  required
                />
              </label>

              <label>
                희망 날짜 및 시간 <span className="required">*</span>
                <input
                  min={getLocalDateTimeMinimum()}
                  name="desiredStartAt"
                  required
                  type="datetime-local"
                />
              </label>

              {errorMessage && <p className="form-error" role="alert">{errorMessage}</p>}
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
          ) : (
            <div className="auth-section">
              <div className="auth-tabs" role="tablist" aria-label="계정 메뉴">
                <button
                  aria-selected={authMode === "signup"}
                  className={authMode === "signup" ? "active" : ""}
                  onClick={() => { setAuthMode("signup"); setErrorMessage(null); }}
                  role="tab"
                  type="button"
                >
                  회원가입
                </button>
                <button
                  aria-selected={authMode === "login"}
                  className={authMode === "login" ? "active" : ""}
                  onClick={() => { setAuthMode("login"); setErrorMessage(null); }}
                  role="tab"
                  type="button"
                >
                  로그인
                </button>
              </div>
              <p className="auth-prompt">
                {authMode === "signup"
                  ? "간단한 정보를 입력하고 설치 요청을 시작하세요."
                  : "계정에 로그인해 설치 요청을 계속하세요."}
              </p>
              <form className="form-stack" onSubmit={handleAuthSubmit}>
                {authMode === "signup" && (
                  <>
                    <label>
                      이름 <span className="required">*</span>
                      <input autoComplete="name" maxLength={100} name="name" required />
                    </label>
                    <label>
                      연락처 <span className="required">*</span>
                      <input
                        autoComplete="tel"
                        maxLength={30}
                        name="phone"
                        placeholder="010-1234-5678"
                        required
                        type="tel"
                      />
                    </label>
                  </>
                )}
                <label>
                  이메일 <span className="required">*</span>
                  <input
                    autoComplete="email"
                    maxLength={254}
                    name="email"
                    required
                    type="email"
                  />
                </label>
                <label>
                  비밀번호 <span className="required">*</span>
                  <input
                    autoComplete={authMode === "signup" ? "new-password" : "current-password"}
                    maxLength={72}
                    minLength={8}
                    name="password"
                    required
                    type="password"
                  />
                  {authMode === "signup" && (
                    <small className="field-hint">8자 이상, UTF-8 기준 72바이트 이하</small>
                  )}
                </label>
                {errorMessage && <p className="form-error" role="alert">{errorMessage}</p>}
                <button className="primary-button" disabled={authLoading} type="submit">
                  {authLoading
                    ? "처리 중..."
                    : authMode === "signup" ? "가입하고 계속하기" : "로그인"}
                  {!authLoading && <span aria-hidden="true">→</span>}
                </button>
              </form>
            </div>
          )}
        </div>

        <aside className="info-panel">
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
      </section>

      <footer className="site-footer">
        <span>스마트무브</span>
        <span>더 나은 공간으로의 이동을 돕습니다.</span>
      </footer>
    </main>
  );
}

function getLocalDateTimeMinimum(): string {
  const now = new Date();
  now.setSeconds(0, 0);
  now.setMinutes(now.getMinutes() + 1);
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
  return now.toISOString().slice(0, 16);
}

function toMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.fieldErrors?.length) {
      return error.fieldErrors.map((fieldError) => fieldError.reason).join(" ");
    }
    return error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return "요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.";
}
