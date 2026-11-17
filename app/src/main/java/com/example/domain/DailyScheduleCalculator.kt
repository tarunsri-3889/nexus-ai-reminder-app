package com.example.domain

import com.example.data.ReminderEntity
import com.example.data.ReminderStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class TodayTaskDisplayStatus(val label: String) {
    UPCOMING("Upcoming"),
    DUE("Due Now"),
    OVERDUE("Overdue"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled")
}

data class NumberedTodayTask(
    val taskNumber: Int,
    val reminder: ReminderEntity,
    val displayStatus: TodayTaskDisplayStatus,
    val formattedDueTime: String
)

data class TodayOverviewSummary(
    val todayDateLabel: String,
    val timezoneLabel: String,
    val tasks: List<NumberedTodayTask>,
    val completedCount: Int,
    val totalCount: Int,
    val cancelledCount: Int,
    val progressPercent: Int,
    val dayKey: String
)

object DailyScheduleCalculator {

    fun calculate(
        reminders: List<ReminderEntity>,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.getDefault()
    ): TodayOverviewSummary {
        val startCal = Calendar.getInstance(timeZone, locale).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDayMillis = startCal.timeInMillis
        val endOfDayMillis = (startCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

        val dateFmt = SimpleDateFormat("EEEE, MMM d, yyyy", locale).apply {
            this.timeZone = timeZone
        }
        val timeFmt = SimpleDateFormat("h:mm a", locale).apply {
            this.timeZone = timeZone
        }
        val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            this.timeZone = timeZone
        }

        val todayReminders = reminders
            .filter { entity ->
                val due = entity.effectiveDueMillis
                due in startOfDayMillis until endOfDayMillis
            }
            .sortedWith(compareBy<ReminderEntity> { it.effectiveDueMillis }.thenBy { it.id })

        val numbered = todayReminders.mapIndexed { index, entity ->
            val displayStatus = resolveDisplayStatus(entity, nowMillis)
            NumberedTodayTask(
                taskNumber = index + 1,
                reminder = entity,
                displayStatus = displayStatus,
                formattedDueTime = timeFmt.format(Date(entity.effectiveDueMillis))
            )
        }

        val cancelledCount = numbered.count { it.displayStatus == TodayTaskDisplayStatus.CANCELLED }
        val totalCount = (numbered.size - cancelledCount).coerceAtLeast(0)
        val completedCount = numbered.count { it.displayStatus == TodayTaskDisplayStatus.COMPLETED }
        val progressPercent = if (totalCount == 0) {
            0
        } else {
            ((completedCount * 100f) / totalCount).toInt().coerceIn(0, 100)
        }

        return TodayOverviewSummary(
            todayDateLabel = dateFmt.format(Date(nowMillis)),
            timezoneLabel = timeZone.getDisplayName(false, TimeZone.SHORT, locale),
            tasks = numbered,
            completedCount = completedCount,
            totalCount = totalCount,
            cancelledCount = cancelledCount,
            progressPercent = progressPercent,
            dayKey = keyFmt.format(Date(nowMillis))
        )
    }

    fun resolveDisplayStatus(
        entity: ReminderEntity,
        nowMillis: Long
    ): TodayTaskDisplayStatus {
        return when (entity.status) {
            ReminderStatus.COMPLETED -> TodayTaskDisplayStatus.COMPLETED
            ReminderStatus.CANCELLED, ReminderStatus.DISMISSED -> TodayTaskDisplayStatus.CANCELLED
            ReminderStatus.RINGING -> TodayTaskDisplayStatus.DUE
            ReminderStatus.OVERDUE -> TodayTaskDisplayStatus.OVERDUE
            ReminderStatus.UPCOMING, ReminderStatus.SNOOZED -> {
                val due = entity.effectiveDueMillis
                when {
                    due < nowMillis - 60_000L -> TodayTaskDisplayStatus.OVERDUE
                    due <= nowMillis + 60_000L -> TodayTaskDisplayStatus.DUE
                    else -> TodayTaskDisplayStatus.UPCOMING
                }
            }
        }
    }
}
