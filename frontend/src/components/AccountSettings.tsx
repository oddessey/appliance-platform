import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import {
  changePassword,
  getCurrentMember,
  updateCurrentMember,
} from "../api/auth";
import { ApiError } from "../api/client";
import type { MemberProfile } from "../types/installation";
import { AdminMemberManagement } from "./AdminMemberManagement";

interface AccountSettingsProps {
  onProfileChanged: (profile: MemberProfile) => void;
  onPasswordChanged: () => void;
}

export function AccountSettings({
  onProfileChanged,
  onPasswordChanged,
}: AccountSettingsProps) {
  const [profile, setProfile] = useState<MemberProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [savingProfile, setSavingProfile] = useState(false);
  const [savingPassword, setSavingPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    getCurrentMember()
      .then((currentProfile) => {
        if (active) {
          setProfile(currentProfile);
        }
      })
      .catch((requestError: unknown) => {
        if (active) {
          setError(toMessage(requestError));
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, []);

  async function handleProfileSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setMessage(null);
    setSavingProfile(true);
    const form = new FormData(event.currentTarget);
    try {
      const updatedProfile = await updateCurrentMember({
        name: String(form.get("name") ?? ""),
        phone: String(form.get("phone") ?? ""),
      });
      setProfile(updatedProfile);
      onProfileChanged(updatedProfile);
      setMessage("회원 정보가 변경되었습니다.");
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      setSavingProfile(false);
    }
  }

  async function handlePasswordSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setMessage(null);
    setSavingPassword(true);
    const form = new FormData(event.currentTarget);
    try {
      await changePassword({
        currentPassword: String(form.get("currentPassword") ?? ""),
        newPassword: String(form.get("newPassword") ?? ""),
      });
      onPasswordChanged();
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      setSavingPassword(false);
    }
  }

  if (loading) {
    return <section className="form-card account-settings" aria-live="polite">회원 정보를 불러오는 중...</section>;
  }

  return (
    <section className="form-card account-settings">
      <div className="card-heading">
        <span className="step-label">ACCOUNT</span>
        <h2>회원 정보</h2>
        <p>내 정보와 비밀번호를 안전하게 관리합니다.</p>
      </div>
      {error && <p className="form-error" role="alert">{error}</p>}
      {message && <p className="success-message" role="status">{message}</p>}
      {profile ? (
        <>
          <form className="form-stack account-form" onSubmit={handleProfileSubmit}>
            <label>
              이메일
              <input value={profile.email} disabled readOnly />
            </label>
            <label>
              이름
              <input autoComplete="name" defaultValue={profile.name} maxLength={100} name="name" required />
            </label>
            <label>
              연락처
              <input autoComplete="tel" defaultValue={profile.phone} maxLength={30} name="phone" required />
            </label>
            <p className="account-meta">계정 상태: {profile.status} · 역할: {profile.roles.join(", ")}</p>
            <button className="primary-button" disabled={savingProfile} type="submit">
              {savingProfile ? "저장 중..." : "회원 정보 저장"}
            </button>
          </form>
          <form className="form-stack account-form" onSubmit={handlePasswordSubmit}>
            <h3>비밀번호 변경</h3>
            <label>
              현재 비밀번호
              <input autoComplete="current-password" maxLength={72} name="currentPassword" required type="password" />
            </label>
            <label>
              새 비밀번호
              <input autoComplete="new-password" maxLength={72} minLength={8} name="newPassword" required type="password" />
              <small className="field-hint">8자 이상, UTF-8 기준 72바이트 이하</small>
            </label>
            <button className="primary-button" disabled={savingPassword} type="submit">
              {savingPassword ? "변경 중..." : "비밀번호 변경"}
            </button>
          </form>
          {profile.roles.includes("ADMIN") && <AdminMemberManagement currentMemberId={profile.id} />}
        </>
      ) : (
        <p className="empty-state">회원 정보를 불러오지 못했습니다. 다시 로그인해 주세요.</p>
      )}
    </section>
  );
}

function toMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return "요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.";
}
