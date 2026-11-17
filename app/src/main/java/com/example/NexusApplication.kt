package com.example

import android.app.Application
import com.example.data.NexusDatabase
import com.example.data.ReminderRepository
import com.example.domain.AndroidReminderScheduler
import com.example.platform.ReminderNotificationHelper

class NexusApplication : Application() {

    val database: NexusDatabase by lazy { NexusDatabase.getInstance(this) }
    val scheduler: AndroidReminderScheduler by lazy { AndroidReminderScheduler(this) }
    val repository: ReminderRepository by lazy {
        ReminderRepository(database.reminderDao(), scheduler)
    }

    override fun onCreate() {
        super.onCreate()
        ReminderNotificationHelper.ensureChannel(this)
    }
}
