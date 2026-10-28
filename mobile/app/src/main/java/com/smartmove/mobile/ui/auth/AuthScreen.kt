package com.smartmove.mobile.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartmove.mobile.AppContainer
import com.smartmove.mobile.ui.AppViewModelFactory
import com.smartmove.mobile.ui.common.BrandLockup
import com.smartmove.mobile.ui.common.ErrorBanner
import com.smartmove.mobile.ui.common.HeroGradient
import com.smartmove.mobile.ui.common.InfoBanner
import com.smartmove.mobile.ui.common.SurfaceCard
import com.smartmove.mobile.ui.theme.Canvas
import com.smartmove.mobile.ui.theme.Ink
import com.smartmove.mobile.ui.theme.InkMuted

@Composable
fun AuthScreen(
    container: AppContainer,
    factory: AppViewModelFactory,
) {
    val viewModel: AuthViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsStateWithLifecycle()

    var mode by rememberSaveable { mutableStateOf("login") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var accountType by rememberSaveable { mutableStateOf("CUSTOMER") }
    var serverUrl by rememberSaveable { mutableStateOf(viewModel.currentBaseUrl) }
    var showServerSettings by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(mode) {
        viewModel.clearError()
    }

    Surface(color = Canvas, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 22.dp, vertical = 18.dp),
        ) {
            BrandLockup()
            Spacer(Modifier.height(22.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(HeroGradient))
                    .padding(24.dp),
            ) {
                Column {
                    Text(
                        text = "가전 이전설치,\n한 번에 해결하세요.",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "접수부터 견적, 방문 예약까지\n스마트무브에서 간편하게.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.82f),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            SurfaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = mode == "login",
                        onClick = { mode = "login" },
                        label = { Text("로그인") },
                    )
                    FilterChip(
                        selected = mode == "signup",
                        onClick = { mode = "signup" },
                        label = { Text("회원가입") },
                    )
                }
                Spacer(Modifier.height(16.dp))

                if (mode == "signup") {
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
                    Spacer(Modifier.height(10.dp))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("이메일") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("비밀번호") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    supportingText = if (mode == "signup") {
                        { Text("8자 이상 입력해 주세요.") }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (mode == "signup") {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "가입 유형",
                        style = MaterialTheme.typography.labelLarge,
                        color = Ink,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = accountType == "CUSTOMER",
                            onClick = { accountType = "CUSTOMER" },
                            label = { Text("고객") },
                        )
                        FilterChip(
                            selected = accountType == "TECHNICIAN",
                            onClick = { accountType = "TECHNICIAN" },
                            label = { Text("설치기사") },
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    InfoBanner(
                        message = if (accountType == "TECHNICIAN") {
                            "기사 계정은 가입 후 프로필 등록과 관리자 승인이 필요합니다."
                        } else {
                            "가입 즉시 설치 요청을 접수하고 견적을 받을 수 있습니다."
                        },
                    )
                }

                Spacer(Modifier.height(14.dp))
                TextButton(onClick = { showServerSettings = !showServerSettings }) {
                    Text(if (showServerSettings) "서버 주소 닫기" else "서버 주소 설정")
                }
                if (showServerSettings) {
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text("API 서버 주소") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        supportingText = {
                            Text("예: http://192.168.0.10:8080/api/v1/")
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (state.error != null) {
                    Spacer(Modifier.height(12.dp))
                    ErrorBanner(
                        message = state.error.orEmpty(),
                        onDismiss = viewModel::clearError,
                    )
                }

                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = {
                        if (mode == "login") {
                            viewModel.login(email, password, serverUrl)
                        } else {
                            viewModel.signup(
                                email = email,
                                password = password,
                                name = name,
                                phone = phone,
                                accountType = accountType,
                                serverUrl = serverUrl,
                            )
                        }
                    },
                    enabled = !state.loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    if (state.loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(
                            text = if (mode == "login") "로그인" else "가입하고 시작하기",
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = "설치기사 계정은 관리자 승인 후 배정을 받을 수 있습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
