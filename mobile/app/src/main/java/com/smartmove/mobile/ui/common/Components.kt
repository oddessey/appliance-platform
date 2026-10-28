package com.smartmove.mobile.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smartmove.mobile.ui.theme.BrandGreen
import com.smartmove.mobile.ui.theme.BrandGreenDark
import com.smartmove.mobile.ui.theme.BrandGreenSoft
import com.smartmove.mobile.ui.theme.BrandMint
import com.smartmove.mobile.ui.theme.Ink
import com.smartmove.mobile.ui.theme.InkMuted
import com.smartmove.mobile.ui.theme.Line

enum class StatusTone {
    Positive,
    Warning,
    Neutral,
    Danger,
    Info,
}

fun toneForStatus(status: String?): StatusTone = when (status) {
    "COMPLETED", "WORK_COMPLETED", "ACCEPTED", "APPROVED", "CONFIRMED", "PAID" -> StatusTone.Positive
    "PENDING", "SUBMITTED", "ANALYZING", "NEEDS_REVIEW", "ON_THE_WAY", "WORK_STARTED",
    "RESCHEDULE_REQUESTED", "IN_PROGRESS", "ISSUED", "QUOTED", "RESERVED" -> StatusTone.Warning
    "REJECTED", "CANCELLED", "EXPIRED", "FAILED", "WITHDRAWN", "SUSPENDED" -> StatusTone.Danger
    else -> StatusTone.Neutral
}

@Composable
fun BrandMark(modifier: Modifier = Modifier, size: Int = 40) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 3).dp))
            .background(BrandGreen),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "S",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun BrandLockup(modifier: Modifier = Modifier, dark: Boolean = false) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandMark(size = 36)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = "스마트무브",
                style = MaterialTheme.typography.titleMedium,
                color = if (dark) Color.White else Ink,
            )
            Text(
                text = "가전 이전설치",
                style = MaterialTheme.typography.bodySmall,
                color = if (dark) Color.White.copy(alpha = 0.72f) else InkMuted,
            )
        }
    }
}

@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, Line),
        colors = androidx.compose.material3.CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content = content,
        )
    }
}

@Composable
fun SectionHeading(
    eyebrow: String?,
    title: String,
    description: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (!eyebrow.isNullOrBlank()) {
            Text(
                text = eyebrow.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = BrandGreen,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = Ink,
        )
        if (!description.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = InkMuted,
            )
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    tone: StatusTone = StatusTone.Neutral,
    modifier: Modifier = Modifier,
) {
    val (container, content) = when (tone) {
        StatusTone.Positive -> Color(0xFFE2F2E8) to Color(0xFF1C6A45)
        StatusTone.Warning -> Color(0xFFFCF0D8) to Color(0xFF8A5A12)
        StatusTone.Danger -> Color(0xFFFCE4E1) to Color(0xFFA12F27)
        StatusTone.Info -> Color(0xFFE4EEF5) to Color(0xFF2B5C75)
        StatusTone.Neutral -> Color(0xFFF0F2EF) to Color(0xFF5D675F)
    }
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = container,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            color = content,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun Tag(text: String, modifier: Modifier = Modifier, prominent: Boolean = false) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = if (prominent) BrandGreenSoft else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (prominent) BrandGreenDark else InkMuted,
        )
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Ink,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            modifier = Modifier.width(82.dp),
            style = MaterialTheme.typography.bodySmall,
            color = InkMuted,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
        )
    }
}

@Composable
fun DividerSpacer() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
fun ErrorBanner(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                if (onRetry != null) {
                    TextButton(onClick = onRetry) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("다시 시도")
                    }
                }
            }
            if (onDismiss != null) {
                TextButton(onClick = onDismiss) {
                    Text("닫기")
                }
            }
        }
    }
}

@Composable
fun InfoBanner(
    message: String,
    modifier: Modifier = Modifier,
    tone: StatusTone = StatusTone.Info,
) {
    val (background, foreground) = when (tone) {
        StatusTone.Positive -> Color(0xFFE7F5EC) to Color(0xFF1C6A45)
        StatusTone.Warning -> Color(0xFFFFF4DE) to Color(0xFF7A5212)
        StatusTone.Danger -> Color(0xFFFFE9E7) to Color(0xFF9C2C25)
        else -> Color(0xFFEAF2F7) to Color(0xFF2B5C75)
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = background,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = foreground,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = foreground,
            )
        }
    }
}

@Composable
fun LoadingState(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(10.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = InkMuted,
        )
    }
}

@Composable
fun EmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(BrandGreenSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text("S", color = BrandGreen, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = Ink)
        Spacer(Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = InkMuted,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun QuoteLineRow(
    description: String,
    amount: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = description,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = InkMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = amount,
            style = MaterialTheme.typography.bodySmall,
            color = Ink,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

val HeroGradient = listOf(BrandGreenDark, BrandGreen, BrandMint.copy(alpha = 0.9f))
