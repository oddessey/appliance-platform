package com.smartmove.mobile.ui.customer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartmove.mobile.AppContainer
import com.smartmove.mobile.data.model.InstallationHistoryItem
import com.smartmove.mobile.data.model.Quote
import com.smartmove.mobile.data.model.Reservation
import com.smartmove.mobile.data.model.UserRole
import com.smartmove.mobile.data.model.UserSummary
import com.smartmove.mobile.ui.AppViewModelFactory
import com.smartmove.mobile.ui.account.AccountScreen
import com.smartmove.mobile.ui.common.BrandLockup
import com.smartmove.mobile.ui.common.DateField
import com.smartmove.mobile.ui.common.DividerSpacer
import com.smartmove.mobile.ui.common.EmptyState
import com.smartmove.mobile.ui.common.ErrorBanner
import com.smartmove.mobile.ui.common.InfoBanner
import com.smartmove.mobile.ui.common.InfoRow
import com.smartmove.mobile.ui.common.LoadingState
import com.smartmove.mobile.ui.common.QuoteLineRow
import com.smartmove.mobile.ui.common.SectionHeading
import com.smartmove.mobile.ui.common.StatusPill
import com.smartmove.mobile.ui.common.StatusTone
import com.smartmove.mobile.ui.common.SurfaceCard
import com.smartmove.mobile.ui.common.Tag
import com.smartmove.mobile.ui.common.TimeField
import com.smartmove.mobile.ui.common.toneForStatus
import com.smartmove.mobile.ui.theme.Canvas
import com.smartmove.mobile.ui.theme.Ink
import com.smartmove.mobile.ui.theme.InkMuted
import com.smartmove.mobile.util.DateTimes
import com.smartmove.mobile.util.Labels
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun CustomerShell(
    container: AppContainer,
    user: UserSummary,
    factory: AppViewModelFactory,
    onSwitchRole: (UserRole) -> Unit,
) {
    val viewModel: CustomerViewModel = viewModel(
        key = "customer-${user.id}",
        factory = factory,
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        containerColor = Canvas,
        topBar = { CustomerHeader(user.name) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("접수") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    label = { Text("내역") },
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("계정") },
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (tab) {
                0 -> CustomerNewRequestScreen(
                    state = state,
                    viewModel = viewModel,
                    onViewHistory = { tab = 1 },
                )
                1 -> CustomerHistoryScreen(
                    state = state,
                    onRefresh = { viewModel.refreshHistory() },
                    onOpen = viewModel::openDetail,
                    onNewRequest = { tab = 0 },
                    onPage = { page -> viewModel.refreshHistory(page = page) },
                )
                else -> AccountScreen(
                    container = container,
                    user = user,
                    currentRole = UserRole.CUSTOMER,
                    factory = factory,
                    onSwitchRole = onSwitchRole,
                )
            }
        }
    }

    if (detail.visible) {
        CustomerRequestDetailSheet(
            state = detail,
            viewModel = viewModel,
        )
    }
}

@Composable
private fun CustomerHeader(userName: String) {
    Surface(color = Canvas) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandLockup()
            Spacer(Modifier.weight(1f))
            Tag(text = "${userName}님", prominent = true)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerNewRequestScreen(
    state: CustomerUiState,
    viewModel: CustomerViewModel,
    onViewHistory: () -> Unit,
) {
    var resetKey by rememberSaveable { mutableIntStateOf(0) }
    val created = state.createdInstallation

    if (created != null) {
        AlertDialog(
            onDismissRequest = viewModel::consumeCreatedInstallation,
            title = { Text("접수 완료") },
            text = {
                Text(
                    "설치 요청이 접수되었습니다.\n접수번호 ${created.requestNo}\n" +
                        "견적이 발행되면 접수 내역에서 확인할 수 있습니다.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.consumeCreatedInstallation()
                        resetKey += 1
                        onViewHistory()
                    },
                ) {
                    Text("접수 내역")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.consumeCreatedInstallation()
                        resetKey += 1
                    },
                ) {
                    Text("추가 접수")
                }
            },
        )
    }

    key(resetKey) {
        CustomerNewRequestForm(
            state = state,
            onSubmit = viewModel::submitRequest,
            onClearError = viewModel::clearSubmitError,
            onRetryTypes = viewModel::loadApplianceTypes,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerNewRequestForm(
    state: CustomerUiState,
    onSubmit: (NewRequestInput) -> Unit,
    onClearError: () -> Unit,
    onRetryTypes: () -> Unit,
) {
    val defaultStart = remember { DateTimes.defaultStart() }
    var applianceCode by rememberSaveable { mutableStateOf("") }
    var originAddress by rememberSaveable { mutableStateOf("") }
    var destinationAddress by rememberSaveable { mutableStateOf("") }
    var startEpochDay by rememberSaveable { mutableStateOf(defaultStart.first.toEpochDay()) }
    var startMinuteOfDay by rememberSaveable {
        mutableStateOf(defaultStart.second.hour * 60 + defaultStart.second.minute)
    }
    var carryingDistance by rememberSaveable { mutableStateOf("") }
    var floorCount by rememberSaveable { mutableStateOf("") }
    var pipingMeters by rememberSaveable { mutableStateOf("") }
    var typeMenuExpanded by remember { mutableStateOf(false) }

    val startDate = LocalDate.ofEpochDay(startEpochDay)
    val startTime = LocalTime.of(startMinuteOfDay / 60, startMinuteOfDay % 60)
    val selectedType = state.applianceTypes.firstOrNull { it.code == applianceCode }

    LaunchedEffect(state.applianceTypes) {
        if (state.applianceTypes.isNotEmpty() &&
            state.applianceTypes.none { it.code == applianceCode }
        ) {
            applianceCode = state.applianceTypes.first().code
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionHeading(
                eyebrow = "NEW REQUEST",
                title = "가전 이전설치를 신청하세요",
                description = "주소와 희망 일정을 남기면 견적과 방문 예약을 도와드립니다.",
            )
        }

        item {
            SurfaceCard {
                Text(
                    text = "설치 정보",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                )
                Spacer(Modifier.height(12.dp))
                if (state.typesLoading) {
                    LoadingState("가전제품 목록을 불러오는 중입니다.")
                } else if (state.typesError != null) {
                    ErrorBanner(
                        message = state.typesError,
                        onRetry = onRetryTypes,
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = typeMenuExpanded,
                        onExpandedChange = { typeMenuExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = selectedType?.name.orEmpty(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("가전제품") },
                            placeholder = { Text("종류를 선택해 주세요") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                        )
                        ExposedDropdownMenu(
                            expanded = typeMenuExpanded,
                            onDismissRequest = { typeMenuExpanded = false },
                        ) {
                            state.applianceTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name) },
                                    onClick = {
                                        applianceCode = type.code
                                        typeMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = originAddress,
                    onValueChange = { originAddress = it },
                    label = { Text("이전 주소") },
                    placeholder = { Text("가전제품이 있는 주소") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = destinationAddress,
                    onValueChange = { destinationAddress = it },
                    label = { Text("설치 주소") },
                    placeholder = { Text("가전제품을 설치할 주소") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "주소에는 시·도와 시·군·구를 포함해 입력해 주세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "희망 일정",
                    style = MaterialTheme.typography.labelLarge,
                    color = Ink,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DateField(
                        label = "날짜",
                        date = startDate,
                        onDateChange = { startEpochDay = it.toEpochDay() },
                        modifier = Modifier.weight(1f),
                    )
                    TimeField(
                        label = "시간",
                        time = startTime,
                        onTimeChange = {
                            startMinuteOfDay = it.hour * 60 + it.minute
                        },
                        modifier = Modifier.weight(0.8f),
                    )
                }
            }
        }

        item {
            SurfaceCard {
                Text(
                    text = "추가 작업 조건",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "모르면 비워두세요. 상담 후 견적에 반영됩니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = carryingDistance,
                        onValueChange = { carryingDistance = it.filter(Char::isDigit) },
                        label = { Text("운반 거리(m)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = floorCount,
                        onValueChange = { floorCount = it.filter(Char::isDigit) },
                        label = { Text("작업 층수") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = pipingMeters,
                    onValueChange = { pipingMeters = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("추가 배관(m)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            InfoBanner(
                message = "접수 후 관리자가 견적을 발행합니다. 견적을 수락하면 방문 일정을 예약할 수 있습니다.",
            )
        }

        if (state.submitError != null) {
            item {
                ErrorBanner(
                    message = state.submitError,
                    onDismiss = onClearError,
                )
            }
        }

        item {
            Button(
                onClick = {
                    onSubmit(
                        NewRequestInput(
                            applianceTypeCode = applianceCode,
                            originAddress = originAddress,
                            destinationAddress = destinationAddress,
                            startDate = startDate,
                            startTime = startTime,
                            carryingDistanceMeters = carryingDistance,
                            floorCount = floorCount,
                            additionalPipingMeters = pipingMeters,
                        )
                    )
                },
                enabled = !state.submitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (state.submitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("설치 요청 접수", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun CustomerHistoryScreen(
    state: CustomerUiState,
    onRefresh: () -> Unit,
    onOpen: (InstallationHistoryItem) -> Unit,
    onNewRequest: () -> Unit,
    onPage: (Int) -> Unit,
) {
    LaunchedEffect(Unit) {
        onRefresh()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    SectionHeading(
                        eyebrow = "HISTORY",
                        title = "내 설치 요청",
                        description = "견적 확인과 방문 예약을 진행할 수 있습니다.",
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "새로고침")
                }
            }
        }

        if (state.historyError != null) {
            item {
                ErrorBanner(
                    message = state.historyError,
                    onRetry = onRefresh,
                )
            }
        }

        if (state.historyLoading && state.history.isEmpty()) {
            item { LoadingState("접수 내역을 불러오는 중입니다.") }
        } else if (!state.historyLoading && state.history.isEmpty()) {
            item {
                EmptyState(
                    title = "아직 접수한 요청이 없습니다.",
                    description = "가전 이전설치를 신청하면 이곳에서 진행 상황을 확인할 수 있습니다.",
                    actionLabel = "새 접수하기",
                    onAction = onNewRequest,
                )
            }
        } else {
            items(state.history, key = { it.id }) { item ->
                InstallationCard(item = item, onClick = { onOpen(item) })
            }
            if (state.historyTotal > PAGE_SIZE) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = { onPage(state.historyPage - 1) },
                            enabled = state.historyPage > 0,
                        ) {
                            Text("이전")
                        }
                        Text(
                            text = "${state.historyPage + 1} / ${pageCount(state.historyTotal)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkMuted,
                        )
                        TextButton(
                            onClick = { onPage(state.historyPage + 1) },
                            enabled = (state.historyPage + 1) * PAGE_SIZE < state.historyTotal,
                        ) {
                            Text("다음")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InstallationCard(
    item: InstallationHistoryItem,
    onClick: () -> Unit,
) {
    SurfaceCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.requestNo,
                    style = MaterialTheme.typography.labelMedium,
                    color = InkMuted,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.applianceTypeName.ifBlank { item.applianceTypeCode },
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                )
            }
            StatusPill(
                label = Labels.installationStatus(item.status),
                tone = toneForStatus(item.status),
            )
        }
        DividerSpacer()
        InfoRow("희망 일정", DateTimes.formatDateTime(item.desiredStartAt))
        Spacer(Modifier.height(6.dp))
        InfoRow("이전", item.originAddress)
        Spacer(Modifier.height(6.dp))
        InfoRow("설치", item.destinationAddress)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerRequestDetailSheet(
    state: CustomerDetailState,
    viewModel: CustomerViewModel,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val request = state.request ?: return
    var quoteToAccept by remember { mutableStateOf<Quote?>(null) }
    var reservationToCancel by remember { mutableStateOf<Reservation?>(null) }

    ModalBottomSheet(
        onDismissRequest = viewModel::closeDetail,
        sheetState = sheetState,
        containerColor = Canvas,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 36.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = request.requestNo,
                        style = MaterialTheme.typography.labelMedium,
                        color = InkMuted,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = request.applianceTypeName.ifBlank { request.applianceTypeCode },
                        style = MaterialTheme.typography.headlineSmall,
                        color = Ink,
                    )
                }
                StatusPill(
                    label = Labels.installationStatus(request.status),
                    tone = toneForStatus(request.status),
                )
                IconButton(onClick = viewModel::closeDetail) {
                    Icon(Icons.Default.Close, contentDescription = "닫기")
                }
            }
            Spacer(Modifier.height(12.dp))
            if (state.busy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
            }

            SurfaceCard {
                InfoRow("희망 일정", DateTimes.formatDateTime(request.desiredStartAt))
                Spacer(Modifier.height(6.dp))
                InfoRow("이전 주소", request.originAddress)
                Spacer(Modifier.height(6.dp))
                InfoRow("설치 주소", request.destinationAddress)
                if (request.createdAt != null) {
                    Spacer(Modifier.height(6.dp))
                    InfoRow("접수일", DateTimes.formatDateTime(request.createdAt))
                }
            }

            if (state.error != null) {
                Spacer(Modifier.height(10.dp))
                ErrorBanner(
                    message = state.error,
                    onDismiss = viewModel::clearDetailMessages,
                )
            }
            if (state.message != null) {
                Spacer(Modifier.height(10.dp))
                InfoBanner(
                    message = state.message,
                    tone = StatusTone.Positive,
                )
            }

            Spacer(Modifier.height(18.dp))
            SectionHeading(
                eyebrow = "QUOTE",
                title = "받은 견적",
                description = "견적을 확인하고 수락하면 방문 예약을 진행할 수 있습니다.",
            )
            Spacer(Modifier.height(10.dp))

            when {
                state.loading -> LoadingState("견적과 예약 정보를 불러오는 중입니다.")
                state.quotes.isEmpty() -> InfoBanner(
                    message = "아직 발행된 견적이 없습니다. 관리자가 견적을 발행하면 알려드립니다.",
                    tone = StatusTone.Warning,
                )
                else -> {
                    state.quotes.sortedByDescending { it.versionNo }.forEach { quote ->
                        QuoteCard(
                            quote = quote,
                            acceptedQuoteExists = state.acceptedQuote != null,
                            busy = state.busy,
                            onAccept = { quoteToAccept = quote },
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            SectionHeading(
                eyebrow = "RESERVATION",
                title = "방문 예약",
                description = "견적 수락 후 원하는 방문 일정을 선택해 주세요.",
            )
            Spacer(Modifier.height(10.dp))

            val activeReservation = state.activeReservation
            val editableReservation = state.editableReservation
            if (activeReservation != null) {
                ReservationCard(
                    reservation = activeReservation,
                    busy = state.busy,
                    canCancel = editableReservation != null,
                    onCancel = { reservationToCancel = activeReservation },
                )
                Spacer(Modifier.height(12.dp))
            }

            if (!state.loading && state.acceptedQuote == null && activeReservation == null) {
                InfoBanner(
                    message = "견적을 수락하면 방문 예약을 진행할 수 있습니다.",
                    tone = StatusTone.Warning,
                )
            } else if (!state.loading && editableReservation != null) {
                ReservationScheduleForm(
                    state = state,
                    viewModel = viewModel,
                    activeReservation = editableReservation,
                )
            } else if (!state.loading && activeReservation != null) {
                InfoBanner(
                    message = "현재 예약 상태에서는 일정 변경이나 취소를 할 수 없습니다. 관리자에게 문의해 주세요.",
                    tone = StatusTone.Warning,
                )
            }
        }
    }

    if (quoteToAccept != null) {
        AlertDialog(
            onDismissRequest = { quoteToAccept = null },
            title = { Text("견적 수락") },
            text = {
                Text(
                    "견적 ${quoteToAccept?.quoteNo}을(를) 수락하시겠습니까?\n" +
                        "총 금액 ${DateTimes.formatCurrency(quoteToAccept?.totalAmount)}",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        quoteToAccept?.let { viewModel.acceptQuote(it.id) }
                        quoteToAccept = null
                    },
                ) {
                    Text("수락")
                }
            },
            dismissButton = {
                TextButton(onClick = { quoteToAccept = null }) {
                    Text("취소")
                }
            },
        )
    }

    if (reservationToCancel != null) {
        AlertDialog(
            onDismissRequest = { reservationToCancel = null },
            title = { Text("예약 취소") },
            text = { Text("예약 ${reservationToCancel?.reservationNo}을(를) 취소하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        reservationToCancel?.let { viewModel.cancelReservation(it.id) }
                        reservationToCancel = null
                    },
                ) {
                    Text("예약 취소")
                }
            },
            dismissButton = {
                TextButton(onClick = { reservationToCancel = null }) {
                    Text("닫기")
                }
            },
        )
    }
}

@Composable
private fun QuoteCard(
    quote: Quote,
    acceptedQuoteExists: Boolean,
    busy: Boolean,
    onAccept: () -> Unit,
) {
    val expired = quote.expiresAt != null && !DateTimes.isFuture(quote.expiresAt)
    SurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "${quote.quoteNo} · v${quote.versionNo}",
                    style = MaterialTheme.typography.labelMedium,
                    color = InkMuted,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = DateTimes.formatCurrency(quote.totalAmount),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Ink,
                )
            }
            StatusPill(
                label = Labels.quoteStatus(quote.status),
                tone = toneForStatus(quote.status),
            )
        }

        if (quote.issuedAt != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "발행 ${DateTimes.formatDateTime(quote.issuedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted,
            )
        }
        if (quote.expiresAt != null) {
            Text(
                text = "만료 ${DateTimes.formatDateTime(quote.expiresAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (expired) MaterialTheme.colorScheme.error else InkMuted,
            )
        }

        if (quote.lines.isNotEmpty()) {
            DividerSpacer()
            quote.lines.forEach { line ->
                QuoteLineRow(
                    description = "${line.description} × ${formatQuantity(line.quantity)}",
                    amount = DateTimes.formatCurrency(line.lineAmount),
                )
                Spacer(Modifier.height(4.dp))
            }
        }

        DividerSpacer()
        QuoteLineRow("소계", DateTimes.formatCurrency(quote.subtotalAmount))
        if (quote.discountAmount != 0.0) {
            Spacer(Modifier.height(4.dp))
            QuoteLineRow("할인", DateTimes.formatCurrency(quote.discountAmount))
        }
        if (quote.taxAmount != 0.0) {
            Spacer(Modifier.height(4.dp))
            QuoteLineRow("세액", DateTimes.formatCurrency(quote.taxAmount))
        }
        Spacer(Modifier.height(6.dp))
        QuoteLineRow("총액", DateTimes.formatCurrency(quote.totalAmount))

        if (quote.status == "ISSUED") {
            Spacer(Modifier.height(12.dp))
            if (acceptedQuoteExists) {
                InfoBanner("이미 수락한 견적이 있습니다.")
            } else if (expired) {
                InfoBanner("만료된 견적입니다.", tone = StatusTone.Warning)
            } else {
                Button(
                    onClick = onAccept,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("이 견적 수락")
                }
            }
        } else if (quote.status == "ACCEPTED") {
            Spacer(Modifier.height(12.dp))
            InfoBanner("수락한 견적입니다.", tone = StatusTone.Positive)
        }
    }
}

@Composable
private fun ReservationCard(
    reservation: Reservation,
    busy: Boolean,
    canCancel: Boolean,
    onCancel: () -> Unit,
) {
    SurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = reservation.reservationNo,
                    style = MaterialTheme.typography.labelMedium,
                    color = InkMuted,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = when (reservation.status) {
                        "CONFIRMED" -> "확정된 방문 일정"
                        "RESCHEDULE_REQUESTED" -> "일정 변경 처리 중"
                        else -> "예약 처리 중"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                )
            }
            StatusPill(
                label = Labels.reservationStatus(reservation.status),
                tone = toneForStatus(reservation.status),
            )
        }
        DividerSpacer()
        InfoRow(
            "일정",
            DateTimes.formatSchedule(reservation.scheduledStartAt, reservation.scheduledEndAt),
        )
        if (!reservation.customerMemo.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            InfoRow("요청사항", reservation.customerMemo)
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onCancel,
            enabled = !busy && canCancel,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("예약 취소")
        }
        if (!canCancel) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "현재 예약 상태에서는 예약을 취소할 수 없습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted,
            )
        }
    }
}

@Composable
private fun ReservationScheduleForm(
    state: CustomerDetailState,
    viewModel: CustomerViewModel,
    activeReservation: Reservation?,
) {
    val request = state.request ?: return
    val defaultDate = DateTimes.parseDate(request.desiredStartAt) ?: LocalDate.now().plusDays(1)
    val defaultTime = DateTimes.parseTime(request.desiredStartAt) ?: LocalTime.of(9, 0)
    val defaultEndTime = defaultTime
        .plusHours(4)
        .takeIf { it.isAfter(defaultTime) }
        ?: LocalTime.of(23, 59)

    var startDate by remember(request.id) { mutableStateOf(defaultDate) }
    var startTime by remember(request.id) { mutableStateOf(defaultTime) }
    var endDate by remember(request.id) { mutableStateOf(defaultDate) }
    var endTime by remember(request.id) { mutableStateOf(defaultEndTime) }
    var memo by rememberSaveable(request.id) { mutableStateOf("") }

    LaunchedEffect(
        activeReservation?.id,
        activeReservation?.scheduledStartAt,
        activeReservation?.scheduledEndAt,
    ) {
        if (activeReservation != null) {
            val parsedStartDate = DateTimes.parseDate(activeReservation.scheduledStartAt)
            val parsedStartTime = DateTimes.parseTime(activeReservation.scheduledStartAt)
            val parsedEndDate = DateTimes.parseDate(activeReservation.scheduledEndAt)
            val parsedEndTime = DateTimes.parseTime(activeReservation.scheduledEndAt)
            if (parsedStartDate != null && parsedStartTime != null) {
                startDate = parsedStartDate
                startTime = parsedStartTime
            }
            if (parsedEndDate != null && parsedEndTime != null) {
                endDate = parsedEndDate
                endTime = parsedEndTime
            }
            memo = activeReservation.customerMemo.orEmpty()
            viewModel.clearDetailAvailability()
        }
    }

    SurfaceCard {
        Text(
            text = if (activeReservation == null) "방문 일정 선택" else "예약 일정 변경",
            style = MaterialTheme.typography.titleMedium,
            color = Ink,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (activeReservation == null) {
                "가능 여부를 확인한 뒤 예약을 확정해 주세요."
            } else {
                "변경할 일정을 선택하고 가능 여부를 확인해 주세요."
            },
            style = MaterialTheme.typography.bodySmall,
            color = InkMuted,
        )
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DateField(
                label = "시작 날짜",
                date = startDate,
                onDateChange = {
                    startDate = it
                    endDate = it
                    viewModel.clearDetailAvailability()
                },
                modifier = Modifier.weight(1f),
            )
            TimeField(
                label = "시작 시간",
                time = startTime,
                onTimeChange = {
                    startTime = it
                    viewModel.clearDetailAvailability()
                },
                modifier = Modifier.weight(0.8f),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DateField(
                label = "종료 날짜",
                date = endDate,
                onDateChange = {
                    endDate = it
                    viewModel.clearDetailAvailability()
                },
                modifier = Modifier.weight(1f),
            )
            TimeField(
                label = "종료 시간",
                time = endTime,
                onTimeChange = {
                    endTime = it
                    viewModel.clearDetailAvailability()
                },
                modifier = Modifier.weight(0.8f),
            )
        }

        if (activeReservation == null) {
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it },
                label = { Text("요청사항 (선택)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (state.availability != null) {
            Spacer(Modifier.height(10.dp))
            InfoBanner(
                message = state.availabilityMessage.orEmpty(),
                tone = if (state.availability == true) StatusTone.Positive else StatusTone.Warning,
            )
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = {
                viewModel.checkAvailability(startDate, startTime, endDate, endTime)
            },
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("예약 가능 여부 확인")
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                viewModel.saveReservation(startDate, startTime, endDate, endTime, memo)
            },
            enabled = state.availability == true && !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (activeReservation == null) "예약 확정" else "일정 변경")
        }
    }
}

private fun formatQuantity(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

private fun pageCount(total: Long): Int =
    if (total <= 0) 1 else ((total + PAGE_SIZE - 1) / PAGE_SIZE).toInt()

private const val PAGE_SIZE = 20
