package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.NumberedTodayTask
import com.example.domain.TodayOverviewSummary
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
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.SkyCyan
import com.example.ui.theme.SurfaceNavy
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TodayOverviewCard(
    overview: TodayOverviewSummary,
    modifier: Modifier = Modifier
) {
    val targetFraction = if (overview.totalCount == 0) 0f else overview.progressPercent / 100f
    val animatedProgress by animateFloatAsState(
        targetValue = targetFraction,
        label = "today_progress"
    )

    Surface(
        color = SurfaceNavy,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, BorderNavy),
        modifier = modifier
            .fillMaxWidth()
            .testTag("today_overview_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarToday,
                        contentDescription = null,
                        tint = ElectricTeal,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = "TODAY OVERVIEW",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricTeal
                        )
                        Text(
                            text = overview.todayDateLabel,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                            modifier = Modifier.testTag("today_date_label")
                        )
                    }
                }

                Surface(
                    color = ElevatedNavy,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, BorderNavy)
                ) {
                    Text(
                        text = overview.timezoneLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = SkyCyan,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${overview.completedCount}/${overview.totalCount} completed",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary,
                    modifier = Modifier.testTag("today_progress_text")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (overview.cancelledCount > 0) {
                        Text(
                            text = "${overview.cancelledCount} cancelled",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = "${overview.progressPercent}%",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = ElectricTeal,
                        modifier = Modifier.testTag("today_progress_percent")
                    )
                }
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .testTag("today_progress_bar"),
                color = ElectricTeal,
                trackColor = MidnightNavy,
                strokeCap = StrokeCap.Round
            )

            Text(
                text = "Daily count tracks today's local calendar day and resets at midnight while preserving history.",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun TodayTimelineSection(
    tasks: List<NumberedTodayTask>,
    onMarkComplete: (Long) -> Unit,
    onCancelTask: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MidnightNavy,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, BorderNavy),
        modifier = modifier
            .fillMaxWidth()
            .testTag("today_timeline_section")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Today's Timeline",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Surface(
                    color = TealContainer,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = "${tasks.size} scheduled",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = ElectricTeal,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Color-coded status legend for instant readability
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .testTag("timeline_status_legend"),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimelineLegendItem(color = SkyCyan, label = "Upcoming")
                TimelineLegendItem(color = AmberSignal, label = "Due")
                TimelineLegendItem(color = EmeraldSuccess, label = "Completed")
                TimelineLegendItem(color = CoralAlert, label = "Overdue")
            }

            if (tasks.isEmpty()) {
                Text(
                    text = "No tasks scheduled for today yet. Add a task or assignment for today to track it here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    tasks.forEachIndexed { index, item ->
                        TodayTimelineRow(
                            item = item,
                            isFirst = index == 0,
                            isLast = index == tasks.lastIndex,
                            onMarkComplete = { onMarkComplete(item.reminder.id) },
                            onCancelTask = { onCancelTask(item.reminder.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineLegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

private data class TimelineStatusVisuals(
    val accentColor: Color,
    val badgeBg: Color,
    val cardSurfaceColor: Color,
    val borderColor: Color
)

private fun visualsForStatus(status: TodayTaskDisplayStatus): TimelineStatusVisuals {
    return when (status) {
        TodayTaskDisplayStatus.UPCOMING -> TimelineStatusVisuals(
            accentColor = SkyCyan,
            badgeBg = TealContainer,
            cardSurfaceColor = SurfaceNavy,
            borderColor = SkyCyan.copy(alpha = 0.35f)
        )
        TodayTaskDisplayStatus.DUE -> TimelineStatusVisuals(
            accentColor = AmberSignal,
            badgeBg = AmberContainer,
            cardSurfaceColor = AmberContainer.copy(alpha = 0.28f).compositeOver(SurfaceNavy),
            borderColor = AmberSignal.copy(alpha = 0.55f)
        )
        TodayTaskDisplayStatus.COMPLETED -> TimelineStatusVisuals(
            accentColor = EmeraldSuccess,
            badgeBg = EmeraldContainer,
            cardSurfaceColor = EmeraldContainer.copy(alpha = 0.24f).compositeOver(SurfaceNavy),
            borderColor = EmeraldSuccess.copy(alpha = 0.45f)
        )
        TodayTaskDisplayStatus.OVERDUE -> TimelineStatusVisuals(
            accentColor = CoralAlert,
            badgeBg = CoralContainer,
            cardSurfaceColor = CoralContainer.copy(alpha = 0.32f).compositeOver(SurfaceNavy),
            borderColor = CoralAlert.copy(alpha = 0.55f)
        )
        TodayTaskDisplayStatus.CANCELLED -> TimelineStatusVisuals(
            accentColor = TextMuted,
            badgeBg = ElevatedNavy,
            cardSurfaceColor = ElevatedNavy.copy(alpha = 0.55f),
            borderColor = BorderNavy
        )
    }
}

@Composable
private fun TodayTimelineRow(
    item: NumberedTodayTask,
    isFirst: Boolean,
    isLast: Boolean,
    onMarkComplete: () -> Unit,
    onCancelTask: () -> Unit
) {
    val isActive = item.displayStatus != TodayTaskDisplayStatus.COMPLETED &&
        item.displayStatus != TodayTaskDisplayStatus.CANCELLED
    val visuals = visualsForStatus(item.displayStatus)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Vertical color-coded timeline track + status node
        Column(
            modifier = Modifier
                .width(26.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(22.dp)
                    .background(if (isFirst) Color.Transparent else BorderNavy)
            )

            TimelineStatusNode(
                status = item.displayStatus,
                accentColor = visuals.accentColor,
                taskNumber = item.taskNumber
            )

            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f)
                    .background(if (isLast) Color.Transparent else BorderNavy)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Task card with color-coded left accent bar and status-tinted surface
        Surface(
            color = visuals.cardSurfaceColor,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, visuals.borderColor),
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 10.dp)
                .testTag("today_task_row_${item.taskNumber}")
        ) {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(visuals.accentColor)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = visuals.badgeBg,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Task ${item.taskNumber}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = visuals.accentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        CategoryBadge(
                            category = item.reminder.category,
                            isAssignment = item.reminder.isAssignment
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        TodayStatusPill(displayStatus = item.displayStatus)
                    }

                    AdaptiveText(
                        text = item.reminder.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isActive) TextPrimary else TextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = visuals.accentColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = item.formattedDueTime,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = visuals.accentColor
                            )
                            if (isActive && item.reminder.advanceMinutes > 0) {
                                Icon(
                                    imageVector = Icons.Rounded.Notifications,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "${item.reminder.advanceMinutes}m prior",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        if (isActive) {
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                TextButton(
                                    onClick = onMarkComplete,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("today_complete_btn_${item.reminder.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Mark Task ${item.taskNumber} complete",
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Complete", color = EmeraldSuccess)
                                }
                                TextButton(
                                    onClick = onCancelTask,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("today_cancel_btn_${item.reminder.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Cancel,
                                        contentDescription = "Cancel Task ${item.taskNumber}",
                                        tint = CoralAlert,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cancel", color = CoralAlert)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineStatusNode(
    status: TodayTaskDisplayStatus,
    accentColor: Color,
    taskNumber: Int
) {
    val description = "Task $taskNumber status: ${status.label}"
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(
                when (status) {
                    TodayTaskDisplayStatus.COMPLETED,
                    TodayTaskDisplayStatus.OVERDUE,
                    TodayTaskDisplayStatus.DUE -> accentColor
                    TodayTaskDisplayStatus.UPCOMING -> ObsidianNavy
                    TodayTaskDisplayStatus.CANCELLED -> ElevatedNavy
                }
            )
            .border(
                width = 2.dp,
                color = accentColor,
                shape = CircleShape
            )
            .semantics { contentDescription = description }
            .testTag("timeline_indicator_${taskNumber}_${status.name.lowercase()}")
    ) {
        when (status) {
            TodayTaskDisplayStatus.COMPLETED -> Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = ObsidianNavy,
                modifier = Modifier.size(12.dp)
            )
            TodayTaskDisplayStatus.OVERDUE -> Icon(
                imageVector = Icons.Rounded.PriorityHigh,
                contentDescription = null,
                tint = ObsidianNavy,
                modifier = Modifier.size(12.dp)
            )
            TodayTaskDisplayStatus.DUE -> Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(ObsidianNavy)
            )
            TodayTaskDisplayStatus.UPCOMING -> Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            TodayTaskDisplayStatus.CANCELLED -> Unit
        }
    }
}
