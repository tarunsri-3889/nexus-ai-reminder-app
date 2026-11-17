package com.example.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.ReminderAlertActivity
import com.example.data.ReminderEntity
import com.example.domain.NaturalLanguageReminderParser
import com.example.domain.PermissionManagerHelper

object ReminderNotificationHelper {

    const val ALARM_CHANNEL_ID = "nexus_reminder_alarm_v1"
    const val ADVANCE_CHANNEL_ID = "nexus_advance_reminder_v1"
    private const val ALARM_NOTIFICATION_ID_BASE = 4000
    private const val ADVANCE_NOTIFICATION_ID_BASE = 14000

    fun alarmNotificationIdFor(reminderId: Long): Int = ALARM_NOTIFICATION_ID_BASE + reminderId.toInt()
    fun advanceNotificationIdFor(reminderId: Long): Int = ADVANCE_NOTIFICATION_ID_BASE + reminderId.toInt()

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (manager.getNotificationChannel(ALARM_CHANNEL_ID) == null) {
                val alarmChannel = NotificationChannel(
                    ALARM_CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.notification_channel_desc)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    enableVibration(true)
                    setSound(null, null)
                    setBypassDnd(false)
                }
                manager.createNotificationChannel(alarmChannel)
            }

            if (manager.getNotificationChannel(ADVANCE_CHANNEL_ID) == null) {
                val advanceChannel = NotificationChannel(
                    ADVANCE_CHANNEL_ID,
                    "NEXUS Advance Heads-Up Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Gentle advance reminder notifications before a task or assignment deadline."
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    enableVibration(true)
                }
                manager.createNotificationChannel(advanceChannel)
            }
        }
    }

    fun showAdvanceNotification(context: Context, reminder: ReminderEntity, advanceMinutes: Int) {
        ensureChannel(context)
        if (!reminder.isActiveForAlarm) return
        if (!PermissionManagerHelper.hasNotificationPermission(context)) return

        val notificationId = advanceNotificationIdFor(reminder.id)
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_COMPLETE
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminder.id)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 50_000,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_CANCEL
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminder.id)
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 60_000,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dueLabel = NaturalLanguageReminderParser.formatDateTimeShort(reminder.effectiveDueMillis)
        val prefix = if (reminder.isAssignment) {
            "Assignment deadline in ${advanceMinutes}m"
        } else {
            "Upcoming task in ${advanceMinutes}m"
        }

        val builder = NotificationCompat.Builder(context, ADVANCE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(prefix)
            .setContentText(reminder.title)
            .setSubText("Due $dueLabel • Local reminder")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(0, context.getString(R.string.action_mark_complete), completePendingIntent)
            .addAction(0, "Cancel Task", cancelPendingIntent)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, builder.build())
    }

    fun showAlarmNotification(context: Context, reminder: ReminderEntity) {
        ensureChannel(context)
        if (!reminder.isActiveForAlarm) return
        if (!PermissionManagerHelper.hasNotificationPermission(context)) return

        val notificationId = alarmNotificationIdFor(reminder.id)
        val fullScreenIntent = Intent(context, ReminderAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ReminderAlertActivity.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderAlertActivity.EXTRA_AUTO_ANSWER, false)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val answerIntent = Intent(context, ReminderAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ReminderAlertActivity.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderAlertActivity.EXTRA_AUTO_ANSWER, true)
        }
        val answerPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 10_000,
            answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_COMPLETE
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminder.id)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 20_000,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_SNOOZE_10M
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminder.id)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 30_000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeSummary = NaturalLanguageReminderParser.formatDateTimeShort(reminder.effectiveDueMillis)
        val builder = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("NEXUS AI Local App Alarm")
            .setContentText(reminder.title)
            .setSubText("$timeSummary • ${reminder.category.displayName}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${reminder.title}\n\nDue: $timeSummary • Local app alarm (not a cellular call)")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(0, context.getString(R.string.action_talk_to_nexus), answerPendingIntent)
            .addAction(0, context.getString(R.string.action_snooze_10m), snoozePendingIntent)
            .addAction(0, context.getString(R.string.action_mark_complete), completePendingIntent)

        if (PermissionManagerHelper.canUseFullScreenIntent(context)) {
            builder.setFullScreenIntent(fullScreenPendingIntent, true)
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, builder.build())
    }

    fun cancelNotification(context: Context, reminderId: Long) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(alarmNotificationIdFor(reminderId))
        manager.cancel(advanceNotificationIdFor(reminderId))
    }
}
