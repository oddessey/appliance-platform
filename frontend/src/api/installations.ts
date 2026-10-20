import { apiBlobRequest, apiRequest } from "./client";
import type {
  ApplianceType,
  AdminInstallationPage,
  CreateInstallationRequest,
  InstallationHistoryResponse,
  InstallationResponse,
  TechnicianAssignment,
  TechnicianAssignmentPage,
  TechnicianAssignmentStatus,
  TechnicianAdminPage,
  TechnicianApprovalStatus,
  TechnicianCandidate,
  TechnicianEvidence,
  PendingTechnicianReservationPage,
  TechnicianProfile,
  TechnicianProfileUpdateRequest,
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

export function getInstallations(page = 0, size = 20): Promise<InstallationHistoryResponse> {
  return apiRequest<InstallationHistoryResponse>(`/installations?page=${page}&size=${size}`);
}

export function getAdminInstallations(
  page = 0,
  size = 20,
  reviewRequired = false,
): Promise<AdminInstallationPage> {
  const query = new URLSearchParams({
    page: String(page),
    size: String(size),
    reviewRequired: String(reviewRequired),
  });
  return apiRequest<AdminInstallationPage>(`/admin/installations?${query.toString()}`);
}

export function getTechnicianAssignments(
  page = 0,
  size = 20,
  status?: TechnicianAssignmentStatus,
): Promise<TechnicianAssignmentPage> {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  if (status) {
    query.set("status", status);
  }
  return apiRequest<TechnicianAssignmentPage>(`/technicians/me/assignments?${query.toString()}`);
}

export function acceptAssignment(id: string): Promise<TechnicianAssignment> {
  return apiRequest<TechnicianAssignment>(`/assignments/${encodeURIComponent(id)}/accept`, {
    method: "POST",
  });
}

export function rejectAssignment(id: string, reason: string): Promise<TechnicianAssignment> {
  return apiRequest<TechnicianAssignment>(`/assignments/${encodeURIComponent(id)}/reject`, {
    method: "POST",
    body: JSON.stringify({ reason }),
  });
}

export function addAssignmentEvent(
  id: string,
  eventType: Exclude<TechnicianAssignmentStatus, "PENDING" | "REJECTED" | "CANCELLED">,
  note?: string,
): Promise<TechnicianAssignment> {
  return apiRequest<TechnicianAssignment>(`/assignments/${encodeURIComponent(id)}/events`, {
    method: "POST",
    body: JSON.stringify({ eventType, note }),
  });
}

export function getTechnicianProfile(): Promise<TechnicianProfile> {
  return apiRequest<TechnicianProfile>("/technicians/me/profile");
}

export function saveTechnicianProfile(
  request: TechnicianProfileUpdateRequest,
): Promise<TechnicianProfile> {
  return apiRequest<TechnicianProfile>("/technicians/me/profile", {
    method: "PUT",
    body: JSON.stringify(request),
  });
}

export function uploadAssignmentEvidence(id: string, file: File): Promise<TechnicianEvidence> {
  const form = new FormData();
  form.append("file", file);
  return apiRequest<TechnicianEvidence>(`/assignments/${encodeURIComponent(id)}/evidence`, {
    method: "POST",
    body: form,
  });
}

export function getAssignmentEvidence(id: string): Promise<TechnicianEvidence[]> {
  return apiRequest<TechnicianEvidence[]>(`/assignments/${encodeURIComponent(id)}/evidence`);
}

export async function downloadAssignmentEvidence(assignmentId: string, evidenceId: string): Promise<Blob> {
  return apiBlobRequest(
    `/assignments/${encodeURIComponent(assignmentId)}/evidence/${encodeURIComponent(evidenceId)}`,
  );
}

export function getAdminTechnicians(
  approvalStatus?: TechnicianApprovalStatus,
): Promise<TechnicianAdminPage> {
  const query = new URLSearchParams({ page: "0", size: "100" });
  if (approvalStatus) query.set("approvalStatus", approvalStatus);
  return apiRequest<TechnicianAdminPage>(`/admin/technicians?${query.toString()}`);
}

export function updateTechnicianApproval(
  profileId: string,
  approvalStatus: TechnicianApprovalStatus,
  active: boolean,
): Promise<void> {
  return apiRequest<void>(`/admin/technicians/${encodeURIComponent(profileId)}`, {
    method: "PATCH",
    body: JSON.stringify({ approvalStatus, active }),
  });
}

export function getTechnicianCandidates(reservationId: string): Promise<TechnicianCandidate[]> {
  return apiRequest<TechnicianCandidate[]>(
    `/admin/reservations/${encodeURIComponent(reservationId)}/technician-candidates`,
  );
}

export function getPendingTechnicianReservations(
  page = 0,
  size = 20,
): Promise<PendingTechnicianReservationPage> {
  return apiRequest<PendingTechnicianReservationPage>(
    `/admin/reservations/pending-technician-assignment?page=${page}&size=${size}`,
  );
}

export function assignTechnician(
  reservationId: string,
  technicianProfileId: string,
): Promise<TechnicianAssignment> {
  return apiRequest<TechnicianAssignment>(`/admin/reservations/${encodeURIComponent(reservationId)}/assignments`, {
    method: "POST",
    body: JSON.stringify({ technicianProfileId }),
  });
}
