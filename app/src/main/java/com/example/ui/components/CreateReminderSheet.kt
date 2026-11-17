package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ReminderCategory
import com.example.domain.CapabilityReadiness
import com.example.domain.NaturalLanguageReminderParser
import com.example.domain.ParsedReminder
import com.example.domain.ReminderAlarmScheduler
import com.example.platform.SpeechInputState
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberSignal
import com.example.ui.theme.BorderNavy
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.ElevatedNavy
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.SkyCyan
import com.example.ui.theme.SurfaceNavy
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.adaptiveFontFamily
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReminderSheet(
    composerText: String,
    parsedPreview: ParsedReminder?,
    overrideScheduledAtMillis: Long?,
    overrideCategory: ReminderCategory?,
    overrideAdvanceMinutes: Int?,
    speechLanguageTag: String,
    speechState: SpeechInputState,
    readiness: CapabilityReadiness?,
    onTextChange: (String) -> Unit,
    onSelectTimeOverride: (Long?) -> Unit,
    onSelectCategoryOverride: (ReminderCategory?) -> Unit,
    onSelectAdvanceMinutes: (Int?) -> Unit,
    onSelectLanguage: (String) -> Unit,
    onMicClick: () -> Unit,
    onStopListening: () -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val effectiveTime = overrideScheduledAtMillis
        ?: parsedPreview?.scheduledAtMillis
        ?: (System.currentTimeMillis() + 60 * 60_000L)
    val effectiveCategory = overrideCategory
        ?: parsedPreview?.category
        ?: ReminderCategory.PERSONAL
    val effectiveIsAssignment = effectiveCategory == ReminderCategory.ASSIGNMENT ||
        (parsedPreview?.isAssignment == true && overrideCategory == null)

    val advanceChoices = if (effectiveIsAssignment) listOf(30, 45) else listOf(15, 20)
    val defaultAdvance = if (effectiveIsAssignment) 45 else 20
    val effectiveAdvance = overrideAdvanceMinutes ?: defaultAdvance

    val alarmPlan = ReminderAlarmScheduler.computeAlarmTimes(
        dueAtMillis = effectiveTime,
        advanceMinutes = effectiveAdvance,
        nowMillis = System.currentTimeMillis()
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MidnightNavy,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (effectiveIsAssignment) "Schedule Assignment Deadline" else "Schedule Reminder",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = "Speak or type naturally in English or हिन्दी",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = speechLanguageTag == "en-IN",
                        onClick = { onSelectLanguage("en-IN") },
                        label = { Text("EN") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealContainer,
                            selectedLabelColor = ElectricTeal
                        )
                    )
                    FilterChip(
                        selected = speechLanguageTag == "hi-IN",
                        onClick = { onSelectLanguage("hi-IN") },
                        label = { Text("हिन्दी") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealContainer,
                            selectedLabelColor = ElectricTeal
                        )
                    )
                }
            }

            // Quick Example Prompts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val examples = listOf(
                    "Remind me at 6 AM to finish my mathematics assignment",
                    "In 30 minutes revise physics notes",
                    "कल सुबह 6 बजे गणित असाइनमेंट पूरा करें"
                )
                examples.forEach { sample ->
                    Surface(
                        onClick = {
                            onSelectTimeOverride(null)
                            onSelectCategoryOverride(null)
                            onSelectAdvanceMinutes(null)
                            onTextChange(sample)
                        },
                        color = SurfaceNavy,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, BorderNavy)
                    ) {
                        AdaptiveText(
                            text = sample,
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricTeal,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Input Box + Contextual Voice Mic Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                OutlinedTextField(
                    value = composerText,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("composer_input"),
                    placeholder = {
                        Text(
                            text = "e.g., Remind me at 6 AM to finish my mathematics assignment",
                            color = TextSecondary
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = adaptiveFontFamily(composerText),
                        color = TextPrimary
                    ),
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricTeal,
                        unfocusedBorderColor = BorderNavy,
                        focusedContainerColor = SurfaceNavy,
                        unfocusedContainerColor = SurfaceNavy,
                        cursorColor = ElectricTeal
                    )
                )

                val isListening = speechState is SpeechInputState.Listening ||
                    speechState is SpeechInputState.Partial
                val micAvailable = readiness?.speechRecognitionAvailable != false

                IconButton(
                    onClick = {
                        if (isListening) onStopListening() else onMicClick()
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("composer_mic_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = when {
                            isListening -> CoralAlert
                            micAvailable -> TealContainer
                            else -> ElevatedNavy
                        },
                        contentColor = when {
                            isListening -> TextPrimary
                            micAvailable -> ElectricTeal
                            else -> TextSecondary
                        }
                    )
                ) {
                    Icon(
                        imageVector = when {
                            isListening -> Icons.Rounded.Stop
                            micAvailable -> Icons.Rounded.Mic
                            else -> Icons.Rounded.MicOff
                        },
                        contentDescription = if (isListening) "Stop voice input" else "Dictate reminder"
                    )
                }
            }

            AnimatedVisibility(visible = speechState !is SpeechInputState.Idle) {
                val (msg, isError) = when (speechState) {
                    is SpeechInputState.Listening -> "Listening locally… speak your reminder now." to false
                    is SpeechInputState.Partial -> "Hearing: \"${speechState.text}\"" to false
                    is SpeechInputState.Result -> "Captured: \"${speechState.text}\"" to false
                    is SpeechInputState.Error -> speechState.message to true
                    SpeechInputState.Idle -> "" to false
                }
                Surface(
                    color = if (isError) CoralContainer else TealContainer,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (isError) CoralAlert.copy(alpha = 0.5f) else ElectricTeal.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isError) TextPrimary else ElectricTeal,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Parsed NLP Preview Card
            if (parsedPreview != null) {
                Surface(
                    color = SurfaceNavy,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = ElectricTeal,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "NEXUS Parsed Preview",
                                style = MaterialTheme.typography.labelLarge,
                                color = ElectricTeal
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            CategoryBadge(
                                category = effectiveCategory,
                                isAssignment = effectiveIsAssignment
                            )
                        }

                        AdaptiveText(
                            text = parsedPreview.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = AmberSignal,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = (if (effectiveIsAssignment) "Deadline: " else "Due: ") +
                                    NaturalLanguageReminderParser.formatDateTimeShort(effectiveTime),
                                style = MaterialTheme.typography.bodyMedium,
                                color = AmberSignal
                            )
                        }
                    }
                }
            }

            // Category chips
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReminderCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = effectiveCategory == cat,
                        onClick = { onSelectCategoryOverride(cat) },
                        label = { Text(cat.displayName) },
                        modifier = Modifier.testTag("category_chip_${cat.name.lowercase()}"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (cat == ReminderCategory.ASSIGNMENT) AmberContainer else TealContainer,
                            selectedLabelColor = if (cat == ReminderCategory.ASSIGNMENT) AmberSignal else ElectricTeal
                        )
                    )
                }
            }

            // Scheduled Time / Assignment Deadline Picker
            Text(
                text = if (effectiveIsAssignment) "Assignment Deadline" else "Scheduled Time",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = { onSelectTimeOverride(System.currentTimeMillis() + 30 * 60_000L) },
                    color = ElevatedNavy,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "In 30 min",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                    )
                }

                Surface(
                    onClick = { onSelectTimeOverride(System.currentTimeMillis() + 60 * 60_000L) },
                    color = ElevatedNavy,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "In 1 hour",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                    )
                }

                Surface(
                    onClick = {
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 6)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                            if (timeInMillis <= System.currentTimeMillis()) {
                                add(Calendar.DAY_OF_YEAR, 1)
                            }
                        }
                        onSelectTimeOverride(cal.timeInMillis)
                    },
                    color = ElevatedNavy,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Next 6:00 AM",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                    )
                }

                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = effectiveTime }
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val picked = Calendar.getInstance().apply {
                                    timeInMillis = effectiveTime
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
                                        onSelectTimeOverride(picked.timeInMillis)
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
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderNavy)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = ElectricTeal
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (effectiveIsAssignment) "Set Deadline" else "Pick Date & Time",
                        color = ElectricTeal
                    )
                }
            }

            // Advance Reminder Chips (15/20m for Tasks, 30/45m for Assignments)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.NotificationsActive,
                    contentDescription = null,
                    tint = SkyCyan,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = if (effectiveIsAssignment) {
                        "Assignment Advance Heads-Up (before deadline)"
                    } else {
                        "Task Advance Heads-Up (before due time)"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                advanceChoices.forEach { mins ->
                    val isDefault = mins == defaultAdvance
                    FilterChip(
                        selected = effectiveAdvance == mins,
                        onClick = { onSelectAdvanceMinutes(mins) },
                        label = {
                            Text(if (isDefault) "${mins} min before (Default)" else "${mins} min before")
                        },
                        modifier = Modifier.testTag("advance_chip_${mins}m"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealContainer,
                            selectedLabelColor = ElectricTeal
                        )
                    )
                }
            }

            // Transparent explanation of what will be scheduled
            Surface(
                color = SurfaceNavy,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (alarmPlan.advanceAtMillis != null) {
                        "Will schedule: gentle advance heads-up (${effectiveAdvance}m prior) + ringing local app alarm at due time."
                    } else {
                        "Due time is under ${effectiveAdvance}m away — scheduling only the due-time local app alarm (no duplicate)."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = SkyCyan,
                    modifier = Modifier.padding(11.dp)
                )
            }

            if (readiness != null && (!readiness.notificationsGranted || !readiness.exactAlarmsGranted)) {
                Surface(
                    color = AmberContainer,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, AmberSignal.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Tip: Notifications or Exact Alarms aren't enabled yet. You can still save now—NEXUS will guide you to grant them cleanly.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Button(
                onClick = onSubmit,
                enabled = composerText.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("schedule_reminder_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricTeal,
                    contentColor = ObsidianNavy
                )
            ) {
                Text(
                    text = stringResource(R.string.action_new_reminder) + " • Schedule Locally",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
