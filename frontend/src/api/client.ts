import type { ApiErrorResponse } from "../types/installation";

const API_BASE_URL = "/api/v1";
let accessToken: string | null = null;

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

export function setAccessToken(token: string | null): void {
  accessToken = token;
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
    const error = (await response.json()) as ApiErrorResponse;
    throw new ApiError(error);
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
    const error = (await response.json()) as ApiErrorResponse;
    throw new ApiError(error);
  }
  return response.blob();
}
