package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val originalInput: String,
    val category: ReminderCategory = ReminderCategory.PERSONAL,
    val scheduledAtMillis: Long,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val status: ReminderStatus = ReminderStatus.UPCOMING,
    val snoozeCount: Int = 0,
    val isAssignment: Boolean = false,
    val notes: String = "",
    val completedAtMillis: Long? = null,
    val assignmentDeadlineMillis: Long? = null,
    val advanceMinutes: Int = 20
) {
    val effectiveDueMillis: Long
        get() = if (isAssignment && assignmentDeadlineMillis != null) {
            assignmentDeadlineMillis
        } else {
            scheduledAtMillis
        }

    val isPending: Boolean
        get() = status == ReminderStatus.UPCOMING ||
            status == ReminderStatus.SNOOZED ||
            status == ReminderStatus.RINGING ||
            status == ReminderStatus.OVERDUE

    val isActiveForAlarm: Boolean
        get() = status != ReminderStatus.COMPLETED &&
            status != ReminderStatus.CANCELLED &&
            status != ReminderStatus.DISMISSED
}
