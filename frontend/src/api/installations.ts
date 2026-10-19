import { apiRequest } from "./client";
import type {
  ApplianceType,
  CreateInstallationRequest,
  InstallationResponse,
} from "../types/installation";

export function getApplianceTypes(): Promise<ApplianceType[]> {
  return apiRequest<ApplianceType[]>("/appliance-types");
}

export function createInstallation(
  request: CreateInstallationRequest,
): Promise<InstallationResponse> {
  return apiRequest<InstallationResponse>("/installations", {
    method: "POST",
    body: JSON.stringify(request),
  });
}
