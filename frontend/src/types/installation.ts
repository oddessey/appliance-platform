export interface ApplianceType {
  code: string;
  name: string;
}

export interface CreateInstallationRequest {
  applianceTypeCode: string;
  originAddress: string;
  destinationAddress: string;
  desiredStartAt: string;
  carryingDistanceMeters?: number;
  floorCount?: number;
  additionalPipingMeters?: number;
}

export interface InstallationResponse {
  id: string;
  requestNo: string;
  status: string;
  createdAt: string;
}

export type MoveRequestImagePurpose = "APPLIANCE" | "INSTALLATION_ENV";

export interface MoveRequestImage {
  id: string;
  moveRequestId: string;
  moveItemId: string | null;
  purpose: MoveRequestImagePurpose;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  uploadedAt: string;
}

export interface InstallationHistoryItem extends InstallationResponse {
  applianceTypeCode: string;
  applianceTypeName: string;
  originAddress: string;
  destinationAddress: string;
  desiredStartAt: string;
}

export interface InstallationHistoryResponse {
  page: number;
  size: number;
  totalElements: number;
  content: InstallationHistoryItem[];
}

export interface AdminAiAnalysisSummary {
  id: string;
  status: string;
  provider: string;
  modelName: string | null;
  confidence: number | null;
  resultJson: string | null;
  errorCode: string | null;
  errorMessage: string | null;
  createdAt: string;
}

export interface AdminInstallationSummary {
  id: string;
  requestNo: string;
  customerName: string;
  customerEmail: string;
  status: string;
  applianceSummary: string | null;
  originAddress: string;
  destinationAddress: string;
  desiredStartAt: string;
  submittedAt: string | null;
  createdAt: string;
  reservationId: string | null;
  reservationNo: string | null;
  reservationStatus: string | null;
  technicianAssignmentStatus: TechnicianAssignmentStatus | null;
  technicianProfileId: string | null;
  technicianName: string | null;
  latestAnalysis: AdminAiAnalysisSummary | null;
}

export interface AdminInstallationPage {
  page: number;
  size: number;
  totalElements: number;
  content: AdminInstallationSummary[];
}

export type TechnicianAssignmentStatus =
  | "PENDING"
  | "ACCEPTED"
  | "REJECTED"
  | "ON_THE_WAY"
  | "WORK_STARTED"
  | "WORK_COMPLETED"
  | "CANCELLED";

export interface TechnicianAssignment {
  id: string;
  moveRequestId: string;
  status: TechnicianAssignmentStatus;
  reservationNo: string;
  scheduledStartAt: string;
  scheduledEndAt: string;
  customerName: string;
  customerPhone: string;
  originAddress: string;
  destinationAddress: string;
  applianceTypes: string;
  customerMemo: string | null;
  quoteNo: string | null;
  quoteTotalAmount: number | null;
}

export type PriceConditionCode =
  | "BASE"
  | "CARRYING_DISTANCE"
  | "FLOOR"
  | "ADDITIONAL_PIPING"
  | "DISCOUNT";

export type PriceCalculationType = "FIXED" | "PER_UNIT" | "PERCENTAGE";

export interface PriceRule {
  id: string;
  ruleCode: string;
  name: string;
  description: string | null;
  applianceTypeCode: string | null;
  conditionCode: PriceConditionCode;
  calculationType: PriceCalculationType;
  amount: number;
  priority: number;
  active: boolean;
  version: number;
  validFrom: string | null;
  validTo: string | null;
}

export interface PriceRuleRequest {
  ruleCode: string;
  name: string;
  description: string | null;
  applianceTypeCode: string | null;
  conditionCode: PriceConditionCode;
  calculationType: PriceCalculationType;
  amount: number;
  priority: number;
  active: boolean;
  validFrom: string | null;
  validTo: string | null;
}

export interface QuoteLine {
  lineCode: string;
  description: string;
  quantity: number;
  unitPrice: number;
  lineAmount: number;
}

export interface Quote {
  id: string;
  quoteNo: string;
  versionNo: number;
  status: "DRAFT" | "ISSUED" | "ACCEPTED" | "EXPIRED" | "CANCELLED";
  currency: string;
  subtotalAmount: number;
  discountAmount: number;
  taxAmount: number;
  totalAmount: number;
  issuedAt: string | null;
  expiresAt: string | null;
  lines: QuoteLine[];
}

export interface QuotePage {
  page: number;
  size: number;
  totalElements: number;
  items: Quote[];
}

export interface PricingRequestSummary {
  id: string;
  requestNo: string;
  status: string;
  customerName: string;
  customerEmail: string;
  applianceTypeCode: string;
  applianceTypeName: string;
  originAddress: string;
  destinationAddress: string;
  desiredStartAt: string;
  createdAt: string;
  latestQuoteStatus: string | null;
  latestQuoteTotalAmount: number | null;
}

export interface PricingRequestPage {
  page: number;
  size: number;
  totalElements: number;
  items: PricingRequestSummary[];
}

export interface TechnicianAssignmentPage {
  page: number;
  size: number;
  totalElements: number;
  content: TechnicianAssignment[];
}

export interface TechnicianAvailability {
  dayOfWeek: number;
  startTime: string;
  endTime: string;
}

export interface TechnicianProfile {
  id: string;
  approvalStatus: "PENDING" | "APPROVED" | "REJECTED";
  active: boolean;
  businessName: string;
  licenseInfo: string | null;
  serviceAreas: string[];
  applianceTypeCodes: string[];
  availability: TechnicianAvailability[];
}

export interface TechnicianProfileUpdateRequest {
  businessName: string;
  licenseInfo?: string;
  serviceAreas: string[];
  applianceTypeCodes: string[];
  availability: TechnicianAvailability[];
}

export interface PendingTechnicianReservation {
  reservationId: string;
  reservationNo: string;
  scheduledStartAt: string;
  scheduledEndAt: string;
  customerName: string;
  customerPhone: string;
  originAddress: string;
  destinationAddress: string;
  applianceTypes: string;
}

export interface PendingTechnicianReservationPage {
  page: number;
  size: number;
  totalElements: number;
  content: PendingTechnicianReservation[];
}

export interface TechnicianEvidence {
  id: string;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  uploadedAt: string;
}

export interface TechnicianCandidate {
  technicianProfileId: string;
  name: string;
  businessName: string;
  phone: string;
  activeAssignmentCount: number;
}

export type TechnicianApprovalStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface TechnicianAdminSummary {
  id: string;
  userId: string;
  name: string;
  email: string;
  phone: string;
  approvalStatus: TechnicianApprovalStatus;
  active: boolean;
  businessName: string;
  licenseInfo: string | null;
  serviceAreas: string | null;
  applianceTypeCodes: string | null;
  availability: string | null;
}

export interface TechnicianAdminPage {
  page: number;
  size: number;
  totalElements: number;
  content: TechnicianAdminSummary[];
}

export interface AuthUser {
  id: string;
  email: string;
  name: string;
  roles: string[];
}

export interface MemberProfile extends AuthUser {
  phone: string;
  status: "ACTIVE" | "SUSPENDED" | "WITHDRAWN";
}

export type AccountRole = "CUSTOMER" | "TECHNICIAN" | "ADMIN";

export interface AccountSummary extends MemberProfile {
  roles: AccountRole[];
}

export interface AccountPage {
  page: number;
  size: number;
  totalElements: number;
  content: AccountSummary[];
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
  accountType?: "CUSTOMER" | "TECHNICIAN";
}

export interface BootstrapAdminRequest {
  bootstrapKey: string;
  email: string;
  password: string;
  name: string;
  phone: string;
}

export interface UpdateProfileRequest {
  name: string;
  phone: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface ApiErrorResponse {
  status: number;
  code: string;
  message: string;
  fieldErrors?: Array<{ field: string; reason: string }>;
}
