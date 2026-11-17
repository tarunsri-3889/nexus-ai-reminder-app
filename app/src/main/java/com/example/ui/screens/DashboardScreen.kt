package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ReminderCategory
import com.example.data.ReminderEntity
import com.example.data.ReminderStatus
import com.example.domain.NaturalLanguageReminderParser
import com.example.ui.components.AdaptiveText
import com.example.ui.components.CategoryBadge
import com.example.ui.components.StatusPill
import com.example.ui.components.TodayOverviewCard
import com.example.ui.components.TodayTimelineSection
import com.example.ui.components.formatRelativeCountdown
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberSignal
import com.example.ui.theme.BorderNavy
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.ElevatedNavy
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.SkyCyan
import com.example.ui.theme.SurfaceNavy
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DashboardUiState
import com.example.ui.viewmodel.ReminderFilter
import java.util.Calendar

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onOpenComposer: () -> Unit,
    onOpenComposerWithMic: () -> Unit,
    onOpenReadiness: () -> Unit,
    onPreviewRingNow: () -> Unit,
    onSchedule5sDemo: () -> Unit,
    onSelectFilter: (ReminderFilter) -> Unit,
    onCompleteReminder: (Long) -> Unit,
    onCancelReminder: (Long) -> Unit,
    onSnooze10m: (Long) -> Unit,
    onRescheduleReminder: (Long, Long) -> Unit,
    onDeleteReminder: (Long) -> Unit,
    onRingReminderNow: (Long) -> Unit,
    onLoadSampleSchedule: () -> Unit,
    onDismissStatus: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            onDismissStatus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianNavy)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(ElectricTeal.copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.08f),
                        radius = size.width * 0.85f
                    ),
                    radius = size.width * 0.85f,
                    center = Offset(size.width * 0.85f, size.height * 0.08f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(SkyCyan.copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.45f),
                        radius = size.width * 0.7f
                    ),
                    radius = size.width * 0.7f,
                    center = Offset(size.width * 0.15f, size.height * 0.45f)
                )
            }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                QuickCommandBottomBar(
                    onOpenComposer = onOpenComposer,
                    onOpenComposerWithMic = onOpenComposerWithMic
                )
            }
        ) { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val isWide = maxWidth >= 700.dp

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = if (isWide) 920.dp else 640.dp)
                        .align(Alignment.TopCenter)
                        .testTag("dashboard_list"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item(key = "header") {
                        NexusHeaderBar(
                            readyCount = uiState.readiness?.readyCount ?: 0,
                            totalCount = uiState.readiness?.totalCount ?: 4,
                            onOpenReadiness = onOpenReadiness,
                            onPreviewRingNow = onPreviewRingNow,
                            onSchedule5sDemo = onSchedule5sDemo
                        )
                    }

                    item(key = "today_overview") {
                        TodayOverviewCard(overview = uiState.todayOverview)
                    }

                    item(key = "next_reminder") {
                        NextReminderSpotlightCard(
                            nextReminder = uiState.nextReminder,
                            nowMillis = uiState.nowMillis,
                            onRingNow = { uiState.nextReminder?.let { onRingReminderNow(it.id) } },
                            onComplete = { uiState.nextReminder?.let { onCompleteReminder(it.id) } },
                            onSnooze10m = { uiState.nextReminder?.let { onSnooze10m(it.id) } },
                            onCreateNew = onOpenComposer,
                            onLoadSamples = onLoadSampleSchedule
                        )
                    }

                    item(key = "today_timeline") {
                        TodayTimelineSection(
                            tasks = uiState.todayOverview.tasks,
                            onMarkComplete = onCompleteReminder,
                            onCancelTask = onCancelReminder
                        )
                    }

                    if (uiState.pendingAssignments.isNotEmpty()) {
                        item(key = "assignments_strip") {
                            PendingAssignmentsBanner(
                                assignments = uiState.pendingAssignments,
                                nowMillis = uiState.nowMillis,
                                onComplete = onCompleteReminder,
                                onRingNow = onRingReminderNow
                            )
                        }
                    }

                    item(key = "filter_row") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ReminderFilter.entries.forEach { filter ->
                                val count = when (filter) {
                                    ReminderFilter.UPCOMING -> uiState.allReminders.count { it.isPending }
                                    ReminderFilter.ASSIGNMENTS -> uiState.allReminders.count {
                                        it.isAssignment || it.category == ReminderCategory.ASSIGNMENT
                                    }
                                    ReminderFilter.COMPLETED -> uiState.allReminders.count {
                                        it.status == ReminderStatus.COMPLETED ||
                                            it.status == ReminderStatus.CANCELLED ||
                                            it.status == ReminderStatus.DISMISSED
                                    }
                                    ReminderFilter.ALL -> uiState.allReminders.size
                                }
                                FilterChip(
                                    selected = uiState.selectedFilter == filter,
                                    onClick = { onSelectFilter(filter) },
                                    label = { Text("${filter.label} ($count)") },
                                    modifier = Modifier.testTag("filter_${filter.name.lowercase()}"),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TealContainer,
                                        selectedLabelColor = ElectricTeal,
                                        containerColor = SurfaceNavy,
                                        labelColor = TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = uiState.selectedFilter == filter,
                                        borderColor = BorderNavy,
                                        selectedBorderColor = ElectricTeal.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }
                    }

                    if (uiState.filteredReminders.isEmpty()) {
                        item(key = "empty_list") {
                            EmptyFilterCard(
                                filter = uiState.selectedFilter,
                                onCreateNew = onOpenComposer,
                                onLoadSamples = onLoadSampleSchedule
                            )
                        }
                    } else {
                        items(
                            items = uiState.filteredReminders,
                            key = { it.id }
                        ) { reminder ->
                            ReminderListCard(
                                reminder = reminder,
                                nowMillis = uiState.nowMillis,
                                onComplete = { onCompleteReminder(reminder.id) },
                                onCancel = { onCancelReminder(reminder.id) },
                                onSnooze10m = { onSnooze10m(reminder.id) },
                                onReschedule = {
                                    val cal = Calendar.getInstance().apply {
                                        timeInMillis = maxOf(reminder.effectiveDueMillis, System.currentTimeMillis())
                                    }
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, day ->
                                            val picked = Calendar.getInstance().apply {
                                                timeInMillis = cal.timeInMillis
                                                set(Calendar.YEAR, year)
                                                set(Calendar.MONTH, month)
                                                set(Calendar.DAY_OF_MONTH, day)
                                            }
                                            TimePickerDialog(
                                                context,
                                                { _, hour, minute ->
                                                    picked.set(Calendar.HOUR_OF_DAY, hour)
                                                    picked.set(Calendar.MINUTE, minute)
                                                    picked.set(Calendar.SECOND, 0)
                                                    picked.set(Calendar.MILLISECOND, 0)
                                                    onRescheduleReminder(reminder.id, picked.timeInMillis)
                                                },
                                                picked.get(Calendar.HOUR_OF_DAY),
                                                picked.get(Calendar.MINUTE),
                                                false
                                            ).show()
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                onRingNow = { onRingReminderNow(reminder.id) },
                                onDelete = { onDeleteReminder(reminder.id) }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun NexusHeaderBar(
    readyCount: Int,
    totalCount: Int,
    onOpenReadiness: () -> Unit,
    onPreviewRingNow: () -> Unit,
    onSchedule5sDemo: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "NEXUS AI",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Surface(
                        color = TealContainer,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = stringResource(R.string.offline_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricTeal,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.nexus_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Surface(
                onClick = onOpenReadiness,
                color = if (readyCount == totalCount) TealContainer else AmberContainer,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    1.dp,
                    if (readyCount == totalCount) ElectricTeal.copy(alpha = 0.45f) else AmberSignal.copy(alpha = 0.5f)
                ),
                modifier = Modifier.testTag("readiness_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = "Permission Readiness",
                        tint = if (readyCount == totalCount) ElectricTeal else AmberSignal,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "$readyCount/$totalCount Ready",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onPreviewRingNow,
                modifier = Modifier.testTag("preview_ring_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.45f))
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhoneInTalk,
                    contentDescription = null,
                    tint = ElectricTeal,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Preview Local Alarm", color = ElectricTeal)
            }

            OutlinedButton(
                onClick = onSchedule5sDemo,
                modifier = Modifier.testTag("schedule_5s_demo_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderNavy)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Timer,
                    contentDescription = null,
                    tint = SkyCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Test 5s Local Alarm", color = SkyCyan)
            }
        }
    }
}

@Composable
private fun NextReminderSpotlightCard(
    nextReminder: ReminderEntity?,
    nowMillis: Long,
    onRingNow: () -> Unit,
    onComplete: () -> Unit,
    onSnooze10m: () -> Unit,
    onCreateNew: () -> Unit,
    onLoadSamples: () -> Unit
) {
    Surface(
        color = MidnightNavy,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("next_reminder_card")
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(SurfaceNavy, MidnightNavy)
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (nextReminder != null) ElectricTeal else TextSecondary)
                    )
                    Text(
                        text = stringResource(R.string.next_reminder_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricTeal
                    )
                }

                if (nextReminder != null) {
                    Surface(
                        color = TealContainer,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = formatRelativeCountdown(nextReminder.effectiveDueMillis, nowMillis),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = ElectricTeal,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (nextReminder != null) {
                AdaptiveText(
                    text = nextReminder.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    preferDisplay = true
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = SkyCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = NaturalLanguageReminderParser.formatDateTimeShort(nextReminder.effectiveDueMillis),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SkyCyan
                    )
                    CategoryBadge(
                        category = nextReminder.category,
                        isAssignment = nextReminder.isAssignment
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onRingNow,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricTeal,
                            contentColor = ObsidianNavy
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ring Alarm", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onSnooze10m,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, BorderNavy)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Snooze,
                            contentDescription = "Snooze 10 minutes",
                            tint = AmberSignal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+10m", color = AmberSignal)
                    }

                    OutlinedButton(
                        onClick = onComplete,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, BorderNavy)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Mark complete",
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.no_upcoming_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = stringResource(R.string.no_upcoming_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onCreateNew,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricTeal,
                            contentColor = ObsidianNavy
                        )
                    ) {
                        Text("Schedule Reminder", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onLoadSamples,
                        modifier = Modifier.testTag("load_samples_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, BorderNavy)
                    ) {
                        Text(stringResource(R.string.load_samples_cta), color = SkyCyan)
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingAssignmentsBanner(
    assignments: List<ReminderEntity>,
    nowMillis: Long,
    onComplete: (Long) -> Unit,
    onRingNow: (Long) -> Unit
) {
    Surface(
        color = SurfaceNavy,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, AmberSignal.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pending_assignments_section")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Assignment,
                        contentDescription = null,
                        tint = AmberSignal,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.pending_assignments_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
                Surface(
                    color = AmberContainer,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = "${assignments.size} open",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AmberSignal,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                assignments.forEach { item ->
                    Surface(
                        color = ElevatedNavy,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, BorderNavy),
                        modifier = Modifier.width(248.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AdaptiveText(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                maxLines = 2
                            )
                            Text(
                                text = "Deadline: ${NaturalLanguageReminderParser.formatDateTimeShort(item.effectiveDueMillis)} • ${
                                    formatRelativeCountdown(item.effectiveDueMillis, nowMillis)
                                }",
                                style = MaterialTheme.typography.labelMedium,
                                color = AmberSignal
                            )
                            Text(
                                text = "Advance heads-up: ${item.advanceMinutes}m before",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = { onRingNow(item.id) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Ring", color = ElectricTeal)
                                }
                                TextButton(
                                    onClick = { onComplete(item.id) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Complete", color = EmeraldSuccess)
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
private fun ReminderListCard(
    reminder: ReminderEntity,
    nowMillis: Long,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    onSnooze10m: () -> Unit,
    onReschedule: () -> Unit,
    onRingNow: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = SurfaceNavy,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, BorderNavy),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reminder_card_${reminder.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryBadge(
                    category = reminder.category,
                    isAssignment = reminder.isAssignment
                )
                StatusPill(
                    status = reminder.status,
                    snoozeCount = reminder.snoozeCount
                )
                Spacer(modifier = Modifier.weight(1f))
                if (reminder.isPending) {
                    Text(
                        text = formatRelativeCountdown(reminder.effectiveDueMillis, nowMillis),
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricTeal
                    )
                }
            }

            AdaptiveText(
                text = reminder.title,
                style = MaterialTheme.typography.titleLarge,
                color = if (reminder.isPending) TextPrimary else TextSecondary
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = (if (reminder.isAssignment) "Deadline: " else "Due: ") +
                        NaturalLanguageReminderParser.formatDateTimeShort(reminder.effectiveDueMillis) +
                        " • ${reminder.advanceMinutes}m advance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            AnimatedVisibility(visible = reminder.notes.isNotBlank()) {
                AdaptiveText(
                    text = reminder.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 2
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (reminder.isPending) {
                        TextButton(
                            onClick = onComplete,
                            modifier = Modifier.testTag("complete_btn_${reminder.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Done", color = EmeraldSuccess)
                        }
                        TextButton(
                            onClick = onCancel,
                            modifier = Modifier.testTag("cancel_btn_${reminder.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Cancel,
                                contentDescription = null,
                                tint = CoralAlert,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancel", color = CoralAlert)
                        }
                        TextButton(
                            onClick = onSnooze10m,
                            modifier = Modifier.testTag("snooze_btn_${reminder.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Snooze,
                                contentDescription = null,
                                tint = AmberSignal,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+10m", color = AmberSignal)
                        }
                    }
                    TextButton(
                        onClick = onReschedule,
                        modifier = Modifier.testTag("reschedule_btn_${reminder.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EditCalendar,
                            contentDescription = null,
                            tint = SkyCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (reminder.isPending) "Edit" else "Re-add", color = SkyCyan)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (reminder.isPending) {
                        IconButton(
                            onClick = onRingNow,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PhoneInTalk,
                                contentDescription = "Preview local app alarm",
                                tint = ElectricTeal
                            )
                        }
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("delete_btn_${reminder.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete reminder",
                            tint = CoralAlert
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFilterCard(
    filter: ReminderFilter,
    onCreateNew: () -> Unit,
    onLoadSamples: () -> Unit
) {
    Surface(
        color = SurfaceNavy,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, BorderNavy),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.NotificationsActive,
                contentDescription = null,
                tint = ElectricTeal,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = "No ${filter.label.lowercase()} reminders",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = "Tap the composer below to type or speak a reminder, or load sample tasks.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onCreateNew) {
                    Text("Create Reminder", color = ElectricTeal)
                }
                TextButton(onClick = onLoadSamples) {
                    Text("Load Samples", color = SkyCyan)
                }
            }
        }
    }
}

@Composable
private fun QuickCommandBottomBar(
    onOpenComposer: () -> Unit,
    onOpenComposerWithMic: () -> Unit
) {
    Surface(
        color = MidnightNavy.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, BorderNavy),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                onClick = onOpenComposer,
                color = SurfaceNavy,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderNavy),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("quick_command_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = null,
                        tint = ElectricTeal,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Remind me at 6 AM to…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            FloatingActionButton(
                onClick = onOpenComposerWithMic,
                containerColor = TealContainer,
                contentColor = ElectricTeal,
                modifier = Modifier
                    .size(52.dp)
                    .testTag("quick_mic_fab")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = "Voice reminder"
                )
            }

            FloatingActionButton(
                onClick = onOpenComposer,
                containerColor = ElectricTeal,
                contentColor = ObsidianNavy,
                modifier = Modifier
                    .size(52.dp)
                    .testTag("new_reminder_fab")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "New reminder"
                )
            }
        }
    }
}
