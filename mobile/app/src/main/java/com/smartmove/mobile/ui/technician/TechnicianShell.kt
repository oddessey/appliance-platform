package com.smartmove.mobile.ui.technician

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartmove.mobile.AppContainer
import com.smartmove.mobile.data.model.TechnicianAssignment
import com.smartmove.mobile.data.model.TechnicianAvailability
import com.smartmove.mobile.data.model.UserRole
import com.smartmove.mobile.data.model.UserSummary
import com.smartmove.mobile.ui.AppViewModelFactory
import com.smartmove.mobile.ui.account.AccountScreen
import com.smartmove.mobile.ui.common.BrandLockup
import com.smartmove.mobile.ui.common.DividerSpacer
import com.smartmove.mobile.ui.common.EmptyState
import com.smartmove.mobile.ui.common.ErrorBanner
import com.smartmove.mobile.ui.common.InfoBanner
import com.smartmove.mobile.ui.common.InfoRow
import com.smartmove.mobile.ui.common.LoadingState
import com.smartmove.mobile.ui.common.SectionHeading
import com.smartmove.mobile.ui.common.StatusPill
import com.smartmove.mobile.ui.common.StatusTone
import com.smartmove.mobile.ui.common.SurfaceCard
import com.smartmove.mobile.ui.common.Tag
import com.smartmove.mobile.ui.common.toneForStatus
import com.smartmove.mobile.ui.theme.Canvas
import com.smartmove.mobile.ui.theme.Ink
import com.smartmove.mobile.ui.theme.InkMuted
import com.smartmove.mobile.ui.theme.Line
import com.smartmove.mobile.util.DateTimes
import com.smartmove.mobile.util.Labels
import com.smartmove.mobile.util.PickedImage
import com.smartmove.mobile.util.readPickedImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.util.Locale

private data class AvailabilityDraft(
    val id: Int,
    val dayOfWeek: Int,
    val start: String,
    val end: String,
)

@Composable
fun TechnicianShell(
    container: AppContainer,
    user: UserSummary,
    factory: AppViewModelFactory,
    onSwitchRole: (UserRole) -> Unit,
) {
    val viewModel: TechnicianViewModel = viewModel(
        key = "technician-${user.id}",
        factory = factory,
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        containerColor = Canvas,
        topBar = { TechnicianHeader(user.name) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("배정") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("프로필") },
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
                0 -> TechnicianAssignmentsScreen(
                    state = state,
                    viewModel = viewModel,
                    onOpenProfile = { tab = 1 },
                    onEvidence = viewModel::openEvidence,
                )
                1 -> TechnicianProfileScreen(
                    state = state,
                    viewModel = viewModel,
                )
                else -> AccountScreen(
                    container = container,
                    user = user,
                    currentRole = UserRole.TECHNICIAN,
                    factory = factory,
                    onSwitchRole = onSwitchRole,
                )
            }
        }
    }

    val evidenceAssignment = state.evidenceAssignment
    if (evidenceAssignment != null) {
        TechnicianEvidenceSheet(
            state = state,
            viewModel = viewModel,
        )
    }
}

@Composable
private fun TechnicianHeader(userName: String) {
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
            Tag(text = "기사", prominent = true)
            Spacer(Modifier.width(6.dp))
            Tag(text = "${userName}님")
        }
    }
}

@Composable
private fun TechnicianAssignmentsScreen(
    state: TechnicianUiState,
    viewModel: TechnicianViewModel,
    onOpenProfile: () -> Unit,
    onEvidence: (TechnicianAssignment) -> Unit,
) {
    var rejectTarget by remember { mutableStateOf<TechnicianAssignment?>(null) }
    var rejectReason by remember { mutableStateOf("") }
    var completeTarget by remember { mutableStateOf<TechnicianAssignment?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshAssignments(showLoading = false)
        while (isActive) {
            delay(30_000)
            viewModel.loadProfile()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
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
                        eyebrow = "ASSIGNMENTS",
                        title = "배정된 작업",
                        description = "배정을 수락하고 이동·작업 상태를 기록하세요.",
                    )
                }
                IconButton(onClick = viewModel::loadProfile) {
                    Icon(Icons.Default.Refresh, contentDescription = "새로고침")
                }
            }
        }

        if (state.profileMissing) {
            item {
                SurfaceCard {
                    InfoBanner(
                        message = "기사 프로필을 등록하고 관리자 승인을 받아야 배정을 받을 수 있습니다.",
                        tone = StatusTone.Warning,
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = onOpenProfile,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("프로필 등록")
                    }
                }
            }
        } else if (state.profile != null && !state.profileApproved) {
            item {
                InfoBanner(
                    message = when {
                        state.profile?.approvalStatus == "REJECTED" ->
                            "기사 프로필이 거절되었습니다. 프로필을 수정해 다시 저장해 주세요."
                        state.profile?.approvalStatus == "APPROVED" ->
                            "기사 프로필이 비활성 상태입니다. 관리자에게 문의해 주세요."
                        else ->
                            "관리자 승인을 기다리고 있습니다. 승인 후 배정을 받을 수 있습니다."
                    },
                    tone = if (state.profile?.approvalStatus == "REJECTED") {
                        StatusTone.Danger
                    } else {
                        StatusTone.Warning
                    },
                )
            }
        } else if (state.profile != null && state.profile.licenseInfo.isNullOrBlank()) {
            item {
                InfoBanner(
                    message = "자격/면허 정보를 입력해야 배정 후보가 될 수 있습니다. 프로필에서 정보를 보완해 주세요.",
                    tone = StatusTone.Warning,
                )
            }
        }

        if (state.profileError != null) {
            item {
                ErrorBanner(
                    message = state.profileError,
                    onRetry = viewModel::loadProfile,
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                assignmentFilters.forEach { (status, label) ->
                    FilterChip(
                        selected = state.filter == status,
                        onClick = { viewModel.setFilter(status) },
                        label = { Text(label) },
                    )
                }
            }
        }

        if (state.error != null) {
            item {
                ErrorBanner(
                    message = state.error,
                    onRetry = viewModel::loadProfile,
                )
            }
        }
        if (state.message != null) {
            item {
                InfoBanner(
                    message = state.message,
                    tone = StatusTone.Positive,
                )
            }
        }

        if (state.profileLoading && state.profile == null) {
            item { LoadingState("기사 프로필을 확인하는 중입니다.") }
        } else if (state.loading && state.assignments.isEmpty()) {
            item { LoadingState("배정 목록을 불러오는 중입니다.") }
        } else if (!state.loading &&
            state.assignments.isEmpty() &&
            state.profileApproved &&
            state.profile?.licenseInfo.isNullOrBlank() != true
        ) {
            item {
                EmptyState(
                    title = "배정된 작업이 없습니다.",
                    description = "새 배정이 들어오면 이곳에서 확인할 수 있습니다.",
                )
            }
        } else {
            items(state.assignments, key = { it.id }) { assignment ->
                AssignmentCard(
                    assignment = assignment,
                    busy = state.busyAssignmentId == assignment.id,
                    onAccept = { viewModel.acceptAssignment(assignment.id) },
                    onReject = {
                        rejectTarget = assignment
                        rejectReason = ""
                    },
                    onAdvance = { eventType ->
                        if (eventType == "WORK_COMPLETED") {
                            completeTarget = assignment
                        } else {
                            viewModel.advanceAssignment(assignment.id, eventType)
                        }
                    },
                    onEvidence = { onEvidence(assignment) },
                )
            }
            if (state.total > ASSIGNMENT_PAGE_SIZE) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = { viewModel.changePage(state.page - 1) },
                            enabled = state.page > 0,
                        ) {
                            Text("이전")
                        }
                        Text(
                            text = "${state.page + 1} / ${assignmentPageCount(state.total)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkMuted,
                        )
                        TextButton(
                            onClick = { viewModel.changePage(state.page + 1) },
                            enabled = (state.page + 1) * ASSIGNMENT_PAGE_SIZE < state.total,
                        ) {
                            Text("다음")
                        }
                    }
                }
            }
        }
    }

    if (rejectTarget != null) {
        AlertDialog(
            onDismissRequest = { rejectTarget = null },
            title = { Text("배정 거절") },
            text = {
                Column {
                    Text("거절 사유를 입력해 주세요.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = { Text("거절 사유") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        rejectTarget?.let { viewModel.rejectAssignment(it.id, rejectReason) }
                        rejectTarget = null
                    },
                    enabled = rejectReason.isNotBlank(),
                ) {
                    Text("거절")
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectTarget = null }) {
                    Text("취소")
                }
            },
        )
    }

    if (completeTarget != null) {
        AlertDialog(
            onDismissRequest = { completeTarget = null },
            title = { Text("작업 완료") },
            text = { Text("작업을 완료 처리하시겠습니까? 완료 후에는 되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        completeTarget?.let {
                            viewModel.advanceAssignment(it.id, "WORK_COMPLETED")
                        }
                        completeTarget = null
                    },
                ) {
                    Text("작업 완료")
                }
            },
            dismissButton = {
                TextButton(onClick = { completeTarget = null }) {
                    Text("취소")
                }
            },
        )
    }
}

@Composable
private fun AssignmentCard(
    assignment: TechnicianAssignment,
    busy: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onAdvance: (String) -> Unit,
    onEvidence: () -> Unit,
) {
    SurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = assignment.reservationNo,
                    style = MaterialTheme.typography.labelMedium,
                    color = InkMuted,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = DateTimes.formatSchedule(
                        assignment.scheduledStartAt,
                        assignment.scheduledEndAt,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                )
            }
            StatusPill(
                label = Labels.assignmentStatus(assignment.status),
                tone = toneForStatus(assignment.status),
            )
        }

        DividerSpacer()
        InfoRow("고객", assignment.customerName)
        Spacer(Modifier.height(6.dp))
        InfoRow("연락처", assignment.customerPhone)
        Spacer(Modifier.height(6.dp))
        InfoRow("가전", assignment.applianceTypes)
        Spacer(Modifier.height(6.dp))
        InfoRow("이전", assignment.originAddress)
        Spacer(Modifier.height(6.dp))
        InfoRow("설치", assignment.destinationAddress)
        if (!assignment.customerMemo.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            InfoRow("요청사항", assignment.customerMemo)
        }
        if (assignment.quoteTotalAmount != null) {
            Spacer(Modifier.height(6.dp))
            InfoRow("견적 금액", DateTimes.formatCurrency(assignment.quoteTotalAmount))
        }

        val evidenceAvailable = assignment.status in setOf(
            "WORK_STARTED",
            "WORK_COMPLETED",
        )
        if (evidenceAvailable) {
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onEvidence,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("작업 사진 관리")
            }
        }

        Spacer(Modifier.height(10.dp))
        when (assignment.status) {
            "PENDING" -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onAccept,
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text("배정 수락")
                        }
                    }
                    OutlinedButton(
                        onClick = onReject,
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("거절")
                    }
                }
            }
            "ACCEPTED" -> {
                Button(
                    onClick = { onAdvance("ON_THE_WAY") },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("이동 시작")
                    }
                }
            }
            "ON_THE_WAY" -> {
                Button(
                    onClick = { onAdvance("WORK_STARTED") },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("작업 시작")
                    }
                }
            }
            "WORK_STARTED" -> {
                Button(
                    onClick = { onAdvance("WORK_COMPLETED") },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("작업 완료")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TechnicianProfileScreen(
    state: TechnicianUiState,
    viewModel: TechnicianViewModel,
) {
    val profile = state.profile
    var businessName by rememberSaveable { mutableStateOf("") }
    var licenseInfo by rememberSaveable { mutableStateOf("") }
    var serviceAreasText by rememberSaveable { mutableStateOf("") }
    var selectedCodes by remember { mutableStateOf(setOf<String>()) }
    var availabilityDrafts by remember { mutableStateOf(listOf<AvailabilityDraft>()) }
    var nextDraftId by remember { mutableIntStateOf(0) }

    LaunchedEffect(profile, state.profileMissing) {
        if (profile != null) {
            businessName = profile.businessName
            licenseInfo = profile.licenseInfo.orEmpty()
            serviceAreasText = profile.serviceAreas.joinToString("\n")
            selectedCodes = profile.applianceTypeCodes.toSet()
            availabilityDrafts = profile.availability.map {
                AvailabilityDraft(
                    id = nextDraftId++,
                    dayOfWeek = it.dayOfWeek,
                    start = normalizeTime(it.startTime),
                    end = normalizeTime(it.endTime),
                )
            }
        } else if (state.profileMissing && availabilityDrafts.isEmpty()) {
            availabilityDrafts = (1..5).map { day ->
                AvailabilityDraft(
                    id = nextDraftId++,
                    dayOfWeek = day,
                    start = "09:00",
                    end = "18:00",
                )
            }
        }
    }

    if (state.profileLoading && profile == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            LoadingState("기사 프로필을 불러오는 중입니다.")
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeading(
                eyebrow = "PROFILE",
                title = "기사 프로필",
                description = "서비스 지역과 담당 가전, 가능한 작업 시간을 관리합니다.",
            )
        }

        if (state.profileError != null) {
            item {
                ErrorBanner(
                    message = state.profileError,
                    onRetry = if (profile == null) viewModel::loadProfile else null,
                    onDismiss = viewModel::clearProfileMessages,
                )
            }
        }
        if (state.profileMessage != null) {
            item {
                InfoBanner(
                    message = state.profileMessage,
                    tone = StatusTone.Positive,
                )
            }
        }

        if (profile != null) {
            item {
                SurfaceCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = profile.businessName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Ink,
                            modifier = Modifier.weight(1f),
                        )
                        StatusPill(
                            label = Labels.approvalStatus(profile.approvalStatus),
                            tone = toneForStatus(profile.approvalStatus),
                        )
                        Spacer(Modifier.width(6.dp))
                        Tag(text = if (profile.active) "활성" else "비활성")
                    }
                    Spacer(Modifier.height(10.dp))
                    InfoBanner(
                        message = when {
                            profile.approvalStatus == "REJECTED" ->
                                "프로필이 거절되었습니다. 정보를 수정해 다시 저장해 주세요."
                            profile.approvalStatus != "APPROVED" ->
                                "관리자 승인 대기 중입니다. 승인 후 배정을 받을 수 있습니다."
                            !profile.active ->
                                "프로필은 승인되었지만 현재 비활성 상태입니다."
                            profile.licenseInfo.isNullOrBlank() ->
                                "자격/면허 정보를 입력해야 배정 후보가 될 수 있습니다."
                            else -> "배정을 받을 수 있는 상태입니다."
                        },
                        tone = when {
                            profile.approvalStatus == "REJECTED" -> StatusTone.Danger
                            profile.approvalStatus != "APPROVED" ||
                                !profile.active ||
                                profile.licenseInfo.isNullOrBlank() -> StatusTone.Warning
                            else -> StatusTone.Positive
                        },
                    )
                }
            }
        } else if (state.profileMissing) {
            item {
                InfoBanner(
                    message = "아직 기사 프로필이 없습니다. 등록 후 관리자 승인을 받아 주세요.",
                    tone = StatusTone.Warning,
                )
            }
        }

        item {
            SurfaceCard {
                Text(
                    text = "기본 정보",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = businessName,
                    onValueChange = { businessName = it },
                    label = { Text("상호명") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = licenseInfo,
                    onValueChange = { licenseInfo = it },
                    label = { Text("자격/면허 정보 (필수)") },
                    supportingText = { Text("배정 후보 등록에 필요합니다.") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = serviceAreasText,
                    onValueChange = { serviceAreasText = it },
                    label = { Text("서비스 지역") },
                    placeholder = { Text("서울특별시 강남구\n서울특별시 송파구") },
                    supportingText = { Text("예: 서울특별시 강남구. 시·도만 입력하면 해당 지역 전체를 의미합니다. 쉼표 또는 줄바꿈으로 구분합니다.") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            SurfaceCard {
                Text(
                    text = "담당 가전제품",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                )
                Spacer(Modifier.height(8.dp))
                when {
                    state.typesLoading -> LoadingState("가전제품 목록을 불러오는 중입니다.")
                    state.typesError != null -> ErrorBanner(
                        message = state.typesError,
                        onRetry = viewModel::loadApplianceTypes,
                    )
                    else -> FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.applianceTypes.forEach { type ->
                            FilterChip(
                                selected = type.code in selectedCodes,
                                onClick = {
                                    selectedCodes = if (type.code in selectedCodes) {
                                        selectedCodes - type.code
                                    } else {
                                        selectedCodes + type.code
                                    }
                                },
                                label = { Text(type.name) },
                            )
                        }
                    }
                }
            }
        }

        item {
            SurfaceCard {
                Text(
                    text = "가능한 작업 시간",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "같은 요일의 시간이 겹치지 않도록 입력해 주세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                )
                Spacer(Modifier.height(12.dp))
                availabilityDrafts.forEach { draft ->
                    key(draft.id) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Line, MaterialTheme.shapes.medium)
                                .padding(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                DayDropdown(
                                    dayOfWeek = draft.dayOfWeek,
                                    onSelect = { day ->
                                        availabilityDrafts = availabilityDrafts.map {
                                            if (it.id == draft.id) it.copy(dayOfWeek = day) else it
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(
                                    onClick = {
                                        availabilityDrafts =
                                            availabilityDrafts.filterNot { it.id == draft.id }
                                    },
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "삭제")
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedTextField(
                                    value = draft.start,
                                    onValueChange = { value ->
                                        availabilityDrafts = availabilityDrafts.map {
                                            if (it.id == draft.id) it.copy(start = value) else it
                                        }
                                    },
                                    label = { Text("시작") },
                                    singleLine = true,
                                    placeholder = { Text("09:00") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                                    modifier = Modifier.weight(1f),
                                )
                                Text("~", color = InkMuted)
                                OutlinedTextField(
                                    value = draft.end,
                                    onValueChange = { value ->
                                        availabilityDrafts = availabilityDrafts.map {
                                            if (it.id == draft.id) it.copy(end = value) else it
                                        }
                                    },
                                    label = { Text("종료") },
                                    singleLine = true,
                                    placeholder = { Text("18:00") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
                OutlinedButton(
                    onClick = {
                        availabilityDrafts = availabilityDrafts + AvailabilityDraft(
                            id = nextDraftId++,
                            dayOfWeek = 1,
                            start = "09:00",
                            end = "18:00",
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("시간 추가")
                }
            }
        }

        item {
            InfoBanner(
                message = "프로필 또는 자격 정보를 변경하면 관리자 재승인이 필요할 수 있습니다.",
                tone = StatusTone.Warning,
            )
        }

        item {
            Button(
                onClick = {
                    viewModel.saveProfile(
                        businessName = businessName,
                        licenseInfo = licenseInfo,
                        serviceAreasText = serviceAreasText,
                        applianceTypeCodes = selectedCodes,
                        availability = availabilityDrafts.map {
                            TechnicianAvailability(
                                dayOfWeek = it.dayOfWeek,
                                startTime = it.start,
                                endTime = it.end,
                            )
                        },
                    )
                },
                enabled = !state.profileSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (state.profileSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        text = if (profile == null) "프로필 등록" else "프로필 저장",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDropdown(
    dayOfWeek: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = "${Labels.dayName(dayOfWeek)}요일",
            onValueChange = {},
            readOnly = true,
            label = { Text("요일") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            (1..7).forEach { day ->
                DropdownMenuItem(
                    text = { Text("${Labels.dayName(day)}요일") },
                    onClick = {
                        onSelect(day)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TechnicianEvidenceSheet(
    state: TechnicianUiState,
    viewModel: TechnicianViewModel,
) {
    val assignment = state.evidenceAssignment ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching { readPickedImage(context, uri) }
                }
                val picked = result.getOrNull()
                if (picked != null) {
                    viewModel.uploadEvidence(picked.fileName, picked.mimeType, picked.bytes)
                } else {
                    viewModel.reportEvidenceError(
                        result.exceptionOrNull()?.message ?: "이미지 파일을 읽지 못했습니다."
                    )
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = viewModel::closeEvidence,
        sheetState = sheetState,
        containerColor = Canvas,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 36.dp),
        ) {
            SectionHeading(
                eyebrow = "EVIDENCE",
                title = "작업 사진",
                description = "${assignment.reservationNo} · ${assignment.customerName}",
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = DateTimes.formatSchedule(
                    assignment.scheduledStartAt,
                    assignment.scheduledEndAt,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = InkMuted,
            )
            Spacer(Modifier.height(14.dp))

            SurfaceCard {
                Text(
                    text = "사진 업로드",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "JPEG, PNG, WebP 이미지 · 10MB 이하",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !state.evidenceUploading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.evidenceUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("작업 사진 선택")
                    }
                }
                if (state.evidenceError != null) {
                    Spacer(Modifier.height(10.dp))
                    ErrorBanner(message = state.evidenceError)
                }
                if (state.evidenceMessage != null) {
                    Spacer(Modifier.height(10.dp))
                    InfoBanner(
                        message = state.evidenceMessage,
                        tone = StatusTone.Positive,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionHeading(
                eyebrow = "UPLOADED",
                title = "업로드된 사진",
            )
            Spacer(Modifier.height(10.dp))

            if (state.evidenceLoading) {
                LoadingState("작업 사진을 불러오는 중입니다.")
            } else if (state.evidence.isEmpty()) {
                EmptyState(
                    title = "업로드된 사진이 없습니다.",
                    description = "작업 전후 사진을 남겨 주세요.",
                )
            } else {
                state.evidence.forEach { evidence ->
                    SurfaceCard {
                        Text(
                            text = evidence.originalFilename,
                            style = MaterialTheme.typography.titleSmall,
                            color = Ink,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${formatFileSize(evidence.sizeBytes)} · " +
                                DateTimes.formatDateTime(evidence.uploadedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = InkMuted,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

private val assignmentFilters = listOf(
    null to "전체",
    "PENDING" to "배정 대기",
    "ACCEPTED" to "수락",
    "ON_THE_WAY" to "이동 중",
    "WORK_STARTED" to "작업 중",
    "WORK_COMPLETED" to "완료",
    "REJECTED" to "거절",
)

private fun normalizeTime(value: String): String =
    if (value.length >= 5) value.take(5) else value

private fun formatFileSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> String.format(Locale.KOREA, "%.1fMB", bytes / (1024.0 * 1024.0))
    bytes >= 1024 -> String.format(Locale.KOREA, "%.0fKB", bytes / 1024.0)
    else -> "${bytes}B"
}

private fun assignmentPageCount(total: Long): Int =
    if (total <= 0) 1 else ((total + ASSIGNMENT_PAGE_SIZE - 1) / ASSIGNMENT_PAGE_SIZE).toInt()

private const val ASSIGNMENT_PAGE_SIZE = 20
