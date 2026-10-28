package com.smartmove.mobile.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartmove.mobile.AppContainer
import com.smartmove.mobile.util.ServerUrl
import com.smartmove.mobile.util.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
)

class AuthViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    val currentBaseUrl: String
        get() = container.session.value.baseUrl

    fun login(email: String, password: String, serverUrl: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = AuthUiState(error = "이메일과 비밀번호를 입력해 주세요.")
            return
        }
        val normalizedUrl = ServerUrl.normalize(serverUrl, container.session.value.baseUrl)
        container.saveBaseUrl(normalizedUrl)
        _state.value = AuthUiState(loading = true)
        viewModelScope.launch {
            try {
                val response = container.api.login(email.trim(), password)
                container.saveAuth(response)
                _state.value = AuthUiState()
            } catch (error: Exception) {
                _state.value = AuthUiState(error = error.userMessage())
            }
        }
    }

    fun signup(
        email: String,
        password: String,
        name: String,
        phone: String,
        accountType: String,
        serverUrl: String,
    ) {
        val validationError = when {
            email.isBlank() -> "이메일을 입력해 주세요."
            password.length < 8 -> "비밀번호는 8자 이상이어야 합니다."
            name.isBlank() -> "이름을 입력해 주세요."
            phone.isBlank() -> "연락처를 입력해 주세요."
            else -> null
        }
        if (validationError != null) {
            _state.value = AuthUiState(error = validationError)
            return
        }

        val normalizedUrl = ServerUrl.normalize(serverUrl, container.session.value.baseUrl)
        container.saveBaseUrl(normalizedUrl)
        _state.value = AuthUiState(loading = true)
        viewModelScope.launch {
            try {
                val response = container.api.signup(
                    email = email.trim(),
                    password = password,
                    name = name.trim(),
                    phone = phone.trim(),
                    accountType = accountType,
                )
                container.saveAuth(response)
                _state.value = AuthUiState()
            } catch (error: Exception) {
                _state.value = AuthUiState(error = error.userMessage())
            }
        }
    }

    fun saveServerUrl(serverUrl: String) {
        val normalized = ServerUrl.normalize(serverUrl, container.session.value.baseUrl)
        container.saveBaseUrl(normalized)
        _state.update { it.copy(error = null) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
