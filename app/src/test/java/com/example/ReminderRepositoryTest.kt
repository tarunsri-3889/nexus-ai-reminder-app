package com.example

import android.app.AlarmManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.NexusDatabase
import com.example.data.ReminderCategory
import com.example.data.ReminderEntity
import com.example.data.ReminderRepository
import com.example.data.ReminderStatus
import com.example.domain.AndroidReminderScheduler
import com.example.domain.NaturalLanguageReminderParser
import com.example.domain.ReminderAlarmScheduler
import com.example.domain.ScheduleResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReminderRepositoryTest {

    private lateinit var context: Context
    private lateinit var database: NexusDatabase
    private lateinit var fakeScheduler: FakeAlarmScheduler
    private lateinit var repository: ReminderRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, NexusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        fakeScheduler = FakeAlarmScheduler()
        repository = ReminderRepository(database.reminderDao(), fakeScheduler)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `create snooze complete and cancel reminder lifecycle cancels alarms`() = runTest {
        val now = System.currentTimeMillis()
        val parsed = NaturalLanguageReminderParser.parse(
            "Remind me at 6 AM to finish my mathematics assignment",
            nowMillis = now
        )
        val created = repository.createReminder(
            parsed = parsed,
            overrideScheduledAtMillis = now + 90 * 60_000L,
            nowMillis = now
        )
        val id = created.reminder.id

        // Assignment defaults to 45m advance + due alarm
        assertEquals(45, created.reminder.advanceMinutes)
        assertNotNull(created.reminder.assignmentDeadlineMillis)
        assertNotNull(created.scheduleResult.advanceScheduledAtMillis)
        assertEquals(1, fakeScheduler.scheduledIds.size)

        repository.snoozeByMinutes(id, 10)
        val snoozed = repository.getReminderById(id)!!
        assertEquals(ReminderStatus.SNOOZED, snoozed.status)
        assertEquals(1, snoozed.snoozeCount)

        repository.markComplete(id)
        val completed = repository.getReminderById(id)!!
        assertEquals(ReminderStatus.COMPLETED, completed.status)
        assertTrue(fakeScheduler.cancelledIds.contains(id))

        // Create a second ordinary task (defaults to 20m advance) and cancel it
        val taskParsed = NaturalLanguageReminderParser.parse("Revise chapter 3", now)
        val createdTask = repository.createReminder(
            parsed = taskParsed,
            overrideCategory = ReminderCategory.STUDY,
            overrideScheduledAtMillis = now + 45 * 60_000L,
            nowMillis = now
        )
        assertEquals(20, createdTask.reminder.advanceMinutes)
        repository.cancelReminder(createdTask.reminder.id)
        val cancelled = repository.getReminderById(createdTask.reminder.id)!!
        assertEquals(ReminderStatus.CANCELLED, cancelled.status)
        assertTrue(fakeScheduler.cancelledIds.contains(createdTask.reminder.id))

        repository.deleteReminder(id)
        repository.deleteReminder(createdTask.reminder.id)
        assertTrue(repository.allReminders.first().isEmpty())
    }

    @Test
    fun `android scheduler registers both advance and due alarms and cancels both on completion`() = runTest {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = shadowOf(alarmManager)
        val realScheduler = AndroidReminderScheduler(context)
        val repoWithRealScheduler = ReminderRepository(database.reminderDao(), realScheduler)

        val now = System.currentTimeMillis()
        val parsed = NaturalLanguageReminderParser.parse("Finish assignment", now)
        val created = repoWithRealScheduler.createReminder(
            parsed = parsed,
            overrideCategory = ReminderCategory.ASSIGNMENT,
            overrideAdvanceMinutes = 45,
            assignmentDeadlineMillis = now + 90 * 60_000L,
            nowMillis = now
        )

        // Both advance (t+45m) and due (t+90m) alarms scheduled
        assertEquals(2, shadowAlarmManager.scheduledAlarms.size)

        // Completing immediately cancels BOTH alarms
        repoWithRealScheduler.markComplete(created.reminder.id)
        assertEquals(0, shadowAlarmManager.scheduledAlarms.size)

        // If deadline is 15m away (< 30m advance), only 1 due alarm is scheduled
        val shortParsed = NaturalLanguageReminderParser.parse("Quick assignment", now)
        val shortCreated = repoWithRealScheduler.createReminder(
            parsed = shortParsed,
            overrideCategory = ReminderCategory.ASSIGNMENT,
            overrideAdvanceMinutes = 30,
            assignmentDeadlineMillis = now + 15 * 60_000L,
            nowMillis = now
        )
        assertNull(shortCreated.scheduleResult.advanceScheduledAtMillis)
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)

        // Cancelling removes the remaining alarm
        repoWithRealScheduler.cancelReminder(shortCreated.reminder.id)
        assertEquals(0, shadowAlarmManager.scheduledAlarms.size)
    }

    private class FakeAlarmScheduler : ReminderAlarmScheduler {
        val scheduledIds = mutableListOf<Long>()
        val cancelledIds = mutableListOf<Long>()

        override fun schedule(reminder: ReminderEntity, nowMillis: Long): ScheduleResult {
            scheduledIds.add(reminder.id)
            val plan = ReminderAlarmScheduler.computeAlarmTimes(
                dueAtMillis = reminder.effectiveDueMillis,
                advanceMinutes = reminder.advanceMinutes,
                nowMillis = nowMillis
            )
            return ScheduleResult(
                scheduledAtMillis = plan.dueAtMillis ?: (nowMillis + 1_000L),
                advanceScheduledAtMillis = plan.advanceAtMillis,
                exactAlarmUsed = true
            )
        }

        override fun cancel(reminderId: Long) {
            cancelledIds.add(reminderId)
        }
    }
}
