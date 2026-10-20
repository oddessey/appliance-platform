import type { ApiErrorResponse } from "../types/installation";

const API_BASE_URL = "/api/v1";
const ACCESS_TOKEN_KEY = "appliance-platform.access-token";
let accessToken: string | null = readAccessToken();

function readAccessToken(): string | null {
  try {
    return window.sessionStorage.getItem(ACCESS_TOKEN_KEY);
  } catch {
    return null;
  }
}

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fieldErrors: ApiErrorResponse["fieldErrors"];

  constructor(error: ApiErrorResponse) {
    super(error.message);
    this.name = "ApiError";
    this.status = error.status;
    this.code = error.code;
    this.fieldErrors = error.fieldErrors;
  }
}

function isApiErrorResponse(value: unknown): value is ApiErrorResponse {
  if (typeof value !== "object" || value === null) return false;
  const error = value as Record<string, unknown>;
  return typeof error.status === "number"
    && typeof error.code === "string"
    && typeof error.message === "string";
}

async function createApiError(response: Response): Promise<ApiError> {
  const body = await response.text();
  if (body) {
    try {
      const payload: unknown = JSON.parse(body);
      if (isApiErrorResponse(payload)) {
        return new ApiError(payload);
      }
    } catch (error) {
      if (!(error instanceof SyntaxError)) throw error;
    }
  }

  const message = response.status === 401
    ? "로그인이 만료되었습니다. 다시 로그인해 주세요."
    : response.status === 403
      ? "이 요청을 처리할 권한이 없습니다."
      : response.status >= 500
        ? `서버에서 요청을 처리하지 못했습니다. (오류 ${response.status}) 잠시 후 다시 시도해 주세요.`
        : `요청을 처리하지 못했습니다. (오류 ${response.status}) 입력 내용을 확인해 주세요.`;
  return new ApiError({
    status: response.status,
    code: "HTTP_ERROR",
    message,
  });
}

export function setAccessToken(token: string | null): void {
  accessToken = token;
  try {
    if (token) {
      window.sessionStorage.setItem(ACCESS_TOKEN_KEY, token);
    } else {
      window.sessionStorage.removeItem(ACCESS_TOKEN_KEY);
    }
  } catch {
    if (token) {
      throw new Error("로그인 상태를 이 브라우저 탭에 저장할 수 없습니다.");
    }
  }
}

export function getAccessToken(): string | null {
  return accessToken;
}

function clearExpiredSession(path: string, response: Response): void {
  if (response.status !== 401 || path.startsWith("/auth/")) return;
  setAccessToken(null);
  window.dispatchEvent(new Event("appliance-platform:unauthorized"));
}

export async function apiRequest<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set("Accept", "application/json");
  if (options.body !== undefined && !(options.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }
  if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers,
    });
  } catch (error) {
    if (error instanceof TypeError) {
      throw new Error("서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.");
    }
    throw error;
  }

  if (!response.ok) {
    clearExpiredSession(path, response);
    throw await createApiError(response);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

export async function apiBlobRequest(path: string): Promise<Blob> {
  const headers = new Headers({ Accept: "application/octet-stream" });
  if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }
  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, { headers });
  } catch (error) {
    if (error instanceof TypeError) {
      throw new Error("서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.");
    }
    throw error;
  }
  if (!response.ok) {
    clearExpiredSession(path, response);
    throw await createApiError(response);
  }
  return response.blob();
}
