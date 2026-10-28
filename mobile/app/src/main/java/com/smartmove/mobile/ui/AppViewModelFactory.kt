package com.smartmove.mobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.smartmove.mobile.AppContainer
import com.smartmove.mobile.ui.account.AccountViewModel
import com.smartmove.mobile.ui.auth.AuthViewModel
import com.smartmove.mobile.ui.customer.CustomerViewModel
import com.smartmove.mobile.ui.technician.TechnicianViewModel

class AppViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(SessionViewModel::class.java) -> SessionViewModel(container) as T
        modelClass.isAssignableFrom(AuthViewModel::class.java) -> AuthViewModel(container) as T
        modelClass.isAssignableFrom(CustomerViewModel::class.java) -> CustomerViewModel(container) as T
        modelClass.isAssignableFrom(TechnicianViewModel::class.java) -> TechnicianViewModel(container) as T
        modelClass.isAssignableFrom(AccountViewModel::class.java) -> AccountViewModel(container) as T
        else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
