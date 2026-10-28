package com.smartmove.mobile.ui.technician

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartmove.mobile.AppContainer
import com.smartmove.mobile.data.model.ApplianceType
import com.smartmove.mobile.data.model.TechnicianAssignment
import com.smartmove.mobile.data.model.TechnicianAvailability
import com.smartmove.mobile.data.model.TechnicianEvidence
import com.smartmove.mobile.data.model.TechnicianProfile
import com.smartmove.mobile.data.model.TechnicianProfileUpdateRequest
import com.smartmove.mobile.data.remote.ApiException
import com.smartmove.mobile.util.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime

data class TechnicianUiState(
    val filter: String? = "PENDING",
    val assignments: List<TechnicianAssignment> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val page: Int = 0,
    val total: Long = 0,
    val busyAssignmentId: String? = null,
    val message: String? = null,
    val applianceTypes: List<ApplianceType> = emptyList(),
    val typesLoading: Boolean = true,
    val typesError: String? = null,
    val profile: TechnicianProfile? = null,
    val profileLoading: Boolean = true,
    val profileMissing: Boolean = false,
    val profileError: String? = null,
    val profileSaving: Boolean = false,
    val profileMessage: String? = null,
    val evidenceAssignment: TechnicianAssignment? = null,
    val evidence: List<TechnicianEvidence> = emptyList(),
    val evidenceLoading: Boolean = false,
    val evidenceError: String? = null,
    val evidenceUploading: Boolean = false,
    val evidenceMessage: String? = null,
) {
    val profileApproved: Boolean
        get() = profile?.approvalStatus == "APPROVED" && profile.active
}

class TechnicianViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val _state = MutableStateFlow(TechnicianUiState())
    val state: StateFlow<TechnicianUiState> = _state.asStateFlow()

    init {
        loadApplianceTypes()
        loadProfile()
    }

    fun loadApplianceTypes() {
        _state.update { it.copy(typesLoading = true, typesError = null) }
        viewModelScope.launch {
            try {
                val types = container.api.applianceTypes()
                _state.update { it.copy(applianceTypes = types, typesLoading = false) }
            } catch (error: Exception) {
                _state.update { it.copy(typesLoading = false, typesError = error.userMessage()) }
            }
        }
    }

    fun loadProfile() {
        _state.update { it.copy(profileLoading = true, profileError = null) }
        viewModelScope.launch {
            try {
                val profile = container.api.getProfile()
                val canWork = profile.approvalStatus == "APPROVED" && profile.active
                _state.update {
                    it.copy(
                        profile = profile,
                        profileLoading = false,
                        profileMissing = false,
                        profileError = null,
                        error = null,
                        assignments = if (canWork) it.assignments else emptyList(),
                        total = if (canWork) it.total else 0,
                        page = if (canWork) it.page else 0,
                        loading = if (canWork) it.loading else false,
                    )
                }
                if (canWork) {
                    refreshAssignments(showLoading = true)
                }
            } catch (error: ApiException) {
                if (error.status == 404) {
                    _state.update {
                        it.copy(
                            profile = null,
                            profileLoading = false,
                            profileMissing = true,
                            profileError = null,
                            loading = false,
                            assignments = emptyList(),
                            total = 0,
                            page = 0,
                            error = null,
                        )
                    }
                } else {
                    _state.update {
                        it.copy(profileLoading = false, profileError = error.userMessage())
                    }
                }
            } catch (error: Exception) {
                _state.update {
                    it.copy(profileLoading = false, profileError = error.userMessage())
                }
            }
        }
    }

    fun saveProfile(
        businessName: String,
        licenseInfo: String,
        serviceAreasText: String,
        applianceTypeCodes: Set<String>,
        availability: List<TechnicianAvailability>,
    ) {
        val serviceAreas = serviceAreasText
            .split(',', '\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val validationError = validateProfile(
            businessName = businessName,
            licenseInfo = licenseInfo,
            serviceAreas = serviceAreas,
            applianceTypeCodes = applianceTypeCodes,
            availability = availability,
        )
        if (validationError != null) {
            _state.update { it.copy(profileError = validationError) }
            return
        }

        _state.update { it.copy(profileSaving = true, profileError = null, profileMessage = null) }
        viewModelScope.launch {
            try {
                val profile = container.api.saveProfile(
                    TechnicianProfileUpdateRequest(
                        businessName = businessName.trim(),
                        licenseInfo = licenseInfo.trim().ifBlank { null },
                        serviceAreas = serviceAreas,
                        applianceTypeCodes = applianceTypeCodes.toList(),
                        availability = availability.map {
                            TechnicianAvailability(
                                dayOfWeek = it.dayOfWeek,
                                startTime = normalizeTime(it.startTime),
                                endTime = normalizeTime(it.endTime),
                            )
                        },
                    )
                )
                val canWork = profile.approvalStatus == "APPROVED" && profile.active
                _state.update {
                    it.copy(
                        profile = profile,
                        profileSaving = false,
                        profileMissing = false,
                        profileMessage = when {
                            canWork -> "프로필을 저장했습니다."
                            profile.approvalStatus == "APPROVED" ->
                                "프로필을 저장했습니다. 현재 비활성 상태입니다. 관리자에게 문의해 주세요."
                            else ->
                                "프로필을 저장했습니다. 관리자 승인 후 배정을 받을 수 있습니다."
                        },
                        assignments = if (canWork) it.assignments else emptyList(),
                        total = if (canWork) it.total else 0,
                        page = if (canWork) it.page else 0,
                        loading = if (canWork) it.loading else false,
                    )
                }
                if (canWork) {
                    refreshAssignments(showLoading = false)
                }
            } catch (error: Exception) {
                _state.update {
                    it.copy(profileSaving = false, profileError = error.userMessage())
                }
            }
        }
    }

    fun clearProfileMessages() {
        _state.update { it.copy(profileError = null, profileMessage = null) }
    }

    fun setFilter(status: String?) {
        if (_state.value.filter == status) return
        _state.update {
            it.copy(filter = status, page = 0, assignments = emptyList(), error = null)
        }
        refreshAssignments(showLoading = true)
    }

    fun refreshAssignments(showLoading: Boolean = true) {
        val current = _state.value
        if (current.profileMissing) return
        val profile = current.profile
        if (current.profileLoading || profile == null) return
        if (profile.approvalStatus != "APPROVED" || !profile.active) return
        if (showLoading) {
            _state.update { it.copy(loading = true, error = null) }
        }
        viewModelScope.launch {
            try {
                val result = container.api.assignments(
                    status = current.filter,
                    page = current.page,
                    size = PAGE_SIZE,
                )
                _state.update {
                    it.copy(
                        assignments = result.content,
                        page = result.page,
                        total = result.totalElements,
                        loading = false,
                        error = null,
                    )
                }
            } catch (error: Exception) {
                if (error is ApiException && error.code == "TECHNICIAN_PROFILE_UNAVAILABLE") {
                    _state.update {
                        it.copy(
                            loading = false,
                            error = "기사 프로필이 승인·활성 상태가 아닙니다. 관리자 승인 후 배정을 받을 수 있습니다.",
                            assignments = emptyList(),
                            total = 0,
                        )
                    }
                    loadProfile()
                } else {
                    _state.update { it.copy(loading = false, error = error.userMessage()) }
                }
            }
        }
    }

    fun changePage(page: Int) {
        if (page < 0) return
        _state.update { it.copy(page = page) }
        refreshAssignments(showLoading = true)
    }

    fun acceptAssignment(assignmentId: String) {
        performAssignmentAction(assignmentId, "배정을 수락했습니다.") {
            container.api.acceptAssignment(assignmentId)
        }
    }

    fun rejectAssignment(assignmentId: String, reason: String) {
        if (reason.isBlank()) {
            _state.update { it.copy(error = "거절 사유를 입력해 주세요.") }
            return
        }
        if (reason.length > 500) {
            _state.update { it.copy(error = "거절 사유는 500자 이내로 입력해 주세요.") }
            return
        }
        performAssignmentAction(assignmentId, "배정을 거절했습니다.") {
            container.api.rejectAssignment(assignmentId, reason.trim())
        }
    }

    fun advanceAssignment(assignmentId: String, eventType: String) {
        val message = when (eventType) {
            "ON_THE_WAY" -> "이동 중 상태로 변경했습니다."
            "WORK_STARTED" -> "작업을 시작했습니다."
            "WORK_COMPLETED" -> "작업을 완료했습니다."
            else -> "상태를 변경했습니다."
        }
        performAssignmentAction(assignmentId, message) {
            container.api.addAssignmentEvent(assignmentId, eventType)
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null, error = null) }
    }

    fun openEvidence(assignment: TechnicianAssignment) {
        _state.update {
            it.copy(
                evidenceAssignment = assignment,
                evidence = emptyList(),
                evidenceLoading = true,
                evidenceError = null,
                evidenceMessage = null,
            )
        }
        loadEvidence(assignment.id)
    }

    fun closeEvidence() {
        _state.update {
            it.copy(
                evidenceAssignment = null,
                evidence = emptyList(),
                evidenceLoading = false,
                evidenceError = null,
                evidenceUploading = false,
                evidenceMessage = null,
            )
        }
    }

    fun reportEvidenceError(message: String) {
        _state.update { it.copy(evidenceError = message) }
    }

    fun uploadEvidence(
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
    ) {
        val assignment = _state.value.evidenceAssignment ?: return
        if (mimeType !in SUPPORTED_EVIDENCE_TYPES) {
            _state.update { it.copy(evidenceError = "JPEG, PNG, WebP 이미지만 업로드할 수 있습니다.") }
            return
        }
        if (bytes.isEmpty()) {
            _state.update { it.copy(evidenceError = "이미지 파일을 읽지 못했습니다.") }
            return
        }
        if (bytes.size > MAX_EVIDENCE_BYTES) {
            _state.update { it.copy(evidenceError = "이미지는 10MB 이하만 업로드할 수 있습니다.") }
            return
        }
        _state.update { it.copy(evidenceUploading = true, evidenceError = null, evidenceMessage = null) }
        viewModelScope.launch {
            try {
                val uploaded = container.api.uploadEvidence(
                    assignmentId = assignment.id,
                    fileName = fileName,
                    mimeType = mimeType,
                    bytes = bytes,
                )
                _state.update {
                    it.copy(
                        evidence = listOf(uploaded) + it.evidence,
                        evidenceUploading = false,
                        evidenceMessage = "작업 사진을 업로드했습니다.",
                    )
                }
            } catch (error: Exception) {
                _state.update {
                    it.copy(evidenceUploading = false, evidenceError = error.userMessage())
                }
            }
        }
    }

    private fun loadEvidence(assignmentId: String) {
        viewModelScope.launch {
            try {
                val evidence = container.api.evidence(assignmentId)
                _state.update {
                    it.copy(evidence = evidence, evidenceLoading = false, evidenceError = null)
                }
            } catch (error: Exception) {
                _state.update {
                    it.copy(evidenceLoading = false, evidenceError = error.userMessage())
                }
            }
        }
    }

    private fun performAssignmentAction(
        assignmentId: String,
        successMessage: String,
        action: suspend () -> TechnicianAssignment,
    ) {
        _state.update { it.copy(busyAssignmentId = assignmentId, error = null, message = null) }
        viewModelScope.launch {
            try {
                action()
                _state.update {
                    it.copy(busyAssignmentId = null, message = successMessage)
                }
                refreshAssignments(showLoading = false)
            } catch (error: Exception) {
                _state.update {
                    it.copy(busyAssignmentId = null, error = error.userMessage())
                }
            }
        }
    }

    private fun validateProfile(
        businessName: String,
        licenseInfo: String,
        serviceAreas: List<String>,
        applianceTypeCodes: Set<String>,
        availability: List<TechnicianAvailability>,
    ): String? {
        if (businessName.isBlank()) return "상호명을 입력해 주세요."
        if (businessName.trim().length > 150) return "상호명은 150자 이내로 입력해 주세요."
        if (licenseInfo.isBlank()) {
            return "자격/면허 정보를 입력해 주세요. 배정 후보 등록에 필요합니다."
        }
        if (licenseInfo.trim().length > 2000) return "자격/면허 정보는 2,000자 이내로 입력해 주세요."
        if (serviceAreas.isEmpty()) return "서비스 지역을 하나 이상 입력해 주세요."
        if (serviceAreas.size > 50) return "서비스 지역은 최대 50개까지 입력할 수 있습니다."
        if (serviceAreas.any { it.length > 30 }) return "서비스 지역은 각각 30자 이내로 입력해 주세요."
        if (serviceAreas.distinct().size != serviceAreas.size) return "중복된 서비스 지역이 있습니다."
        if (applianceTypeCodes.isEmpty()) return "담당 가전제품을 하나 이상 선택해 주세요."
        if (applianceTypeCodes.size > 100) return "담당 가전제품은 최대 100개까지 선택할 수 있습니다."
        if (availability.isEmpty()) return "가능한 작업 시간을 하나 이상 추가해 주세요."
        if (availability.size > 50) return "가능한 작업 시간은 최대 50개까지 추가할 수 있습니다."

        val parsed = availability.map {
            val start = parseTime(it.startTime)
            val end = parseTime(it.endTime)
            if (start == null || end == null) return "작업 시간은 HH:mm 형식으로 입력해 주세요."
            if (!start.isBefore(end)) return "종료 시간은 시작 시간 이후여야 합니다."
            Triple(it.dayOfWeek, start, end)
        }
        for (index in parsed.indices) {
            val current = parsed[index]
            for (otherIndex in index + 1 until parsed.size) {
                val other = parsed[otherIndex]
                if (current.first == other.first &&
                    current.second.isBefore(other.third) &&
                    other.second.isBefore(current.third)
                ) {
                    return "같은 요일의 작업 시간이 겹칩니다."
                }
            }
        }
        return null
    }

    private fun parseTime(value: String): LocalTime? = runCatching {
        if (value.length == 5) LocalTime.parse("$value:00") else LocalTime.parse(value)
    }.getOrNull()

    private fun normalizeTime(value: String): String =
        if (value.length >= 5) value.take(5) else value

    private companion object {
        const val PAGE_SIZE = 20
        const val MAX_EVIDENCE_BYTES = 10 * 1024 * 1024
        val SUPPORTED_EVIDENCE_TYPES = setOf("image/jpeg", "image/png", "image/webp")
    }
}
