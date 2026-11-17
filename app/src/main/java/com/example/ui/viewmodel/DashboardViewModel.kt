package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NexusApplication
import com.example.data.ReminderCategory
import com.example.data.ReminderEntity
import com.example.data.ReminderRepository
import com.example.data.ReminderStatus
import com.example.domain.CapabilityReadiness
import com.example.domain.DailyScheduleCalculator
import com.example.domain.NaturalLanguageReminderParser
import com.example.domain.ParsedReminder
import com.example.domain.PermissionManagerHelper
import com.example.domain.TodayOverviewSummary
import com.example.platform.ReminderNotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ReminderFilter(val label: String) {
    UPCOMING("Upcoming"),
    ASSIGNMENTS("Assignments"),
    COMPLETED("Completed"),
    ALL("All")
}

data class DashboardUiState(
    val allReminders: List<ReminderEntity> = emptyList(),
    val filteredReminders: List<ReminderEntity> = emptyList(),
    val nextReminder: ReminderEntity? = null,
    val pendingAssignments: List<ReminderEntity> = emptyList(),
    val todayOverview: TodayOverviewSummary = DailyScheduleCalculator.calculate(emptyList()),
    val selectedFilter: ReminderFilter = ReminderFilter.UPCOMING,
    val readiness: CapabilityReadiness? = null,
    val composerText: String = "",
    val parsedPreview: ParsedReminder? = null,
    val overrideScheduledAtMillis: Long? = null,
    val overrideCategory: ReminderCategory? = null,
    val overrideAdvanceMinutes: Int? = null,
    val composerNotes: String = "",
    val speechLanguageTag: String = "en-IN",
    val statusMessage: String? = null,
    val nowMillis: Long = System.currentTimeMillis()
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ReminderRepository =
        (application as NexusApplication).repository

    private val selectedFilter = MutableStateFlow(ReminderFilter.UPCOMING)
    private val readiness = MutableStateFlow(PermissionManagerHelper.inspect(application))
    private val composerText = MutableStateFlow("")
    private val overrideScheduledAtMillis = MutableStateFlow<Long?>(null)
    private val overrideCategory = MutableStateFlow<ReminderCategory?>(null)
    private val overrideAdvanceMinutes = MutableStateFlow<Int?>(null)
    private val composerNotes = MutableStateFlow("")
    private val speechLanguageTag = MutableStateFlow("en-IN")
    private val statusMessage = MutableStateFlow<String?>(null)
    private val nowTicker = MutableStateFlow(System.currentTimeMillis())

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(15_000L)
                nowTicker.value = System.currentTimeMillis()
            }
        }
    }

    private data class ComposerDraft(
        val text: String,
        val overrideTime: Long?,
        val overrideCat: ReminderCategory?,
        val overrideAdvance: Int?,
        val notes: String,
        val languageTag: String
    )

    private val draftFlow = combine(
        composerText,
        overrideScheduledAtMillis,
        overrideCategory,
        overrideAdvanceMinutes,
        combine(composerNotes, speechLanguageTag) { n, l -> n to l }
    ) { text, overrideTime, overrideCat, overrideAdvance, notesAndLang ->
        ComposerDraft(
            text = text,
            overrideTime = overrideTime,
            overrideCat = overrideCat,
            overrideAdvance = overrideAdvance,
            notes = notesAndLang.first,
            languageTag = notesAndLang.second
        )
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.allReminders,
        selectedFilter,
        readiness,
        draftFlow,
        combine(statusMessage, nowTicker) { msg, now -> msg to now }
    ) { reminders, filter, capReadiness, draft, msgAndNow ->
        val (msg, now) = msgAndNow
        val pending = reminders
            .filter { it.isPending }
            .sortedBy { it.effectiveDueMillis }

        val next = pending.firstOrNull()
        val assignments = pending.filter { it.isAssignment || it.category == ReminderCategory.ASSIGNMENT }
        val todayOverview = DailyScheduleCalculator.calculate(reminders, nowMillis = now)

        val filtered = when (filter) {
            ReminderFilter.UPCOMING -> pending
            ReminderFilter.ASSIGNMENTS -> reminders.filter {
                it.isAssignment || it.category == ReminderCategory.ASSIGNMENT
            }.sortedBy { it.effectiveDueMillis }
            ReminderFilter.COMPLETED -> reminders.filter {
                it.status == ReminderStatus.COMPLETED ||
                    it.status == ReminderStatus.CANCELLED ||
                    it.status == ReminderStatus.DISMISSED
            }.sortedByDescending { it.completedAtMillis ?: it.effectiveDueMillis }
            ReminderFilter.ALL -> reminders.sortedBy { it.effectiveDueMillis }
        }

        val parsed = if (draft.text.isNotBlank()) {
            NaturalLanguageReminderParser.parse(draft.text, now)
        } else {
            null
        }

        DashboardUiState(
            allReminders = reminders,
            filteredReminders = filtered,
            nextReminder = next,
            pendingAssignments = assignments,
            todayOverview = todayOverview,
            selectedFilter = filter,
            readiness = capReadiness,
            composerText = draft.text,
            parsedPreview = parsed,
            overrideScheduledAtMillis = draft.overrideTime,
            overrideCategory = draft.overrideCat,
            overrideAdvanceMinutes = draft.overrideAdvance,
            composerNotes = draft.notes,
            speechLanguageTag = draft.languageTag,
            statusMessage = msg,
            nowMillis = now
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = DashboardUiState(
            readiness = PermissionManagerHelper.inspect(application)
        )
    )

    fun refreshReadiness() {
        readiness.value = PermissionManagerHelper.inspect(getApplication())
        nowTicker.value = System.currentTimeMillis()
    }

    fun selectFilter(filter: ReminderFilter) {
        selectedFilter.value = filter
    }

    fun updateComposerText(text: String) {
        composerText.value = text
    }

    fun setOverrideScheduledAt(millis: Long?) {
        overrideScheduledAtMillis.value = millis
    }

    fun setOverrideCategory(category: ReminderCategory?) {
        overrideCategory.value = category
        // Reset advance override so assignment/task default updates cleanly
        overrideAdvanceMinutes.value = null
    }

    fun setOverrideAdvanceMinutes(minutes: Int?) {
        overrideAdvanceMinutes.value = minutes
    }

    fun setComposerNotes(notes: String) {
        composerNotes.value = notes
    }

    fun setSpeechLanguage(tag: String) {
        speechLanguageTag.value = tag
    }

    fun resetComposer() {
        composerText.value = ""
        overrideScheduledAtMillis.value = null
        overrideCategory.value = null
        overrideAdvanceMinutes.value = null
        composerNotes.value = ""
    }

    fun clearStatusMessage() {
        statusMessage.value = null
    }

    fun showStatus(message: String) {
        statusMessage.value = message
    }

    fun scheduleFromComposer(onCreated: (Long, Boolean) -> Unit = { _, _ -> }) {
        val raw = composerText.value.trim()
        if (raw.isBlank()) {
            statusMessage.value = "Enter or dictate a reminder first."
            return
        }
        val now = System.currentTimeMillis()
        val parsed = NaturalLanguageReminderParser.parse(raw, now)
        val overrideTime = overrideScheduledAtMillis.value
        val overrideCat = overrideCategory.value
        val isAssignment = (overrideCat ?: parsed.category) == ReminderCategory.ASSIGNMENT || parsed.isAssignment
        val advanceMins = overrideAdvanceMinutes.value ?: if (isAssignment) 45 else 20
        val notes = composerNotes.value.trim()

        viewModelScope.launch {
            val result = repository.createReminder(
                parsed = parsed,
                overrideScheduledAtMillis = overrideTime,
                overrideCategory = overrideCat,
                overrideAdvanceMinutes = advanceMins,
                assignmentDeadlineMillis = if (isAssignment) (overrideTime ?: parsed.scheduledAtMillis) else null,
                notes = notes,
                nowMillis = now
            )
            resetComposer()
            nowTicker.value = System.currentTimeMillis()

            val dueFormatted = NaturalLanguageReminderParser.formatDateTimeShort(
                result.scheduleResult.scheduledAtMillis
            )
            val advNote = if (result.scheduleResult.advanceScheduledAtMillis != null) {
                " + ${advanceMins}m advance heads-up"
            } else {
                ""
            }
            statusMessage.value = if (result.scheduleResult.exactAlarmUsed) {
                "Local app alarm scheduled for $dueFormatted$advNote"
            } else {
                "Saved for $dueFormatted$advNote (inexact battery-saver delivery)"
            }
            onCreated(result.reminder.id, result.scheduleResult.exactAlarmUsed)
        }
    }

    fun markComplete(id: Long) {
        viewModelScope.launch {
            ReminderNotificationHelper.cancelNotification(getApplication(), id)
            repository.markComplete(id)
            nowTicker.value = System.currentTimeMillis()
            statusMessage.value = "Task completed. Pending alarms cancelled."
        }
    }

    fun cancelReminder(id: Long) {
        viewModelScope.launch {
            ReminderNotificationHelper.cancelNotification(getApplication(), id)
            repository.cancelReminder(id)
            nowTicker.value = System.currentTimeMillis()
            statusMessage.value = "Task cancelled. Advance & due alarms cleared."
        }
    }

    fun snoozeMinutes(id: Long, minutes: Int) {
        viewModelScope.launch {
            ReminderNotificationHelper.cancelNotification(getApplication(), id)
            val result = repository.snoozeByMinutes(id, minutes)
            nowTicker.value = System.currentTimeMillis()
            if (result != null) {
                val formatted = NaturalLanguageReminderParser.formatDateTimeShort(result.scheduledAtMillis)
                statusMessage.value = "Snoozed +${minutes}m until $formatted"
            }
        }
    }

    fun rescheduleTo(id: Long, newMillis: Long) {
        viewModelScope.launch {
            ReminderNotificationHelper.cancelNotification(getApplication(), id)
            val result = repository.rescheduleTo(id, newMillis)
            nowTicker.value = System.currentTimeMillis()
            if (result != null) {
                val formatted = NaturalLanguageReminderParser.formatDateTimeShort(result.scheduledAtMillis)
                statusMessage.value = "Rescheduled to $formatted"
            }
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            ReminderNotificationHelper.cancelNotification(getApplication(), id)
            repository.deleteReminder(id)
            nowTicker.value = System.currentTimeMillis()
            statusMessage.value = "Reminder deleted."
        }
    }

    fun seedSampleSchedule() {
        viewModelScope.launch {
            repository.seedSampleSchedule()
            nowTicker.value = System.currentTimeMillis()
            statusMessage.value = "Sample reminders & assignments loaded locally."
        }
    }

    fun createAndScheduleDelayedDemo(
        delaySeconds: Int = 5,
        onScheduled: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val targetMillis = now + delaySeconds * 1_000L
            val parsed = NaturalLanguageReminderParser.parse(
                "Remind me to finish my mathematics assignment",
                now
            )
            val created = repository.createReminder(
                parsed = parsed,
                overrideScheduledAtMillis = targetMillis,
                overrideCategory = ReminderCategory.ASSIGNMENT,
                overrideAdvanceMinutes = 45,
                assignmentDeadlineMillis = targetMillis,
                notes = "Scheduled ${delaySeconds}s demo trigger",
                nowMillis = now
            )
            nowTicker.value = System.currentTimeMillis()
            statusMessage.value = "Demo local app alarm will ring in ${delaySeconds}s…"
            onScheduled(created.reminder.id)
        }
    }

    fun ensurePreviewReminder(onReady: (Long) -> Unit) {
        viewModelScope.launch {
            val existing = uiState.value.nextReminder
            if (existing != null) {
                onReady(existing.id)
            } else {
                val now = System.currentTimeMillis()
                val parsed = NaturalLanguageReminderParser.parse(
                    "Remind me at 6 AM to finish my mathematics assignment",
                    now
                )
                val created = repository.createReminder(parsed = parsed, nowMillis = now)
                onReady(created.reminder.id)
            }
        }
    }
}
