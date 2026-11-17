package com.example

import com.example.data.ReminderCategory
import com.example.data.ReminderEntity
import com.example.data.ReminderStatus
import com.example.domain.DailyScheduleCalculator
import com.example.domain.ReminderAlarmScheduler
import com.example.domain.TodayTaskDisplayStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class DailyScheduleCalculatorTest {

    @Test
    fun `calculates chronological Task 1 Task 2 numbering and daily progress in local timezone`() {
        val tz = TimeZone.getTimeZone("Asia/Kolkata")
        val dayCal = Calendar.getInstance(tz, Locale.US).apply {
            set(2026, Calendar.NOVEMBER, 17, 6, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nowMillis = dayCal.timeInMillis

        fun atTime(dayOffset: Int, hour: Int, minute: Int): Long {
            return (dayCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
            }.timeInMillis
        }

        val yesterdayCompleted = ReminderEntity(
            id = 1,
            title = "Yesterday history item",
            originalInput = "Yesterday",
            scheduledAtMillis = atTime(-1, 18, 0),
            status = ReminderStatus.COMPLETED
        )
        val task7am = ReminderEntity(
            id = 2,
            title = "Physics Assignment",
            originalInput = "7 AM",
            category = ReminderCategory.ASSIGNMENT,
            scheduledAtMillis = atTime(0, 7, 0),
            assignmentDeadlineMillis = atTime(0, 7, 0),
            isAssignment = true,
            status = ReminderStatus.UPCOMING
        )
        val task6am = ReminderEntity(
            id = 3,
            title = "Mathematics Assignment",
            originalInput = "6 AM",
            category = ReminderCategory.ASSIGNMENT,
            scheduledAtMillis = atTime(0, 6, 0),
            assignmentDeadlineMillis = atTime(0, 6, 0),
            isAssignment = true,
            status = ReminderStatus.COMPLETED
        )
        val task615amOverdue = ReminderEntity(
            id = 4,
            title = "Drink water",
            originalInput = "6:15 AM",
            category = ReminderCategory.HEALTH,
            scheduledAtMillis = atTime(0, 6, 15),
            status = ReminderStatus.UPCOMING
        )
        val tomorrowTask = ReminderEntity(
            id = 5,
            title = "Tomorrow task",
            originalInput = "Tomorrow 8 AM",
            scheduledAtMillis = atTime(1, 8, 0),
            status = ReminderStatus.UPCOMING
        )

        val all = listOf(yesterdayCompleted, task7am, task6am, task615amOverdue, tomorrowTask)
        val summaryToday = DailyScheduleCalculator.calculate(all, nowMillis, tz, Locale.US)

        // Excludes yesterday and tomorrow; keeps only today's 3 tasks in chronological order
        assertEquals(3, summaryToday.tasks.size)
        assertEquals(1, summaryToday.tasks[0].taskNumber)
        assertEquals("Mathematics Assignment", summaryToday.tasks[0].reminder.title)
        assertEquals(TodayTaskDisplayStatus.COMPLETED, summaryToday.tasks[0].displayStatus)

        assertEquals(2, summaryToday.tasks[1].taskNumber)
        assertEquals("Drink water", summaryToday.tasks[1].reminder.title)
        assertEquals(TodayTaskDisplayStatus.OVERDUE, summaryToday.tasks[1].displayStatus)

        assertEquals(3, summaryToday.tasks[2].taskNumber)
        assertEquals("Physics Assignment", summaryToday.tasks[2].reminder.title)
        assertEquals(TodayTaskDisplayStatus.UPCOMING, summaryToday.tasks[2].displayStatus)

        assertEquals(1, summaryToday.completedCount)
        assertEquals(3, summaryToday.totalCount)
        assertEquals(33, summaryToday.progressPercent)
        assertEquals("2026-11-17", summaryToday.dayKey)

        // Midnight rollover: advancing to next local calendar day resets daily count automatically
        val afterMidnightMillis = atTime(1, 0, 5)
        val summaryTomorrow = DailyScheduleCalculator.calculate(all, afterMidnightMillis, tz, Locale.US)
        assertEquals("2026-11-18", summaryTomorrow.dayKey)
        assertEquals(1, summaryTomorrow.tasks.size)
        assertEquals("Tomorrow task", summaryTomorrow.tasks[0].reminder.title)
        assertEquals(0, summaryTomorrow.completedCount)
        assertEquals(1, summaryTomorrow.totalCount)
        assertEquals(0, summaryTomorrow.progressPercent)
    }

    @Test
    fun `advance reminder planner skips past advance interval without duplicating due alarm`() {
        val now = 1_700_000_000_000L

        // Deadline 60 minutes away with 45 min advance -> schedules BOTH advance (t+15m) and due (t+60m)
        val fullPlan = ReminderAlarmScheduler.computeAlarmTimes(
            dueAtMillis = now + 60 * 60_000L,
            advanceMinutes = 45,
            nowMillis = now
        )
        assertNotNull(fullPlan.dueAtMillis)
        assertNotNull(fullPlan.advanceAtMillis)
        assertEquals(now + 15 * 60_000L, fullPlan.advanceAtMillis)

        // Deadline only 20 minutes away with 45 min advance -> advance is in past, schedules ONLY due alarm
        val shortPlan = ReminderAlarmScheduler.computeAlarmTimes(
            dueAtMillis = now + 20 * 60_000L,
            advanceMinutes = 45,
            nowMillis = now
        )
        assertNotNull(shortPlan.dueAtMillis)
        assertNull(shortPlan.advanceAtMillis)
    }
}
