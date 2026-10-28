package com.smartmove.mobile

import android.content.Context
import com.smartmove.mobile.data.SessionSnapshot
import com.smartmove.mobile.data.SessionStore
import com.smartmove.mobile.data.model.AccountResponse
import com.smartmove.mobile.data.model.AuthResponse
import com.smartmove.mobile.data.model.UserRole
import com.smartmove.mobile.data.model.UserSummary
import com.smartmove.mobile.data.remote.ApiClient
import com.smartmove.mobile.data.remote.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json

class AppContainer(context: Context) {
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    val sessionStore = SessionStore(context, json, BuildConfig.DEFAULT_API_BASE_URL)
    val apiClient = ApiClient(sessionStore, json)
    val api = ApiService(apiClient, json)

    private val _session = MutableStateFlow(sessionStore.load())
    val session: StateFlow<SessionSnapshot> = _session.asStateFlow()

    private val _selectedRole = MutableStateFlow(
        resolveRole(_session.value.user, sessionStore.loadSelectedRole()),
    )
    val selectedRole: StateFlow<UserRole> = _selectedRole.asStateFlow()

    init {
        apiClient.onUnauthorized = { clearSession() }
    }

    fun saveBaseUrl(url: String) {
        sessionStore.saveBaseUrl(url)
        _session.update { current ->
            current.copy(baseUrl = sessionStore.baseUrl())
        }
    }

    fun saveAuth(response: AuthResponse) {
        sessionStore.saveAuth(response, _session.value.baseUrl)
        _session.value = sessionStore.load()
        selectRole(resolveRole(response.user, sessionStore.loadSelectedRole()))
    }

    fun updateUser(user: UserSummary) {
        sessionStore.saveUser(user)
        _session.value = sessionStore.load()
        if (!user.hasRole(_selectedRole.value)) {
            selectRole(resolveRole(user, null))
        }
    }

    fun updateUser(account: AccountResponse) {
        updateUser(account.toUserSummary())
    }

    fun clearSession() {
        sessionStore.clearAuth()
        _session.value = sessionStore.load()
        _selectedRole.value = UserRole.CUSTOMER
    }

    fun selectRole(role: UserRole) {
        val user = _session.value.user ?: return
        if (!user.hasRole(role)) return
        _selectedRole.value = role
        sessionStore.saveSelectedRole(role)
    }

    private fun resolveRole(user: UserSummary?, preferred: UserRole?): UserRole {
        if (user == null) return UserRole.CUSTOMER
        if (preferred != null && user.hasRole(preferred)) return preferred
        return user.availableRole()
    }
}
