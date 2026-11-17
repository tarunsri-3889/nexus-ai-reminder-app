package com.example.domain

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.MainActivity
import com.example.data.ReminderEntity
import com.example.platform.ReminderAlarmReceiver

data class ScheduleResult(
    val scheduledAtMillis: Long,
    val advanceScheduledAtMillis: Long?,
    val exactAlarmUsed: Boolean
)

data class PlannedAlarmTimes(
    val dueAtMillis: Long?,
    val advanceAtMillis: Long?
)

interface ReminderAlarmScheduler {
    fun schedule(reminder: ReminderEntity, nowMillis: Long = System.currentTimeMillis()): ScheduleResult
    fun cancel(reminderId: Long)

    companion object {
        /**
         * Pure scheduling planner:
         * - Due alarm is scheduled only if due time is in the future (or immediate trigger).
         * - Advance alarm is scheduled ONLY if advanceMinutes > 0 AND (due - advanceMinutes) is strictly
         *   in the future and distinct from the due alarm (no duplicate triggers).
         */
        fun computeAlarmTimes(
            dueAtMillis: Long,
            advanceMinutes: Int,
            nowMillis: Long
        ): PlannedAlarmTimes {
            val safeDue = if (dueAtMillis > nowMillis) dueAtMillis else null
            val advanceCandidate = if (advanceMinutes > 0) {
                dueAtMillis - advanceMinutes * 60_000L
            } else {
                null
            }
            val safeAdvance = if (
                advanceCandidate != null &&
                advanceCandidate > nowMillis + 2_000L &&
                advanceCandidate < dueAtMillis - 2_000L
            ) {
                advanceCandidate
            } else {
                null
            }
            return PlannedAlarmTimes(
                dueAtMillis = safeDue,
                advanceAtMillis = safeAdvance
            )
        }
    }
}

class AndroidReminderScheduler(private val context: Context) : ReminderAlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(reminder: ReminderEntity, nowMillis: Long): ScheduleResult {
        // Cancel any stale alarms for this reminder first so re-scheduling never duplicates
        cancel(reminder.id)

        if (!reminder.isActiveForAlarm) {
            return ScheduleResult(
                scheduledAtMillis = reminder.effectiveDueMillis,
                advanceScheduledAtMillis = null,
                exactAlarmUsed = false
            )
        }

        val dueMillis = reminder.effectiveDueMillis
        val plan = ReminderAlarmScheduler.computeAlarmTimes(
            dueAtMillis = dueMillis,
            advanceMinutes = reminder.advanceMinutes,
            nowMillis = nowMillis
        )

        val exactPermitted = PermissionManagerHelper.canScheduleExactAlarms(context)
        var exactUsed = false

        // 1. Schedule due-time local app alarm
        val triggerDueAt = (plan.dueAtMillis ?: (nowMillis + 1_000L))
        val duePendingIntent = buildDuePendingIntent(reminder.id)
        exactUsed = scheduleSingleAlarm(
            reminderId = reminder.id,
            triggerAtMillis = triggerDueAt,
            pendingIntent = duePendingIntent,
            useAlarmClock = true,
            exactPermitted = exactPermitted
        )

        // 2. Schedule advance gentle reminder ONLY if strictly in the future
        plan.advanceAtMillis?.let { advanceAt ->
            val advancePendingIntent = buildAdvancePendingIntent(reminder.id, reminder.advanceMinutes)
            scheduleSingleAlarm(
                reminderId = reminder.id,
                triggerAtMillis = advanceAt,
                pendingIntent = advancePendingIntent,
                useAlarmClock = false,
                exactPermitted = exactPermitted
            )
        }

        return ScheduleResult(
            scheduledAtMillis = triggerDueAt,
            advanceScheduledAtMillis = plan.advanceAtMillis,
            exactAlarmUsed = exactUsed
        )
    }

    override fun cancel(reminderId: Long) {
        val duePi = buildDuePendingIntent(reminderId)
        alarmManager.cancel(duePi)
        duePi.cancel()

        val advPi = buildAdvancePendingIntent(reminderId, 0)
        alarmManager.cancel(advPi)
        advPi.cancel()
    }

    private fun scheduleSingleAlarm(
        reminderId: Long,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent,
        useAlarmClock: Boolean,
        exactPermitted: Boolean
    ): Boolean {
        if (exactPermitted) {
            if (useAlarmClock) {
                val showIntent = PendingIntent.getActivity(
                    context,
                    reminderId.toInt(),
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                runCatching {
                    alarmManager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
                        pendingIntent
                    )
                    return true
                }
            } else {
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                    }
                    return true
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
        return false
    }

    private fun buildDuePendingIntent(reminderId: Long): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_TRIGGER_REMINDER
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            dueRequestCode(reminderId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildAdvancePendingIntent(reminderId: Long, advanceMinutes: Int): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_ADVANCE_REMINDER
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderAlarmReceiver.EXTRA_ADVANCE_MINUTES, advanceMinutes)
        }
        return PendingIntent.getBroadcast(
            context,
            advanceRequestCode(reminderId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        fun dueRequestCode(reminderId: Long): Int = (reminderId * 2L).toInt()
        fun advanceRequestCode(reminderId: Long): Int = (reminderId * 2L + 1L).toInt()
    }
}
