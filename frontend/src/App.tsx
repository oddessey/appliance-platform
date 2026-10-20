import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { bootstrapAdmin, getCurrentMember, logIn, logOut, signUp } from "./api/auth";
import { ApiError, getAccessToken, setAccessToken } from "./api/client";
import { AccountSettings } from "./components/AccountSettings";
import { AdminScreen } from "./screens/AdminScreen";
import { CustomerScreen } from "./screens/CustomerScreen";
import { TechnicianScreen } from "./screens/TechnicianScreen";
import type { AuthUser, MemberProfile } from "./types/installation";
import "./App.css";

type Screen = "customer" | "technician" | "admin";
type AuthMode = "login" | "signup";

function screenForPath(path: string): Screen {
  if (path.startsWith("/technician")) return "technician";
  if (path.startsWith("/admin") && path !== "/admin/register") return "admin";
  return "customer";
}

function screenPath(screen: Screen, history = false): string {
  if (screen === "customer") return history ? "/customer/history" : "/customer";
  return `/${screen}`;
}

function defaultScreen(user: AuthUser): Screen {
  if (user.roles.includes("ADMIN")) return "admin";
  if (user.roles.includes("TECHNICIAN")) return "technician";
  return "customer";
}

function screenTitle(screen: Screen): { title: string; description: string } {
  const content: Record<Screen, { title: string; description: string }> = {
    customer: {
      title: "새로운 공간으로, 가전은 편안하게.",
      description: "제품과 이동 정보를 입력하고 원하는 일정을 알려주세요.",
    },
    technician: {
      title: "오늘의 작업을, 한눈에 확인하세요.",
      description: "배정된 작업 일정과 고객 요청을 확인하고 안전하게 진행하세요.",
    },
    admin: {
      title: "요청부터 예약까지, 운영 업무를 관리하세요.",
      description: "설치 요청과 AI 결과를 검토하고 회원·기사·가격·예약을 관리하세요.",
    },
  };
  return content[screen];
}

export function App() {
  const [authUser, setAuthUser] = useState<AuthUser | null>(readCachedUser);
  const [authMode, setAuthMode] = useState<AuthMode>(readAuthMode);
  const [path, setPath] = useState(window.location.pathname);
  const [accountSettingsOpen, setAccountSettingsOpen] = useState(false);
  const [authLoading, setAuthLoading] = useState(false);
  const [sessionChecking, setSessionChecking] = useState(() => Boolean(getAccessToken()));
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    const token = getAccessToken();
    if (!token) {
      cacheUser(null);
      setAuthUser(null);
      setSessionChecking(false);
      return;
    }

    let active = true;
    getCurrentMember()
      .then((member) => {
        if (!active) return;
        setAuthUser(member);
        cacheUser(member);
      })
      .catch((error: unknown) => {
        if (!active) return;
        if (error instanceof ApiError && error.status === 401) {
          setAccessToken(null);
          setAuthUser(null);
          cacheUser(null);
          setAuthMode("login");
          rememberAuthMode("login");
          setErrorMessage("로그인이 만료되었습니다. 다시 로그인해 주세요.");
        } else {
          setErrorMessage(toMessage(error));
        }
      })
      .finally(() => {
        if (active) setSessionChecking(false);
      });

    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    function syncPath() {
      setPath(window.location.pathname);
    }
    window.addEventListener("popstate", syncPath);
    return () => window.removeEventListener("popstate", syncPath);
  }, []);

  useEffect(() => {
    function handleUnauthorized() {
      setAuthUser(null);
      cacheUser(null);
      setAuthMode("login");
      rememberAuthMode("login");
      setAccountSettingsOpen(false);
      setErrorMessage("로그인이 만료되었습니다. 다시 로그인해 주세요.");
    }

    window.addEventListener("appliance-platform:unauthorized", handleUnauthorized);
    return () => window.removeEventListener("appliance-platform:unauthorized", handleUnauthorized);
  }, []);

  useEffect(() => {
    if (!authUser && path !== "/admin/register"
        && (path.startsWith("/admin") || path.startsWith("/technician"))) {
      setAuthMode("login");
    }
  }, [authUser, path]);

  function navigate(nextPath: string, replace = false) {
    if (replace) {
      window.history.replaceState(null, "", nextPath);
    } else {
      window.history.pushState(null, "", nextPath);
    }
    setPath(nextPath);
    setAccountSettingsOpen(false);
    setErrorMessage(null);
  }

  function openScreen(screen: Screen) {
    navigate(screenPath(screen));
  }

  async function handleAuthSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage(null);
    setAuthLoading(true);
    const form = new FormData(event.currentTarget);
    const email = String(form.get("email") ?? "");
    const password = String(form.get("password") ?? "");
    const accountType = form.get("accountType") === "TECHNICIAN" ? "TECHNICIAN" : "CUSTOMER";

    try {
      const response = authMode === "signup"
        ? await signUp({
            email,
            password,
            name: String(form.get("name") ?? ""),
            phone: String(form.get("phone") ?? ""),
            accountType,
          })
        : await logIn({ email, password });
      setAuthUser(response.user);
      cacheUser(response.user);
      rememberAuthMode(null);
      navigate(screenPath(defaultScreen(response.user)), true);
    } catch (error) {
      setErrorMessage(toMessage(error));
    } finally {
      setAuthLoading(false);
    }
  }

  async function handleAdminBootstrapSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage(null);
    setAuthLoading(true);
    const form = new FormData(event.currentTarget);
    try {
      const response = await bootstrapAdmin({
        bootstrapKey: String(form.get("bootstrapKey") ?? ""),
        email: String(form.get("email") ?? ""),
        password: String(form.get("password") ?? ""),
        name: String(form.get("name") ?? ""),
        phone: String(form.get("phone") ?? ""),
      });
      setAuthUser(response.user);
      cacheUser(response.user);
      rememberAuthMode(null);
      navigate("/admin", true);
    } catch (error) {
      setErrorMessage(toMessage(error));
    } finally {
      setAuthLoading(false);
    }
  }

  async function handleLogOut() {
    try {
      await logOut();
    } catch (error) {
      setErrorMessage(toMessage(error));
    } finally {
      setAuthUser(null);
      cacheUser(null);
      rememberAuthMode(null);
      setAccountSettingsOpen(false);
      navigate("/", true);
    }
  }

  function handleProfileChanged(profile: MemberProfile) {
    setAuthUser((current) => {
      if (!current) return current;
      const updated = { ...current, name: profile.name };
      cacheUser(updated);
      return updated;
    });
  }

  function handlePasswordChanged() {
    setAuthUser(null);
    cacheUser(null);
    rememberAuthMode("login");
    setAccountSettingsOpen(false);
    navigate("/", true);
  }

  const isAdminRegistration = path === "/admin/register";
  const screen = path === "/" && authUser ? defaultScreen(authUser) : screenForPath(path);
  const title = screenTitle(screen);
  const hasScreenAccess = authUser?.roles.includes(screenRole(screen)) ?? false;
  const heroBenefits: Record<Screen, string[]> = {
    customer: ["제품 선택", "일정 맞춤", "안심 이전"],
    technician: ["오늘의 배정", "현장 진행", "완료 증빙"],
    admin: ["요청·AI 검토", "회원·기사 관리", "예약·배정 운영"],
  };

  return (
    <main className={`page-shell page-shell--${screen}`}>
      <header className="site-header">
        <a
          className="brand"
          href={authUser ? screenPath(defaultScreen(authUser)) : "/"}
          aria-label="가전 이전설치 홈"
          onClick={(event) => {
            event.preventDefault();
            navigate(authUser ? screenPath(defaultScreen(authUser)) : "/");
          }}
        >
          <span className="brand-mark" aria-hidden="true">S</span>
          <span>스마트무브</span>
        </a>
        {authUser ? (
          <div className="account-bar">
            <span>{authUser.name}님</span>
            {(["customer", "technician", "admin"] as const)
              .filter((item) => authUser.roles.includes(screenRole(item)))
              .map((item) => (
                <button
                  aria-current={screen === item ? "page" : undefined}
                  className={`screen-nav-button${screen === item ? " is-current" : ""}`}
                  key={item}
                  onClick={() => openScreen(item)}
                  type="button"
                >
                  {screenLabel(item)}
                </button>
              ))}
            {authUser.roles.includes("CUSTOMER") && (
              <button
                className="text-button"
                onClick={() => navigate("/customer/history")}
                type="button"
              >
                접수 내역
              </button>
            )}
            <button
              className="text-button"
              onClick={() => setAccountSettingsOpen((open) => !open)}
              type="button"
            >
              내 정보
            </button>
            <button className="text-button" onClick={() => void handleLogOut()} type="button">
              로그아웃
            </button>
          </div>
        ) : (
          <span className="header-caption">안전하고 간편한 가전 이전설치</span>
        )}
      </header>

      {accountSettingsOpen && authUser && (
        <AccountSettings
          onPasswordChanged={handlePasswordChanged}
          onProfileChanged={handleProfileChanged}
        />
      )}

      <section className={`hero hero--${screen}`}>
        <div className="hero-copy">
          <span className="eyebrow">
            {isAdminRegistration ? "SECURE ADMIN SETUP" : `${screen.toUpperCase()} WORKSPACE`}
          </span>
          <h1>{isAdminRegistration ? <>최초 관리자 계정을,<br />안전하게 등록하세요.</> : title.title}</h1>
          <p>
            {isAdminRegistration
              ? "서버에 설정된 1회용 등록 키가 있어야 초기 관리자 계정을 만들 수 있습니다."
              : title.description}
          </p>
          <div className="hero-benefits">
            {heroBenefits[screen].map((item, index) => (
              <span key={item}><b>0{index + 1}</b> {item}</span>
            ))}
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

      {isAdminRegistration && !authUser ? (
        <section className={`content-grid screen-content screen-content--${screen}`}>
          <AdminRegistrationScreen
            error={errorMessage}
            loading={authLoading}
            onSubmit={(event) => void handleAdminBootstrapSubmit(event)}
          />
          <PublicInformationPanel />
        </section>
      ) : !authUser ? (
        <section className={`content-grid screen-content screen-content--${screen}`}>
          {sessionChecking ? (
            <div aria-live="polite" className="form-card installation-card">로그인 상태를 확인하는 중...</div>
          ) : (
            <AuthScreen
              authMode={authMode}
              error={errorMessage}
              loading={authLoading}
              screen={screen}
              onModeChange={(mode) => {
                setAuthMode(mode);
                rememberAuthMode(mode);
                setErrorMessage(null);
              }}
              onSubmit={(event) => void handleAuthSubmit(event)}
            />
          )}
          <PublicInformationPanel />
        </section>
      ) : hasScreenAccess ? (
        <>
          {path === "/customer/history" && screen === "customer" && (
            <div className="customer-screen-heading">
              <button className="text-button" onClick={() => openScreen("customer")} type="button">
                고객 화면으로
              </button>
            </div>
          )}
          <section className={`content-grid screen-content screen-content--${screen}`}>
            {screen === "customer" ? (
              <CustomerScreen
                showHistory={path === "/customer/history"}
                onViewHistory={() => navigate("/customer/history")}
                onViewHome={() => openScreen("customer")}
              />
            ) : screen === "technician" ? (
              <TechnicianScreen technicianName={authUser.name} />
            ) : (
              <AdminScreen currentMemberId={authUser.id} />
            )}
          </section>
        </>
      ) : (
        <section className="content-grid">
          <div className="form-card installation-card role-access-denied">
            <span className="step-label">ROLE ACCESS</span>
            <h2>이 화면에 접근할 수 없습니다.</h2>
            <p>현재 계정에 필요한 역할이 없습니다. 계정에 부여된 역할의 화면으로 이동해 주세요.</p>
            <div className="additional-request-actions">
              {(["customer", "technician", "admin"] as const)
                .filter((item) => authUser.roles.includes(screenRole(item)))
                .map((item) => (
                  <button className="primary-button" key={item} onClick={() => openScreen(item)} type="button">
                    {screenLabel(item)}
                  </button>
                ))}
            </div>
          </div>
          <PublicInformationPanel />
        </section>
      )}

      <footer className="site-footer">
        <span>스마트무브</span>
        <span>더 나은 공간으로의 이동을 돕습니다.</span>
      </footer>
    </main>
  );
}

const AUTH_USER_KEY = "appliance-platform.auth-user";
const AUTH_MODE_KEY = "appliance-platform.auth-mode";

function readAuthMode(): AuthMode {
  try {
    if (window.sessionStorage.getItem(AUTH_MODE_KEY) === "login") return "login";
  } catch {
    return "signup";
  }
  return window.location.pathname.startsWith("/admin")
      || window.location.pathname.startsWith("/technician")
    ? "login"
    : "signup";
}

function rememberAuthMode(mode: AuthMode | null): void {
  try {
    if (mode) {
      window.sessionStorage.setItem(AUTH_MODE_KEY, mode);
    } else {
      window.sessionStorage.removeItem(AUTH_MODE_KEY);
    }
  } catch {
    return;
  }
}

function readCachedUser(): AuthUser | null {
  try {
    const value: unknown = JSON.parse(window.sessionStorage.getItem(AUTH_USER_KEY) ?? "null");
    if (typeof value !== "object" || value === null) return null;
    const user = value as Record<string, unknown>;
    if (typeof user.id !== "string" || typeof user.email !== "string"
        || typeof user.name !== "string" || !Array.isArray(user.roles)
        || !user.roles.every((role) => typeof role === "string")) {
      return null;
    }
    return {
      id: user.id,
      email: user.email,
      name: user.name,
      roles: user.roles,
    };
  } catch {
    return null;
  }
}

function cacheUser(user: AuthUser | null): void {
  try {
    if (user) {
      window.sessionStorage.setItem(AUTH_USER_KEY, JSON.stringify(user));
    } else {
      window.sessionStorage.removeItem(AUTH_USER_KEY);
    }
  } catch {
    return;
  }
}

interface AuthScreenProps {
  authMode: AuthMode;
  error: string | null;
  loading: boolean;
  screen: Screen;
  onModeChange: (mode: AuthMode) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
}

function AuthScreen({ authMode, error, loading, screen, onModeChange, onSubmit }: AuthScreenProps) {
  const isRoleLogin = screen !== "customer";
  return (
    <div className="form-card installation-card">
      <div className="card-heading">
        <span className="step-label">{isRoleLogin ? `${screen.toUpperCase()} ACCESS` : "REQUEST · 01"}</span>
        <h2>{isRoleLogin ? `${screenLabel(screen)} 로그인` : "설치 요청하기"}</h2>
        <p>
          {isRoleLogin
            ? `${screenLabel(screen)} 역할이 있는 계정으로 로그인해 주세요.`
            : "로그인하거나 가입하고 설치 요청을 시작해 주세요."}
        </p>
      </div>
      <div className="auth-section">
        {!isRoleLogin && (
          <>
            <div className="auth-tabs" role="tablist" aria-label="계정 메뉴">
              <button
                aria-selected={authMode === "signup"}
                className={authMode === "signup" ? "active" : ""}
                onClick={() => onModeChange("signup")}
                role="tab"
                type="button"
              >
                회원가입
              </button>
              <button
                aria-selected={authMode === "login"}
                className={authMode === "login" ? "active" : ""}
                onClick={() => onModeChange("login")}
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
          </>
        )}
        <form className="form-stack" onSubmit={onSubmit}>
          {authMode === "signup" && !isRoleLogin && (
            <>
              <label>
                가입 유형 <span className="required">*</span>
                <select defaultValue="CUSTOMER" name="accountType" required>
                  <option value="CUSTOMER">고객</option>
                  <option value="TECHNICIAN">기사</option>
                </select>
              </label>
              <label>
                이름 <span className="required">*</span>
                <input autoComplete="name" maxLength={100} name="name" required />
              </label>
              <label>
                연락처 <span className="required">*</span>
                <input autoComplete="tel" maxLength={30} name="phone" placeholder="010-1234-5678" required type="tel" />
              </label>
            </>
          )}
          <label>
            이메일 <span className="required">*</span>
            <input autoComplete="email" maxLength={254} name="email" required type="email" />
          </label>
          <label>
            비밀번호 <span className="required">*</span>
            <input
              autoComplete={authMode === "signup" && !isRoleLogin ? "new-password" : "current-password"}
              maxLength={72}
              minLength={8}
              name="password"
              required
              type="password"
            />
            {authMode === "signup" && !isRoleLogin && <small className="field-hint">8자 이상, UTF-8 기준 72바이트 이하</small>}
            {authMode === "signup" && !isRoleLogin && (
              <small className="field-hint">
                기사로 가입하면 기사 역할이 부여되며, 프로필 등록 후 관리자 승인·활성화 시 작업 화면을 이용할 수 있습니다.
              </small>
            )}
          </label>
          {error && <p className="form-error" role="alert">{error}</p>}
          <button className="primary-button" disabled={loading} type="submit">
            {loading ? "처리 중..." : authMode === "signup" ? "가입하고 계속하기" : "로그인"}
            {!loading && <span aria-hidden="true">→</span>}
          </button>
        </form>
        {authMode === "login" && screen === "admin" && (
          <a className="admin-bootstrap-link" href="/admin/register">최초 관리자 등록</a>
        )}
      </div>
    </div>
  );
}

interface AdminRegistrationScreenProps {
  error: string | null;
  loading: boolean;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
}

function AdminRegistrationScreen({ error, loading, onSubmit }: AdminRegistrationScreenProps) {
  return (
    <div className="form-card installation-card">
      <div className="card-heading">
        <span className="step-label">SECURE SETUP · ONE TIME</span>
        <h2>최초 관리자 등록</h2>
        <p>부트스트랩 키는 서버 환경설정 값과 일치해야 하며, 성공 후 이 등록은 비활성화됩니다.</p>
      </div>
      <form className="form-stack admin-bootstrap-form" onSubmit={onSubmit}>
        <label>
          1회용 부트스트랩 키 <span className="required">*</span>
          <input autoComplete="off" maxLength={256} name="bootstrapKey" required type="password" />
        </label>
        <label>이름 <span className="required">*</span><input autoComplete="name" maxLength={100} name="name" required /></label>
        <label>
          연락처 <span className="required">*</span>
          <input autoComplete="tel" maxLength={30} name="phone" placeholder="010-1234-5678" required type="tel" />
        </label>
        <label>이메일 <span className="required">*</span><input autoComplete="email" maxLength={254} name="email" required type="email" /></label>
        <label>
          비밀번호 <span className="required">*</span>
          <input autoComplete="new-password" maxLength={72} minLength={8} name="password" required type="password" />
          <small className="field-hint">8자 이상, UTF-8 기준 72바이트 이하</small>
        </label>
        {error && <p className="form-error" role="alert">{error}</p>}
        <button className="primary-button" disabled={loading} type="submit">
          {loading ? "관리자 계정을 등록하고 있어요..." : "초기 관리자 등록"}
          {!loading && <span aria-hidden="true">→</span>}
        </button>
        <a className="admin-bootstrap-back" href="/">로그인 화면으로 돌아가기</a>
      </form>
    </div>
  );
}

function PublicInformationPanel() {
  return (
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
  );
}

function screenRole(screen: Screen): string {
  return { customer: "CUSTOMER", technician: "TECHNICIAN", admin: "ADMIN" }[screen];
}

function screenLabel(screen: Screen): string {
  return { customer: "고객 화면", technician: "기사 화면", admin: "관리자 화면" }[screen];
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
