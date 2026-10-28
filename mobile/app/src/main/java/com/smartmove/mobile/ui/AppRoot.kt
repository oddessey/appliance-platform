package com.smartmove.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartmove.mobile.AppContainer
import com.smartmove.mobile.data.model.UserRole
import com.smartmove.mobile.data.model.UserSummary
import com.smartmove.mobile.ui.auth.AuthScreen
import com.smartmove.mobile.ui.common.BrandLockup
import com.smartmove.mobile.ui.customer.CustomerShell
import com.smartmove.mobile.ui.technician.TechnicianShell
import com.smartmove.mobile.ui.theme.Canvas
import com.smartmove.mobile.ui.theme.InkMuted

@Composable
fun AppRoot(container: AppContainer) {
    val factory = remember(container) { AppViewModelFactory(container) }
    val sessionViewModel: SessionViewModel = viewModel(factory = factory)
    val session by container.session.collectAsStateWithLifecycle()
    val selectedRole by container.selectedRole.collectAsStateWithLifecycle()
    val bootState by sessionViewModel.bootState.collectAsStateWithLifecycle()

    val user = session.user
    when {
        session.isAuthenticated && user != null && bootState == BootState.Checking -> BootScreen()
        !session.isAuthenticated || user == null -> AuthScreen(
            container = container,
            factory = factory,
        )
        user.roles.none { UserRole.from(it) != null } -> UnsupportedAccountScreen(
            user = user,
            onLogout = sessionViewModel::logout,
        )
        else -> when (selectedRole) {
            UserRole.CUSTOMER -> CustomerShell(
                container = container,
                user = user,
                factory = factory,
                onSwitchRole = container::selectRole,
            )
            UserRole.TECHNICIAN -> TechnicianShell(
                container = container,
                user = user,
                factory = factory,
                onSwitchRole = container::selectRole,
            )
        }
    }
}

@Composable
private fun BootScreen() {
    Surface(color = Canvas, modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                BrandLockup()
                CircularProgressIndicator()
                Text(
                    text = "로그인 정보를 확인하고 있습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                )
            }
        }
    }
}

@Composable
private fun UnsupportedAccountScreen(
    user: UserSummary,
    onLogout: () -> Unit,
) {
    Surface(color = Canvas, modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                BrandLockup()
                Text(
                    text = "모바일 앱에서 지원하지 않는 계정입니다.",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "${user.email}\n고객 또는 설치기사 계정으로 로그인해 주세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = onLogout) {
                    Text("로그아웃")
                }
            }
        }
    }
}
