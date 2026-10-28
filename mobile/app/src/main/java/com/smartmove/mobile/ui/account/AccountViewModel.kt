package com.smartmove.mobile.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartmove.mobile.AppContainer
import com.smartmove.mobile.data.model.AccountResponse
import com.smartmove.mobile.util.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountUiState(
    val loading: Boolean = true,
    val profile: AccountResponse? = null,
    val error: String? = null,
    val saving: Boolean = false,
    val message: String? = null,
    val passwordSaving: Boolean = false,
    val passwordError: String? = null,
)

class AccountViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val _state = MutableStateFlow(AccountUiState())
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val account = container.api.me()
                container.updateUser(account)
                _state.update { it.copy(loading = false, profile = account, error = null) }
            } catch (error: Exception) {
                _state.update { it.copy(loading = false, error = error.userMessage()) }
            }
        }
    }

    fun saveProfile(name: String, phone: String) {
        if (name.isBlank()) {
            _state.update { it.copy(error = "이름을 입력해 주세요.") }
            return
        }
        if (phone.isBlank()) {
            _state.update { it.copy(error = "연락처를 입력해 주세요.") }
            return
        }
        _state.update { it.copy(saving = true, error = null, message = null) }
        viewModelScope.launch {
            try {
                val account = container.api.updateProfile(name.trim(), phone.trim())
                container.updateUser(account)
                _state.update {
                    it.copy(
                        saving = false,
                        profile = account,
                        message = "회원 정보를 저장했습니다.",
                    )
                }
            } catch (error: Exception) {
                _state.update { it.copy(saving = false, error = error.userMessage()) }
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        val validationError = when {
            currentPassword.isBlank() -> "현재 비밀번호를 입력해 주세요."
            newPassword.length < 8 -> "새 비밀번호는 8자 이상이어야 합니다."
            newPassword != confirmPassword -> "새 비밀번호 확인이 일치하지 않습니다."
            else -> null
        }
        if (validationError != null) {
            _state.update { it.copy(passwordError = validationError) }
            return
        }
        _state.update { it.copy(passwordSaving = true, passwordError = null) }
        viewModelScope.launch {
            try {
                container.api.changePassword(currentPassword, newPassword)
                container.clearSession()
            } catch (error: Exception) {
                _state.update {
                    it.copy(passwordSaving = false, passwordError = error.userMessage())
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            runCatching { container.api.logout() }
            container.clearSession()
        }
    }

    fun clearMessages() {
        _state.update { it.copy(error = null, message = null, passwordError = null) }
    }
}
