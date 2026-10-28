package com.smartmove.mobile.ui.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartmove.mobile.AppContainer
import com.smartmove.mobile.data.model.ApplianceType
import com.smartmove.mobile.data.model.CreateInstallationRequest
import com.smartmove.mobile.data.model.CreateReservationRequest
import com.smartmove.mobile.data.model.InstallationHistoryItem
import com.smartmove.mobile.data.model.InstallationResponse
import com.smartmove.mobile.data.model.Quote
import com.smartmove.mobile.data.model.RescheduleReservationRequest
import com.smartmove.mobile.data.model.Reservation
import com.smartmove.mobile.util.DateTimes
import com.smartmove.mobile.util.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class NewRequestInput(
    val applianceTypeCode: String,
    val originAddress: String,
    val destinationAddress: String,
    val startDate: LocalDate,
    val startTime: LocalTime,
    val carryingDistanceMeters: String,
    val floorCount: String,
    val additionalPipingMeters: String,
)

data class CustomerUiState(
    val applianceTypes: List<ApplianceType> = emptyList(),
    val typesLoading: Boolean = true,
    val typesError: String? = null,
    val history: List<InstallationHistoryItem> = emptyList(),
    val historyLoading: Boolean = false,
    val historyError: String? = null,
    val historyPage: Int = 0,
    val historyTotal: Long = 0,
    val submitting: Boolean = false,
    val submitError: String? = null,
    val createdInstallation: InstallationResponse? = null,
)

data class CustomerDetailState(
    val visible: Boolean = false,
    val request: InstallationHistoryItem? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val quotes: List<Quote> = emptyList(),
    val reservations: List<Reservation> = emptyList(),
    val busy: Boolean = false,
    val message: String? = null,
    val availability: Boolean? = null,
    val availabilityMessage: String? = null,
    val checkedStartAt: String? = null,
    val checkedEndAt: String? = null,
) {
    val activeReservation: Reservation?
        get() = reservations.firstOrNull {
            it.status == "CONFIRMED" || it.status == "PENDING" || it.status == "RESCHEDULE_REQUESTED"
        }

    val editableReservation: Reservation?
        get() = activeReservation?.takeIf {
            it.status == "CONFIRMED" &&
                it.technicianAssignmentStatus != "WORK_STARTED" &&
                it.technicianAssignmentStatus != "WORK_COMPLETED" &&
                DateTimes.isFuture(it.scheduledStartAt)
        }

    val acceptedQuote: Quote?
        get() = quotes.firstOrNull { it.status == "ACCEPTED" }
}

class CustomerViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val _state = MutableStateFlow(CustomerUiState())
    val state: StateFlow<CustomerUiState> = _state.asStateFlow()

    private val _detail = MutableStateFlow(CustomerDetailState())
    val detail: StateFlow<CustomerDetailState> = _detail.asStateFlow()

    init {
        loadApplianceTypes()
        refreshHistory(showLoading = true)
    }

    fun loadApplianceTypes() {
        _state.update { it.copy(typesLoading = true, typesError = null) }
        viewModelScope.launch {
            try {
                val types = container.api.applianceTypes()
                _state.update {
                    it.copy(applianceTypes = types, typesLoading = false)
                }
            } catch (error: Exception) {
                _state.update {
                    it.copy(typesLoading = false, typesError = error.userMessage())
                }
            }
        }
    }

    fun refreshHistory(page: Int = _state.value.historyPage, showLoading: Boolean = true) {
        if (showLoading) {
            _state.update { it.copy(historyLoading = true, historyError = null) }
        }
        viewModelScope.launch {
            try {
                val result = container.api.installations(page = page, size = PAGE_SIZE)
                _state.update {
                    it.copy(
                        history = result.content,
                        historyPage = result.page,
                        historyTotal = result.totalElements,
                        historyLoading = false,
                        historyError = null,
                    )
                }
            } catch (error: Exception) {
                _state.update {
                    it.copy(historyLoading = false, historyError = error.userMessage())
                }
            }
        }
    }

    fun submitRequest(input: NewRequestInput) {
        val validationError = validateRequest(input)
        if (validationError != null) {
            _state.update { it.copy(submitError = validationError) }
            return
        }
        val carryingDistance = input.carryingDistanceMeters.trim().ifBlank { "0" }.toIntOrNull() ?: 0
        val floorCount = input.floorCount.trim().ifBlank { "0" }.toIntOrNull() ?: 0
        val piping = input.additionalPipingMeters.trim().ifBlank { "0" }.toDoubleOrNull() ?: 0.0

        _state.update { it.copy(submitting = true, submitError = null, createdInstallation = null) }
        viewModelScope.launch {
            try {
                val created = container.api.createInstallation(
                    CreateInstallationRequest(
                        applianceTypeCode = input.applianceTypeCode,
                        originAddress = input.originAddress.trim(),
                        destinationAddress = input.destinationAddress.trim(),
                        desiredStartAt = DateTimes.toIso(input.startDate, input.startTime),
                        carryingDistanceMeters = carryingDistance,
                        floorCount = floorCount,
                        additionalPipingMeters = piping,
                    )
                )
                _state.update {
                    it.copy(submitting = false, createdInstallation = created, submitError = null)
                }
                refreshHistory(page = 0, showLoading = false)
            } catch (error: Exception) {
                _state.update {
                    it.copy(submitting = false, submitError = error.userMessage())
                }
            }
        }
    }

    fun consumeCreatedInstallation() {
        _state.update { it.copy(createdInstallation = null) }
    }

    fun clearSubmitError() {
        _state.update { it.copy(submitError = null) }
    }

    fun openDetail(item: InstallationHistoryItem) {
        _detail.value = CustomerDetailState(
            visible = true,
            request = item,
            loading = true,
        )
        viewModelScope.launch {
            loadDetailData(item.id)
        }
    }

    fun closeDetail() {
        _detail.value = CustomerDetailState()
    }

    fun acceptQuote(quoteId: String) {
        val request = _detail.value.request ?: return
        _detail.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch {
            try {
                container.api.acceptQuote(quoteId)
                _detail.update { it.copy(message = "견적을 수락했습니다. 이제 예약할 수 있습니다.") }
                loadDetailData(request.id, keepMessage = true)
                refreshHistory(page = _state.value.historyPage, showLoading = false)
            } catch (error: Exception) {
                _detail.update { it.copy(busy = false, error = error.userMessage()) }
            }
        }
    }

    fun checkAvailability(
        startDate: LocalDate,
        startTime: LocalTime,
        endDate: LocalDate,
        endTime: LocalTime,
    ) {
        val request = _detail.value.request ?: return
        val activeReservation = _detail.value.activeReservation
        if (activeReservation != null && _detail.value.editableReservation == null) {
            _detail.update {
                it.copy(
                    error = "현재 예약 상태에서는 일정을 변경할 수 없습니다. 관리자에게 문의해 주세요.",
                    availability = null,
                )
            }
            return
        }
        val validationError = validateInterval(startDate, startTime, endDate, endTime)
        if (validationError != null) {
            _detail.update { it.copy(error = validationError, availability = null) }
            return
        }
        val startAt = DateTimes.toIso(startDate, startTime)
        val endAt = DateTimes.toIso(endDate, endTime)
        _detail.update {
            it.copy(
                busy = true,
                error = null,
                message = null,
                availability = null,
                availabilityMessage = null,
            )
        }
        viewModelScope.launch {
            try {
                val result = container.api.checkAvailability(
                    moveRequestId = request.id,
                    startAt = startAt,
                    endAt = endAt,
                    reservationId = _detail.value.editableReservation?.id,
                )
                _detail.update {
                    it.copy(
                        busy = false,
                        availability = result.available,
                        availabilityMessage = if (result.available) {
                            "선택한 일정으로 예약할 수 있습니다."
                        } else {
                            "이 설치 요청에는 이미 활성 예약이 있습니다."
                        },
                        checkedStartAt = if (result.available) startAt else null,
                        checkedEndAt = if (result.available) endAt else null,
                    )
                }
            } catch (error: Exception) {
                _detail.update {
                    it.copy(busy = false, availability = null, error = error.userMessage())
                }
            }
        }
    }

    fun saveReservation(
        startDate: LocalDate,
        startTime: LocalTime,
        endDate: LocalDate,
        endTime: LocalTime,
        memo: String,
    ) {
        val detail = _detail.value
        val request = detail.request ?: return
        val activeReservation = detail.editableReservation
        if (detail.activeReservation != null && activeReservation == null) {
            _detail.update {
                it.copy(error = "현재 예약 상태에서는 일정을 변경할 수 없습니다. 관리자에게 문의해 주세요.")
            }
            return
        }
        val validationError = validateInterval(startDate, startTime, endDate, endTime)
        if (validationError != null) {
            _detail.update { it.copy(error = validationError) }
            return
        }
        val startAt = DateTimes.toIso(startDate, startTime)
        val endAt = DateTimes.toIso(endDate, endTime)
        if (detail.availability != true ||
            detail.checkedStartAt != startAt ||
            detail.checkedEndAt != endAt
        ) {
            _detail.update { it.copy(error = "먼저 예약 가능 여부를 확인해 주세요.") }
            return
        }

        if (activeReservation == null) {
            val acceptedQuote = detail.acceptedQuote
            if (acceptedQuote == null) {
                _detail.update { it.copy(error = "예약하려면 먼저 견적을 수락해 주세요.") }
                return
            }
            _detail.update { it.copy(busy = true, error = null, message = null) }
            viewModelScope.launch {
                try {
                    container.api.createReservation(
                        CreateReservationRequest(
                            moveRequestId = request.id,
                            quoteId = acceptedQuote.id,
                            scheduledStartAt = startAt,
                            scheduledEndAt = endAt,
                            customerMemo = memo.trim().ifBlank { null },
                        ),
                        idempotencyKey = UUID.randomUUID().toString(),
                    )
                    _detail.update {
                        it.copy(
                            busy = false,
                            message = "예약이 확정되었습니다.",
                            availability = null,
                            availabilityMessage = null,
                            checkedStartAt = null,
                            checkedEndAt = null,
                        )
                    }
                    loadDetailData(request.id, keepMessage = true)
                    refreshHistory(page = _state.value.historyPage, showLoading = false)
                } catch (error: Exception) {
                    _detail.update { it.copy(busy = false, error = error.userMessage()) }
                }
            }
        } else {
            _detail.update { it.copy(busy = true, error = null, message = null) }
            viewModelScope.launch {
                try {
                    container.api.rescheduleReservation(
                        activeReservation.id,
                        RescheduleReservationRequest(startAt, endAt),
                    )
                    _detail.update {
                        it.copy(
                            busy = false,
                            message = "예약 일정을 변경했습니다.",
                            availability = null,
                            availabilityMessage = null,
                            checkedStartAt = null,
                            checkedEndAt = null,
                        )
                    }
                    loadDetailData(request.id, keepMessage = true)
                    refreshHistory(page = _state.value.historyPage, showLoading = false)
                } catch (error: Exception) {
                    _detail.update { it.copy(busy = false, error = error.userMessage()) }
                }
            }
        }
    }

    fun cancelReservation(reservationId: String) {
        val request = _detail.value.request ?: return
        if (_detail.value.editableReservation?.id != reservationId) {
            _detail.update {
                it.copy(error = "현재 예약 상태에서는 예약을 취소할 수 없습니다. 관리자에게 문의해 주세요.")
            }
            return
        }
        _detail.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch {
            try {
                container.api.cancelReservation(reservationId)
                _detail.update {
                    it.copy(
                        busy = false,
                        message = "예약을 취소했습니다. 수락한 견적으로 다시 예약할 수 있습니다.",
                        availability = null,
                        availabilityMessage = null,
                        checkedStartAt = null,
                        checkedEndAt = null,
                    )
                }
                loadDetailData(request.id, keepMessage = true)
                refreshHistory(page = _state.value.historyPage, showLoading = false)
            } catch (error: Exception) {
                _detail.update { it.copy(busy = false, error = error.userMessage()) }
            }
        }
    }

    fun clearDetailAvailability() {
        _detail.update {
            it.copy(
                availability = null,
                availabilityMessage = null,
                checkedStartAt = null,
                checkedEndAt = null,
            )
        }
    }

    fun clearDetailMessages() {
        _detail.update { it.copy(error = null, message = null) }
    }

    private suspend fun loadDetailData(requestId: String, keepMessage: Boolean = false) {
        val previousMessage = if (keepMessage) _detail.value.message else null
        var message: String? = null
        val quotes = try {
            container.api.quotes(requestId).items
        } catch (error: Exception) {
            message = error.userMessage()
            emptyList()
        }
        val reservations = try {
            container.api.reservations(requestId).items
        } catch (error: Exception) {
            if (message == null) message = error.userMessage()
            emptyList()
        }
        _detail.update {
            it.copy(
                loading = false,
                busy = false,
                quotes = quotes,
                reservations = reservations,
                error = message,
                message = previousMessage,
            )
        }
    }

    private fun validateRequest(input: NewRequestInput): String? {
        if (input.applianceTypeCode.isBlank()) return "가전제품 종류를 선택해 주세요."
        if (input.originAddress.isBlank() || input.destinationAddress.isBlank()) {
            return "이전 주소와 설치 주소를 모두 입력해 주세요."
        }
        val carryingDistance = input.carryingDistanceMeters.trim().ifBlank { "0" }.toIntOrNull()
            ?: return "운반 거리는 0 이상의 숫자로 입력해 주세요."
        val floorCount = input.floorCount.trim().ifBlank { "0" }.toIntOrNull()
            ?: return "작업 층수는 0 이상의 숫자로 입력해 주세요."
        val piping = input.additionalPipingMeters.trim().ifBlank { "0" }.toDoubleOrNull()
            ?: return "추가 배관 길이는 0 이상의 숫자로 입력해 주세요."
        if (carryingDistance !in 0..10000) return "운반 거리는 0~10,000m 범위로 입력해 주세요."
        if (floorCount !in 0..200) return "작업 층수는 0~200층 범위로 입력해 주세요."
        if (piping < 0 || piping > 1000) return "추가 배관 길이는 0~1,000m 범위로 입력해 주세요."
        val desiredAt = DateTimes.toIso(input.startDate, input.startTime)
        if (!DateTimes.isFuture(desiredAt)) return "현재 시각 이후의 희망 날짜와 시간을 선택해 주세요."
        return null
    }

    private fun validateInterval(
        startDate: LocalDate,
        startTime: LocalTime,
        endDate: LocalDate,
        endTime: LocalTime,
    ): String? {
        if (startDate != endDate) return "예약 시작과 종료는 같은 날짜로 선택해 주세요."
        if (!endTime.isAfter(startTime)) return "종료 시각은 시작 시각 이후여야 합니다."
        if (!DateTimes.isFuture(DateTimes.toIso(startDate, startTime))) {
            return "현재 시각 이후의 일정을 선택해 주세요."
        }
        return null
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
