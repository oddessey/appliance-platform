package com.smartmove.mobile.data.remote

import com.smartmove.mobile.data.model.AccountResponse
import com.smartmove.mobile.data.model.ApplianceType
import com.smartmove.mobile.data.model.AssignmentEventRequest
import com.smartmove.mobile.data.model.AuthResponse
import com.smartmove.mobile.data.model.ChangePasswordRequest
import com.smartmove.mobile.data.model.CreateInstallationRequest
import com.smartmove.mobile.data.model.CreateReservationRequest
import com.smartmove.mobile.data.model.InstallationHistoryPage
import com.smartmove.mobile.data.model.InstallationResponse
import com.smartmove.mobile.data.model.LoginRequest
import com.smartmove.mobile.data.model.QuotePage
import com.smartmove.mobile.data.model.RejectAssignmentRequest
import com.smartmove.mobile.data.model.RescheduleReservationRequest
import com.smartmove.mobile.data.model.Reservation
import com.smartmove.mobile.data.model.ReservationAvailability
import com.smartmove.mobile.data.model.ReservationPage
import com.smartmove.mobile.data.model.SignupRequest
import com.smartmove.mobile.data.model.TechnicianAssignment
import com.smartmove.mobile.data.model.TechnicianAssignmentPage
import com.smartmove.mobile.data.model.TechnicianEvidence
import com.smartmove.mobile.data.model.TechnicianProfile
import com.smartmove.mobile.data.model.TechnicianProfileUpdateRequest
import com.smartmove.mobile.data.model.UpdateProfileRequest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ApiService(
    private val client: ApiClient,
    private val json: Json,
) {
    suspend fun login(email: String, password: String): AuthResponse = decode(
        client.send("POST", "auth/login", json.encodeToString(LoginRequest(email, password)))
    )

    suspend fun signup(
        email: String,
        password: String,
        name: String,
        phone: String,
        accountType: String,
    ): AuthResponse = decode(
        client.send(
            "POST",
            "auth/signup",
            json.encodeToString(SignupRequest(email, password, name, phone, accountType)),
        )
    )

    suspend fun logout() {
        client.sendNoContent("POST", "auth/logout")
    }

    suspend fun me(): AccountResponse = decode(client.get("auth/me"))

    suspend fun updateProfile(name: String, phone: String): AccountResponse = decode(
        client.send("PATCH", "users/me", json.encodeToString(UpdateProfileRequest(name, phone)))
    )

    suspend fun changePassword(currentPassword: String, newPassword: String) {
        client.sendNoContent(
            "PATCH",
            "users/me/password",
            json.encodeToString(ChangePasswordRequest(currentPassword, newPassword)),
        )
    }

    suspend fun applianceTypes(): List<ApplianceType> = decode(client.get("appliance-types"))

    suspend fun createInstallation(request: CreateInstallationRequest): InstallationResponse = decode(
        client.send("POST", "installations", json.encodeToString(request))
    )

    suspend fun installations(page: Int = 0, size: Int = 20): InstallationHistoryPage = decode(
        client.get(
            "installations",
            mapOf("page" to page.toString(), "size" to size.toString()),
        )
    )

    suspend fun quotes(installationId: String, page: Int = 0, size: Int = 20): QuotePage = decode(
        client.get(
            "installations/${encode(installationId)}/quotes",
            mapOf("page" to page.toString(), "size" to size.toString()),
        )
    )

    suspend fun acceptQuote(quoteId: String) {
        client.sendNoContent("POST", "quotes/${encode(quoteId)}/accept")
    }

    suspend fun reservations(moveRequestId: String, page: Int = 0, size: Int = 20): ReservationPage = decode(
        client.get(
            "reservations",
            mapOf(
                "moveRequestId" to moveRequestId,
                "page" to page.toString(),
                "size" to size.toString(),
            ),
        )
    )

    suspend fun checkAvailability(
        moveRequestId: String,
        startAt: String,
        endAt: String,
        reservationId: String? = null,
    ): ReservationAvailability = decode(
        client.get(
            "reservations/availability",
            mapOf(
                "moveRequestId" to moveRequestId,
                "startAt" to startAt,
                "endAt" to endAt,
                "reservationId" to reservationId,
            ),
        )
    )

    suspend fun createReservation(
        request: CreateReservationRequest,
        idempotencyKey: String,
    ): Reservation = decode(
        client.send(
            "POST",
            "reservations",
            json.encodeToString(request),
            headers = mapOf("Idempotency-Key" to idempotencyKey),
        )
    )

    suspend fun rescheduleReservation(
        reservationId: String,
        request: RescheduleReservationRequest,
    ): Reservation = decode(
        client.send(
            "POST",
            "reservations/${encode(reservationId)}/reschedule",
            json.encodeToString(request),
        )
    )

    suspend fun cancelReservation(reservationId: String) {
        client.sendNoContent("POST", "reservations/${encode(reservationId)}/cancel")
    }

    suspend fun assignments(
        status: String? = null,
        page: Int = 0,
        size: Int = 20,
    ): TechnicianAssignmentPage = decode(
        client.get(
            "technicians/me/assignments",
            mapOf(
                "status" to status,
                "page" to page.toString(),
                "size" to size.toString(),
            ),
        )
    )

    suspend fun acceptAssignment(id: String): TechnicianAssignment = decode(
        client.send("POST", "assignments/${encode(id)}/accept")
    )

    suspend fun rejectAssignment(id: String, reason: String): TechnicianAssignment = decode(
        client.send(
            "POST",
            "assignments/${encode(id)}/reject",
            json.encodeToString(RejectAssignmentRequest(reason)),
        )
    )

    suspend fun addAssignmentEvent(
        id: String,
        eventType: String,
        note: String? = null,
    ): TechnicianAssignment = decode(
        client.send(
            "POST",
            "assignments/${encode(id)}/events",
            json.encodeToString(AssignmentEventRequest(eventType, note)),
        )
    )

    suspend fun uploadEvidence(
        assignmentId: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
    ): TechnicianEvidence = decode(
        client.upload(
            "assignments/${encode(assignmentId)}/evidence",
            fileName,
            mimeType,
            bytes,
        )
    )

    suspend fun evidence(assignmentId: String): List<TechnicianEvidence> = decode(
        client.get("assignments/${encode(assignmentId)}/evidence")
    )

    suspend fun getProfile(): TechnicianProfile = decode(
        client.get("technicians/me/profile")
    )

    suspend fun saveProfile(request: TechnicianProfileUpdateRequest): TechnicianProfile = decode(
        client.send("PUT", "technicians/me/profile", json.encodeToString(request))
    )

    private inline fun <reified T> decode(raw: String): T = json.decodeFromString(raw)

    private fun encode(value: String): String = java.net.URLEncoder.encode(value, "UTF-8")
}
