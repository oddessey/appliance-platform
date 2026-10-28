package com.smartmove.mobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartmove.mobile.AppContainer
import com.smartmove.mobile.data.remote.ApiException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class BootState {
    Checking,
    Ready,
}

class SessionViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val _bootState = MutableStateFlow(
        if (container.session.value.isAuthenticated) BootState.Checking else BootState.Ready,
    )
    val bootState: StateFlow<BootState> = _bootState.asStateFlow()

    init {
        validateSession()
    }

    fun validateSession() {
        val session = container.session.value
        if (!session.isAuthenticated) {
            _bootState.value = BootState.Ready
            return
        }

        viewModelScope.launch {
            try {
                container.updateUser(container.api.me())
            } catch (error: ApiException) {
                if (error.status == 401) {
                    container.clearSession()
                }
            } catch (_: Exception) {
                // Keep the cached session when the server is temporarily unreachable.
            } finally {
                _bootState.value = BootState.Ready
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            runCatching { container.api.logout() }
            container.clearSession()
        }
    }
}
