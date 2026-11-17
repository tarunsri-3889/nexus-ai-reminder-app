package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY scheduledAtMillis ASC")
    fun observeAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE status IN ('UPCOMING', 'SNOOZED', 'RINGING', 'OVERDUE') ORDER BY scheduledAtMillis ASC")
    fun observePendingReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE status IN ('UPCOMING', 'SNOOZED', 'OVERDUE') ORDER BY scheduledAtMillis ASC")
    suspend fun getSchedulableRemindersOnce(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderById(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    fun observeReminderById(id: Long): Flow<ReminderEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)

    @Query("SELECT COUNT(*) FROM reminders")
    suspend fun countReminders(): Int
}
