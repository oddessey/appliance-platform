import { apiRequest, setAccessToken } from "./client";
import type { AuthRequest, AuthResponse } from "../types/installation";

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

export function logOut(): void {
  setAccessToken(null);
}
