import { useEffect, useState } from "react";
import {
  getMembers,
  updateMemberRoles,
  updateMemberStatus,
} from "../api/auth";
import { ApiError } from "../api/client";
import type { AccountRole, AccountSummary, MemberProfile } from "../types/installation";

const AVAILABLE_ROLES: AccountRole[] = ["CUSTOMER", "TECHNICIAN", "ADMIN"];
const ACCOUNT_STATUSES: MemberProfile["status"][] = ["ACTIVE", "SUSPENDED", "WITHDRAWN"];

interface AdminMemberManagementProps {
  currentMemberId: string;
}

export function AdminMemberManagement({ currentMemberId }: AdminMemberManagementProps) {
  const [accounts, setAccounts] = useState<AccountSummary[]>([]);
  const [page, setPage] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  async function refreshAccounts(requestedPage: number) {
    setLoading(true);
    setError(null);
    try {
      const result = await getMembers(requestedPage);
      setAccounts(result.content);
      setPage(result.page);
      setTotalElements(result.totalElements);
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void refreshAccounts(0);
  }, []);

  async function saveRoles(account: AccountSummary, roles: AccountRole[]) {
    setError(null);
    try {
      await updateMemberRoles(account.id, roles);
      await refreshAccounts(page);
    } catch (requestError) {
      setError(toMessage(requestError));
    }
  }

  async function saveStatus(account: AccountSummary, status: MemberProfile["status"]) {
    setError(null);
    try {
      await updateMemberStatus(account.id, status);
      await refreshAccounts(page);
    } catch (requestError) {
      setError(toMessage(requestError));
    }
  }

  const hasNextPage = (page + 1) * 20 < totalElements;

  return (
    <div className="admin-members">
      <div className="card-heading">
        <span className="step-label">ADMINISTRATION</span>
        <h3>회원 역할 및 계정 상태</h3>
      </div>
      {error && <p className="form-error" role="alert">{error}</p>}
      {loading ? (
        <p aria-live="polite">회원 목록을 불러오는 중...</p>
      ) : accounts.length === 0 ? (
        <p className="empty-state">조회할 회원이 없습니다.</p>
      ) : (
        <div className="admin-account-list">
          {accounts.map((account) => (
            <MemberManagementRow
              account={account}
              disabled={account.id === currentMemberId}
              key={account.id}
              onSaveRoles={saveRoles}
              onSaveStatus={saveStatus}
            />
          ))}
        </div>
      )}
      <div className="admin-pagination">
        <button className="text-button" disabled={page === 0 || loading} onClick={() => void refreshAccounts(page - 1)} type="button">
          이전
        </button>
        <span>{page + 1} 페이지 · 전체 {totalElements}명</span>
        <button className="text-button" disabled={!hasNextPage || loading} onClick={() => void refreshAccounts(page + 1)} type="button">
          다음
        </button>
      </div>
    </div>
  );
}

interface MemberManagementRowProps {
  account: AccountSummary;
  disabled: boolean;
  onSaveRoles: (account: AccountSummary, roles: AccountRole[]) => Promise<void>;
  onSaveStatus: (account: AccountSummary, status: MemberProfile["status"]) => Promise<void>;
}

function MemberManagementRow({
  account,
  disabled,
  onSaveRoles,
  onSaveStatus,
}: MemberManagementRowProps) {
  const [roles, setRoles] = useState<AccountRole[]>(account.roles);
  const [status, setStatus] = useState<MemberProfile["status"]>(account.status);

  return (
    <article className="admin-account-row">
      <div>
        <strong>{account.name}</strong>
        <small>{account.email}</small>
        {disabled && <small>본인 계정의 역할·상태는 이 화면에서 변경할 수 없습니다.</small>}
      </div>
      <fieldset disabled={disabled}>
        <legend>역할</legend>
        {AVAILABLE_ROLES.map((role) => (
          <label key={role}>
            <input
              checked={roles.includes(role)}
              onChange={(event) => setRoles((current) => (
                event.target.checked
                  ? [...current, role]
                  : current.filter((item) => item !== role)
              ))}
              type="checkbox"
            />
            {role}
          </label>
        ))}
        <button
          className="text-button"
          disabled={roles.length === 0 || sameRoles(roles, account.roles)}
          onClick={() => void onSaveRoles(account, roles)}
          type="button"
        >
          역할 저장
        </button>
      </fieldset>
      <label className="admin-status-control">
        계정 상태
        <select
          disabled={disabled}
          onChange={(event) => setStatus(event.target.value as MemberProfile["status"])}
          value={status}
        >
          {ACCOUNT_STATUSES.map((item) => <option key={item} value={item}>{item}</option>)}
        </select>
      </label>
      <button
        className="text-button"
        disabled={disabled || status === account.status}
        onClick={() => void onSaveStatus(account, status)}
        type="button"
      >
        상태 저장
      </button>
    </article>
  );
}

function sameRoles(left: AccountRole[], right: AccountRole[]): boolean {
  return left.length === right.length && left.every((role) => right.includes(role));
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
