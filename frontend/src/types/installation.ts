export interface ApplianceType {
  code: string;
  name: string;
}

export interface CreateInstallationRequest {
  applianceTypeCode: string;
  originAddress: string;
  destinationAddress: string;
  desiredStartAt: string;
}

export interface InstallationResponse {
  id: string;
  requestNo: string;
  status: string;
  createdAt: string;
}

export interface AuthUser {
  id: string;
  email: string;
  name: string;
  roles: string[];
}

export interface AuthResponse {
  accessToken: string;
  tokenType: "Bearer";
  expiresIn: number;
  user: AuthUser;
}

export interface AuthRequest {
  email: string;
  password: string;
  name?: string;
  phone?: string;
}

export interface ApiErrorResponse {
  status: number;
  code: string;
  message: string;
  fieldErrors?: Array<{ field: string; reason: string }>;
}
