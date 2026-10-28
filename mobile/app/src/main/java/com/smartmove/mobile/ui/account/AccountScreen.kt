package com.smartmove.mobile.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartmove.mobile.AppContainer
import com.smartmove.mobile.BuildConfig
import com.smartmove.mobile.data.model.UserRole
import com.smartmove.mobile.data.model.UserSummary
import com.smartmove.mobile.ui.AppViewModelFactory
import com.smartmove.mobile.ui.common.BrandMark
import com.smartmove.mobile.ui.common.DividerSpacer
import com.smartmove.mobile.ui.common.ErrorBanner
import com.smartmove.mobile.ui.common.InfoBanner
import com.smartmove.mobile.ui.common.InfoRow
import com.smartmove.mobile.ui.common.LoadingState
import com.smartmove.mobile.ui.common.SectionHeading
import com.smartmove.mobile.ui.common.StatusTone
import com.smartmove.mobile.ui.common.SurfaceCard
import com.smartmove.mobile.ui.common.Tag
import com.smartmove.mobile.ui.theme.Ink
import com.smartmove.mobile.ui.theme.InkMuted

@Composable
fun AccountScreen(
    container: AppContainer,
    user: UserSummary,
    currentRole: UserRole,
    factory: AppViewModelFactory,
    onSwitchRole: (UserRole) -> Unit,
) {
    val viewModel: AccountViewModel = viewModel(
        key = "account-${user.id}",
        factory = factory,
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val session by container.session.collectAsStateWithLifecycle()

    var name by rememberSaveable { mutableStateOf(user.name) }
    var phone by rememberSaveable { mutableStateOf("") }
    var showPasswordForm by rememberSaveable { mutableStateOf(false) }
    var currentPassword by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }
    LaunchedEffect(state.profile?.name, state.profile?.phone) {
        state.profile?.let {
            name = it.name
            phone = it.phone
        }
    }

    val otherRole = if (currentRole == UserRole.CUSTOMER) {
        UserRole.TECHNICIAN
    } else {
        UserRole.CUSTOMER
    }
    val errorMessage = state.error
    val infoMessage = state.message
    val passwordError = state.passwordError

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeading(
                eyebrow = "ACCOUNT",
                title = "내 계정",
                description = "회원 정보와 보안 설정을 관리합니다.",
            )
        }

        if (errorMessage != null) {
            item {
                ErrorBanner(
                    message = errorMessage,
                    onDismiss = viewModel::clearMessages,
                )
            }
        }
        if (infoMessage != null) {
            item {
                InfoBanner(
                    message = infoMessage,
                    tone = StatusTone.Positive,
                )
            }
        }

        item {
            SurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BrandMark(size = 48)
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = state.profile?.name ?: user.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = Ink,
                        )
                        Text(
                            text = state.profile?.email ?: user.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = InkMuted,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    user.roles.forEach { roleValue ->
                        Tag(
                            text = UserRole.from(roleValue)?.label ?: roleValue,
                            prominent = roleValue == currentRole.apiValue,
                        )
                    }
                }
                DividerSpacer()
                InfoRow("계정 상태", accountStatusLabel(state.profile?.status))
                Spacer(Modifier.height(6.dp))
                InfoRow("서버", session.baseUrl)
                Spacer(Modifier.height(6.dp))
                InfoRow("앱 버전", BuildConfig.VERSION_NAME)
            }
        }

        if (state.loading && state.profile == null) {
            item { LoadingState("회원 정보를 불러오는 중입니다.") }
        }

        item {
            SurfaceCard {
                Text(
                    text = "회원 정보",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("이름") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("연락처") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.saveProfile(name, phone) },
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("회원 정보 저장")
                    }
                }
            }
        }

        if (user.hasRole(otherRole)) {
            item {
                SurfaceCard {
                    Text(
                        text = "화면 전환",
                        style = MaterialTheme.typography.titleMedium,
                        color = Ink,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "이 계정은 고객과 설치기사 역할을 모두 가지고 있습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkMuted,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { onSwitchRole(otherRole) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("${otherRole.label} 화면으로 전환")
                    }
                }
            }
        }

        item {
            SurfaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "비밀번호 변경",
                        style = MaterialTheme.typography.titleMedium,
                        color = Ink,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { showPasswordForm = !showPasswordForm }) {
                        Text(if (showPasswordForm) "닫기" else "변경")
                    }
                }
                if (showPasswordForm) {
                    InfoBanner(
                        message = "비밀번호를 변경하면 다시 로그인해야 합니다.",
                        tone = StatusTone.Warning,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("현재 비밀번호") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("새 비밀번호") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        supportingText = { Text("8자 이상 입력해 주세요.") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("새 비밀번호 확인") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (passwordError != null) {
                        Spacer(Modifier.height(10.dp))
                        ErrorBanner(message = passwordError)
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            viewModel.changePassword(
                                currentPassword = currentPassword,
                                newPassword = newPassword,
                                confirmPassword = confirmPassword,
                            )
                        },
                        enabled = !state.passwordSaving,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.passwordSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text("비밀번호 변경")
                        }
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = viewModel::logout,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                Text("로그아웃", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun accountStatusLabel(status: String?): String = when (status) {
    "ACTIVE" -> "활성"
    "SUSPENDED" -> "정지"
    "WITHDRAWN" -> "탈퇴"
    null -> "확인 중"
    else -> status
}
