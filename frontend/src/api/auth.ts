import { apiRequest, setAccessToken } from "./client";
import type {
  AccountPage,
  AccountRole,
  AccountSummary,
  AuthRequest,
  AuthResponse,
  BootstrapAdminRequest,
  ChangePasswordRequest,
  MemberProfile,
  UpdateProfileRequest,
} from "../types/installation";

export async function signUp(request: AuthRequest): Promise<AuthResponse> {
  const response = await apiRequest<AuthResponse>("/auth/signup", {
    method: "POST",
    body: JSON.stringify(request),
  });
  setAccessToken(response.accessToken);
  return response;
}

export async function logIn(request: AuthRequest): Promise<AuthResponse> {
  const response = await apiRequest<AuthResponse>("/auth/login", {
    method: "POST",
    body: JSON.stringify(request),
  });
  setAccessToken(response.accessToken);
  return response;
}

export async function bootstrapAdmin(request: BootstrapAdminRequest): Promise<AuthResponse> {
  const response = await apiRequest<AuthResponse>("/auth/bootstrap-admin", {
    method: "POST",
    body: JSON.stringify(request),
  });
  setAccessToken(response.accessToken);
  return response;
}

export async function logOut(): Promise<void> {
  try {
    await apiRequest<void>("/auth/logout", { method: "POST" });
  } finally {
    setAccessToken(null);
  }
}

export function getCurrentMember(): Promise<MemberProfile> {
  return apiRequest<MemberProfile>("/auth/me");
}

export function updateCurrentMember(request: UpdateProfileRequest): Promise<MemberProfile> {
  return apiRequest<MemberProfile>("/users/me", {
    method: "PATCH",
    body: JSON.stringify(request),
  });
}

export async function changePassword(request: ChangePasswordRequest): Promise<void> {
  await apiRequest<void>("/users/me/password", {
    method: "PATCH",
    body: JSON.stringify(request),
  });
  setAccessToken(null);
}

export function getMembers(page = 0, size = 20): Promise<AccountPage> {
  return apiRequest<AccountPage>(`/admin/users?page=${page}&size=${size}`);
}

export function updateMemberRoles(id: string, roles: AccountRole[]): Promise<AccountSummary> {
  return apiRequest<AccountSummary>(`/admin/users/${encodeURIComponent(id)}/roles`, {
    method: "PATCH",
    body: JSON.stringify({ roles }),
  });
}

export function updateMemberStatus(
  id: string,
  status: MemberProfile["status"],
): Promise<AccountSummary> {
  return apiRequest<AccountSummary>(`/admin/users/${encodeURIComponent(id)}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status }),
  });
}
