package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ReminderCategory
import com.example.data.ReminderStatus
import com.example.domain.TodayTaskDisplayStatus
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberSignal
import com.example.ui.theme.BorderNavy
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.ElevatedNavy
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SkyCyan
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.adaptiveFontFamily

@Composable
fun AdaptiveText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    preferDisplay: Boolean = false
) {
    Text(
        text = text,
        modifier = modifier,
        style = style.copy(fontFamily = adaptiveFontFamily(text, preferDisplay = preferDisplay)),
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun CategoryBadge(
    category: ReminderCategory,
    isAssignment: Boolean,
    modifier: Modifier = Modifier
) {
    val effectiveCat = if (isAssignment) ReminderCategory.ASSIGNMENT else category
    val (bg, fg, icon) = when (effectiveCat) {
        ReminderCategory.ASSIGNMENT -> Triple(AmberContainer, AmberSignal, Icons.AutoMirrored.Rounded.Assignment)
        ReminderCategory.STUDY -> Triple(TealContainer, ElectricTeal, Icons.AutoMirrored.Rounded.MenuBook)
        ReminderCategory.WORK -> Triple(ElevatedNavy, SkyCyan, Icons.Rounded.Work)
        ReminderCategory.HEALTH -> Triple(EmeraldContainer, EmeraldSuccess, Icons.Rounded.Favorite)
        ReminderCategory.PERSONAL -> Triple(ElevatedNavy, TextSecondary, Icons.Rounded.Person)
    }

    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = effectiveCat.displayName,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = fg
            )
        }
    }
}

@Composable
fun StatusPill(
    status: ReminderStatus,
    snoozeCount: Int,
    modifier: Modifier = Modifier
) {
    val (label, bg, fg, icon) = when (status) {
        ReminderStatus.UPCOMING -> Quad("Upcoming", TealContainer, ElectricTeal, Icons.Rounded.NotificationsActive)
        ReminderStatus.RINGING -> Quad("Due / Ringing", CoralContainer, AmberSignal, Icons.Rounded.NotificationsActive)
        ReminderStatus.OVERDUE -> Quad("Overdue", CoralContainer, CoralAlert, Icons.Rounded.ErrorOutline)
        ReminderStatus.SNOOZED -> Quad(
            if (snoozeCount > 0) "Snoozed ×$snoozeCount" else "Snoozed",
            AmberContainer,
            AmberSignal,
            Icons.Rounded.Snooze
        )
        ReminderStatus.COMPLETED -> Quad("Completed", EmeraldContainer, EmeraldSuccess, Icons.Rounded.CheckCircle)
        ReminderStatus.CANCELLED, ReminderStatus.DISMISSED -> Quad("Cancelled", ElevatedNavy, TextSecondary, Icons.Rounded.Cancel)
    }

    StatusChipRaw(label = label, bg = bg, fg = fg, icon = icon, modifier = modifier)
}

@Composable
fun TodayStatusPill(
    displayStatus: TodayTaskDisplayStatus,
    modifier: Modifier = Modifier
) {
    val (bg, fg, icon) = when (displayStatus) {
        TodayTaskDisplayStatus.UPCOMING -> Triple(TealContainer, ElectricTeal, Icons.Rounded.NotificationsActive)
        TodayTaskDisplayStatus.DUE -> Triple(AmberContainer, AmberSignal, Icons.Rounded.NotificationsActive)
        TodayTaskDisplayStatus.OVERDUE -> Triple(CoralContainer, CoralAlert, Icons.Rounded.ErrorOutline)
        TodayTaskDisplayStatus.COMPLETED -> Triple(EmeraldContainer, EmeraldSuccess, Icons.Rounded.CheckCircle)
        TodayTaskDisplayStatus.CANCELLED -> Triple(ElevatedNavy, TextSecondary, Icons.Rounded.Cancel)
    }

    StatusChipRaw(label = displayStatus.label, bg = bg, fg = fg, icon = icon, modifier = modifier)
}

@Composable
private fun StatusChipRaw(
    label: String,
    bg: Color,
    fg: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, BorderNavy)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fg
            )
        }
    }
}

private data class Quad(
    val label: String,
    val bg: Color,
    val fg: Color,
    val icon: ImageVector
)

fun formatRelativeCountdown(targetMillis: Long, nowMillis: Long): String {
    val diff = targetMillis - nowMillis
    if (diff <= 0L) return "Due / Overdue"
    val totalMinutes = (diff / 60_000L).toInt()
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    return when {
        totalMinutes < 1 -> "In < 1 min"
        hours == 0 -> "In ${mins}m"
        hours < 24 -> if (mins == 0) "In ${hours}h" else "In ${hours}h ${mins}m"
        else -> {
            val days = hours / 24
            val remHours = hours % 24
            if (remHours == 0) "In ${days}d" else "In ${days}d ${remHours}h"
        }
    }
}
