package com.example.data

import com.example.domain.ParsedReminder
import com.example.domain.ReminderAlarmScheduler
import com.example.domain.ScheduleResult
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

data class CreatedReminderResult(
    val reminder: ReminderEntity,
    val scheduleResult: ScheduleResult
)

class ReminderRepository(
    private val dao: ReminderDao,
    private val scheduler: ReminderAlarmScheduler
) {
    val allReminders: Flow<List<ReminderEntity>> = dao.observeAllReminders()
    val pendingReminders: Flow<List<ReminderEntity>> = dao.observePendingReminders()

    fun observeReminder(id: Long): Flow<ReminderEntity?> = dao.observeReminderById(id)

    suspend fun getReminderById(id: Long): ReminderEntity? = dao.getReminderById(id)

    suspend fun createReminder(
        parsed: ParsedReminder,
        overrideScheduledAtMillis: Long? = null,
        overrideCategory: ReminderCategory? = null,
        overrideAdvanceMinutes: Int? = null,
        assignmentDeadlineMillis: Long? = null,
        notes: String = "",
        nowMillis: Long = System.currentTimeMillis()
    ): CreatedReminderResult {
        val category = overrideCategory ?: parsed.category
        val isAssignment = parsed.isAssignment || category == ReminderCategory.ASSIGNMENT
        val scheduledAt = when {
            isAssignment && assignmentDeadlineMillis != null -> assignmentDeadlineMillis
            overrideScheduledAtMillis != null -> overrideScheduledAtMillis
            else -> parsed.scheduledAtMillis
        }
        val deadline = if (isAssignment) (assignmentDeadlineMillis ?: scheduledAt) else null
        val advanceMins = overrideAdvanceMinutes ?: if (isAssignment) 45 else 20

        val entity = ReminderEntity(
            title = parsed.title,
            originalInput = parsed.originalInput,
            category = category,
            scheduledAtMillis = scheduledAt,
            createdAtMillis = nowMillis,
            status = ReminderStatus.UPCOMING,
            isAssignment = isAssignment,
            notes = notes,
            assignmentDeadlineMillis = deadline,
            advanceMinutes = advanceMins
        )
        val newId = dao.insertReminder(entity)
        val persisted = entity.copy(id = newId)
        val scheduleResult = scheduler.schedule(persisted, nowMillis)
        return CreatedReminderResult(persisted, scheduleResult)
    }

    suspend fun markComplete(id: Long) {
        val current = dao.getReminderById(id) ?: return
        scheduler.cancel(id)
        dao.updateReminder(
            current.copy(
                status = ReminderStatus.COMPLETED,
                completedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun cancelReminder(id: Long) {
        val current = dao.getReminderById(id) ?: return
        scheduler.cancel(id)
        dao.updateReminder(
            current.copy(
                status = ReminderStatus.CANCELLED
            )
        )
    }

    suspend fun dismissReminder(id: Long) {
        val current = dao.getReminderById(id) ?: return
        scheduler.cancel(id)
        if (current.isActiveForAlarm) {
            dao.updateReminder(
                current.copy(
                    status = ReminderStatus.OVERDUE
                )
            )
        }
    }

    suspend fun markRinging(id: Long): ReminderEntity? {
        val current = dao.getReminderById(id) ?: return null
        if (!current.isActiveForAlarm) return null
        val updated = current.copy(status = ReminderStatus.RINGING)
        dao.updateReminder(updated)
        return updated
    }

    suspend fun markOverdueIfActive(id: Long): ReminderEntity? {
        val current = dao.getReminderById(id) ?: return null
        if (!current.isActiveForAlarm) return null
        val updated = current.copy(status = ReminderStatus.OVERDUE)
        dao.updateReminder(updated)
        return updated
    }

    suspend fun snoozeByMinutes(id: Long, minutes: Int): ScheduleResult? {
        val current = dao.getReminderById(id) ?: return null
        if (!current.isActiveForAlarm && current.status != ReminderStatus.OVERDUE) return null
        val now = System.currentTimeMillis()
        val targetMillis = now + minutes * 60_000L
        val updated = current.copy(
            scheduledAtMillis = targetMillis,
            assignmentDeadlineMillis = if (current.isAssignment) targetMillis else current.assignmentDeadlineMillis,
            status = ReminderStatus.SNOOZED,
            snoozeCount = current.snoozeCount + 1
        )
        dao.updateReminder(updated)
        return scheduler.schedule(updated, now)
    }

    suspend fun rescheduleTomorrowMorning(id: Long, hourOfDay: Int = 6): ScheduleResult? {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return rescheduleTo(id, cal.timeInMillis)
    }

    suspend fun rescheduleTo(id: Long, newMillis: Long): ScheduleResult? {
        val current = dao.getReminderById(id) ?: return null
        val now = System.currentTimeMillis()
        val updated = current.copy(
            scheduledAtMillis = newMillis,
            assignmentDeadlineMillis = if (current.isAssignment) newMillis else current.assignmentDeadlineMillis,
            status = ReminderStatus.UPCOMING,
            completedAtMillis = null
        )
        dao.updateReminder(updated)
        return scheduler.schedule(updated, now)
    }

    suspend fun deleteReminder(id: Long) {
        scheduler.cancel(id)
        dao.deleteReminderById(id)
    }

    suspend fun rescheduleAllPending() {
        val now = System.currentTimeMillis()
        dao.getSchedulableRemindersOnce().forEach { reminder ->
            if (reminder.isActiveForAlarm && reminder.effectiveDueMillis > now) {
                scheduler.schedule(reminder, now)
            }
        }
    }

    suspend fun seedSampleSchedule() {
        if (dao.countReminders() > 0) return

        val now = System.currentTimeMillis()
        val calFirstToday = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.MINUTE, 50)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val calSecondToday = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.HOUR_OF_DAY, 2)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // Keep within today if late evening
            if (get(Calendar.DAY_OF_YEAR) != Calendar.getInstance().get(Calendar.DAY_OF_YEAR)) {
                timeInMillis = now + 25 * 60_000L
            }
        }
        val calTomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 6)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val samples = listOf(
            ReminderEntity(
                title = "Finish my mathematics assignment",
                originalInput = "Remind me to finish my mathematics assignment",
                category = ReminderCategory.ASSIGNMENT,
                scheduledAtMillis = calFirstToday.timeInMillis,
                assignmentDeadlineMillis = calFirstToday.timeInMillis,
                advanceMinutes = 45,
                isAssignment = true,
                notes = "Calculus problem set chapters 4 & 5"
            ),
            ReminderEntity(
                title = "Revise physics optics notes",
                originalInput = "Revise physics optics notes",
                category = ReminderCategory.STUDY,
                scheduledAtMillis = calSecondToday.timeInMillis,
                advanceMinutes = 20,
                isAssignment = false
            ),
            ReminderEntity(
                title = "भौतिक विज्ञान (Physics) लैब रिपोर्ट जमा करें",
                originalInput = "कल सुबह 6 बजे Physics लैब रिपोर्ट जमा करने की याद दिलाना",
                category = ReminderCategory.ASSIGNMENT,
                scheduledAtMillis = calTomorrow.timeInMillis,
                assignmentDeadlineMillis = calTomorrow.timeInMillis,
                advanceMinutes = 45,
                isAssignment = true,
                notes = "Optics experiment observation table"
            )
        )

        samples.forEach { entity ->
            val id = dao.insertReminder(entity)
            scheduler.schedule(entity.copy(id = id), now)
        }
    }
}
