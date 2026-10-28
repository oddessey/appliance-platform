package com.smartmove.mobile.data.model

import kotlinx.serialization.Serializable

enum class UserRole(val apiValue: String, val label: String) {
    CUSTOMER("CUSTOMER", "고객"),
    TECHNICIAN("TECHNICIAN", "기사");

    companion object {
        fun from(value: String): UserRole? = entries.firstOrNull { it.apiValue == value }
    }
}

@Serializable
data class UserSummary(
    val id: String,
    val email: String,
    val name: String,
    val roles: List<String> = emptyList(),
) {
    fun hasRole(role: UserRole): Boolean = roles.contains(role.apiValue)

    fun availableRole(): UserRole = when {
        hasRole(UserRole.CUSTOMER) -> UserRole.CUSTOMER
        hasRole(UserRole.TECHNICIAN) -> UserRole.TECHNICIAN
        else -> UserRole.CUSTOMER
    }
}

@Serializable
data class AuthResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long = 0,
    val user: UserSummary,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class SignupRequest(
    val email: String,
    val password: String,
    val name: String,
    val phone: String,
    val accountType: String,
)

@Serializable
data class AccountResponse(
    val id: String,
    val email: String,
    val name: String,
    val phone: String = "",
    val status: String = "ACTIVE",
    val roles: List<String> = emptyList(),
) {
    fun toUserSummary(): UserSummary = UserSummary(id = id, email = email, name = name, roles = roles)
}

@Serializable
data class UpdateProfileRequest(
    val name: String,
    val phone: String,
)

@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
)

@Serializable
data class FieldError(
    val field: String? = null,
    val reason: String? = null,
)

@Serializable
data class ApiErrorBody(
    val status: Int = 0,
    val code: String = "",
    val message: String = "",
    val fieldErrors: List<FieldError> = emptyList(),
)

@Serializable
data class ApplianceType(
    val code: String,
    val name: String,
)

@Serializable
data class CreateInstallationRequest(
    val applianceTypeCode: String,
    val originAddress: String,
    val destinationAddress: String,
    val desiredStartAt: String,
    val carryingDistanceMeters: Int = 0,
    val floorCount: Int = 0,
    val additionalPipingMeters: Double = 0.0,
)

@Serializable
data class InstallationResponse(
    val id: String,
    val requestNo: String,
    val status: String,
    val createdAt: String? = null,
)

@Serializable
data class InstallationHistoryItem(
    val id: String,
    val requestNo: String,
    val status: String,
    val applianceTypeCode: String = "",
    val applianceTypeName: String = "",
    val originAddress: String = "",
    val destinationAddress: String = "",
    val desiredStartAt: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class InstallationHistoryPage(
    val page: Int = 0,
    val size: Int = 20,
    val totalElements: Long = 0,
    val content: List<InstallationHistoryItem> = emptyList(),
)

@Serializable
data class QuoteLine(
    val lineCode: String = "",
    val description: String = "",
    val quantity: Double = 0.0,
    val unitPrice: Double = 0.0,
    val lineAmount: Double = 0.0,
)

@Serializable
data class Quote(
    val id: String,
    val quoteNo: String,
    val versionNo: Int = 1,
    val status: String = "DRAFT",
    val currency: String = "KRW",
    val subtotalAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val issuedAt: String? = null,
    val expiresAt: String? = null,
    val lines: List<QuoteLine> = emptyList(),
)

@Serializable
data class QuotePage(
    val page: Int = 0,
    val size: Int = 20,
    val totalElements: Long = 0,
    val items: List<Quote> = emptyList(),
)

@Serializable
data class Reservation(
    val id: String,
    val reservationNo: String,
    val moveRequestId: String = "",
    val requestNo: String = "",
    val quoteId: String = "",
    val quoteNo: String = "",
    val status: String = "PENDING",
    val scheduledStartAt: String,
    val scheduledEndAt: String,
    val customerMemo: String? = null,
    val confirmedAt: String? = null,
    val cancelledAt: String? = null,
    val createdAt: String? = null,
    val customerName: String = "",
    val customerPhone: String = "",
    val originAddress: String = "",
    val destinationAddress: String = "",
    val quoteTotalAmount: Double = 0.0,
    val technicianAssignmentStatus: String? = null,
    val technicianAssignmentId: String? = null,
    val technicianName: String? = null,
)

@Serializable
data class ReservationPage(
    val page: Int = 0,
    val size: Int = 20,
    val totalElements: Long = 0,
    val items: List<Reservation> = emptyList(),
)

@Serializable
data class ReservationAvailability(
    val moveRequestId: String = "",
    val available: Boolean = false,
    val scheduledStartAt: String? = null,
    val scheduledEndAt: String? = null,
)

@Serializable
data class CreateReservationRequest(
    val moveRequestId: String,
    val quoteId: String,
    val scheduledStartAt: String,
    val scheduledEndAt: String,
    val customerMemo: String? = null,
)

@Serializable
data class RescheduleReservationRequest(
    val scheduledStartAt: String,
    val scheduledEndAt: String,
)

@Serializable
data class TechnicianAssignment(
    val id: String,
    val status: String,
    val reservationNo: String,
    val scheduledStartAt: String,
    val scheduledEndAt: String,
    val customerName: String = "",
    val customerPhone: String = "",
    val originAddress: String = "",
    val destinationAddress: String = "",
    val applianceTypes: String = "",
    val customerMemo: String? = null,
    val quoteNo: String? = null,
    val quoteTotalAmount: Double? = null,
)

@Serializable
data class TechnicianAssignmentPage(
    val page: Int = 0,
    val size: Int = 20,
    val totalElements: Long = 0,
    val content: List<TechnicianAssignment> = emptyList(),
)

@Serializable
data class RejectAssignmentRequest(
    val reason: String,
)

@Serializable
data class AssignmentEventRequest(
    val eventType: String,
    val note: String? = null,
)

@Serializable
data class TechnicianAvailability(
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
)

@Serializable
data class TechnicianProfile(
    val id: String? = null,
    val approvalStatus: String = "PENDING",
    val active: Boolean = false,
    val businessName: String = "",
    val licenseInfo: String? = null,
    val serviceAreas: List<String> = emptyList(),
    val applianceTypeCodes: List<String> = emptyList(),
    val availability: List<TechnicianAvailability> = emptyList(),
)

@Serializable
data class TechnicianProfileUpdateRequest(
    val businessName: String,
    val licenseInfo: String? = null,
    val serviceAreas: List<String>,
    val applianceTypeCodes: List<String>,
    val availability: List<TechnicianAvailability>,
)

@Serializable
data class TechnicianEvidence(
    val id: String,
    val originalFilename: String,
    val contentType: String = "",
    val sizeBytes: Long = 0,
    val uploadedAt: String? = null,
)
