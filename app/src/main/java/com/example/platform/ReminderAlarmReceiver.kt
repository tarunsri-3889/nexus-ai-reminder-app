package com.example.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.example.NexusApplication
import com.example.ReminderAlertActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

data class AdvanceVoiceAlertEvent(
    val reminderId: Long,
    val title: String,
    val advanceMinutes: Int,
    val isAssignment: Boolean
)

object AdvanceAlertBus {
    private val _events = MutableSharedFlow<AdvanceVoiceAlertEvent>(extraBufferCapacity = 4)
    val events = _events.asSharedFlow()

    fun emit(event: AdvanceVoiceAlertEvent) {
        _events.tryEmit(event)
    }
}

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId <= 0L) return

        val app = context.applicationContext as NexusApplication
        val repository = app.repository
        val pendingResult = goAsync()

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "nexusai:reminder_alarm_wakelock"
        ).apply { acquire(10_000L) }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_ADVANCE_REMINDER -> {
                        val reminder = repository.getReminderById(reminderId)
                        if (reminder == null || !reminder.isActiveForAlarm) {
                            app.scheduler.cancel(reminderId)
                            ReminderNotificationHelper.cancelNotification(context, reminderId)
                            return@launch
                        }
                        val advanceMinutes = intent.getIntExtra(EXTRA_ADVANCE_MINUTES, reminder.advanceMinutes)
                        ReminderNotificationHelper.showAdvanceNotification(context, reminder, advanceMinutes)
                        AdvanceAlertBus.emit(
                            AdvanceVoiceAlertEvent(
                                reminderId = reminder.id,
                                title = reminder.title,
                                advanceMinutes = advanceMinutes,
                                isAssignment = reminder.isAssignment
                            )
                        )
                    }

                    ACTION_TRIGGER_REMINDER -> {
                        val reminder = repository.getReminderById(reminderId)
                        if (reminder == null || !reminder.isActiveForAlarm) {
                            // Completed/cancelled tasks must never ring
                            app.scheduler.cancel(reminderId)
                            ReminderNotificationHelper.cancelNotification(context, reminderId)
                            return@launch
                        }
                        repository.markRinging(reminderId)
                        ReminderNotificationHelper.cancelNotification(context, reminderId)
                        ReminderNotificationHelper.showAlarmNotification(context, reminder)
                        val alertIntent = Intent(context, ReminderAlertActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra(ReminderAlertActivity.EXTRA_REMINDER_ID, reminderId)
                        }
                        runCatching { context.startActivity(alertIntent) }
                    }

                    ACTION_COMPLETE -> {
                        ReminderNotificationHelper.cancelNotification(context, reminderId)
                        repository.markComplete(reminderId)
                    }

                    ACTION_CANCEL -> {
                        ReminderNotificationHelper.cancelNotification(context, reminderId)
                        repository.cancelReminder(reminderId)
                    }

                    ACTION_DISMISS -> {
                        ReminderNotificationHelper.cancelNotification(context, reminderId)
                        repository.dismissReminder(reminderId)
                    }

                    ACTION_SNOOZE_10M -> {
                        ReminderNotificationHelper.cancelNotification(context, reminderId)
                        repository.snoozeByMinutes(reminderId, 10)
                    }
                }
            } finally {
                if (wakeLock.isHeld) {
                    runCatching { wakeLock.release() }
                }
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_TRIGGER_REMINDER = "com.example.action.TRIGGER_REMINDER"
        const val ACTION_ADVANCE_REMINDER = "com.example.action.ADVANCE_REMINDER"
        const val ACTION_COMPLETE = "com.example.action.COMPLETE_REMINDER"
        const val ACTION_CANCEL = "com.example.action.CANCEL_REMINDER"
        const val ACTION_DISMISS = "com.example.action.DISMISS_REMINDER"
        const val ACTION_SNOOZE_10M = "com.example.action.SNOOZE_10M"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_ADVANCE_MINUTES = "extra_advance_minutes"
    }
}
