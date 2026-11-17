package com.example

import com.example.data.ReminderCategory
import com.example.domain.FollowUpCommand
import com.example.domain.NaturalLanguageReminderParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class NaturalLanguageReminderParserTest {

    @Test
    fun `parses classic 6 AM mathematics assignment prompt`() {
        val baseCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 17, 1, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val parsed = NaturalLanguageReminderParser.parse(
            rawInput = "Remind me at 6 AM to finish my mathematics assignment",
            nowMillis = baseCal.timeInMillis
        )

        assertEquals("Finish my mathematics assignment", parsed.title)
        assertEquals(ReminderCategory.ASSIGNMENT, parsed.category)
        assertTrue(parsed.isAssignment)
        assertTrue(parsed.timeDetected)

        val scheduledCal = Calendar.getInstance().apply { timeInMillis = parsed.scheduledAtMillis }
        assertEquals(6, scheduledCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, scheduledCal.get(Calendar.MINUTE))
        assertEquals(17, scheduledCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `rolls over passed morning time to tomorrow`() {
        val baseCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 17, 10, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val parsed = NaturalLanguageReminderParser.parse(
            rawInput = "Remind me at 6 AM to finish my mathematics assignment",
            nowMillis = baseCal.timeInMillis
        )

        val scheduledCal = Calendar.getInstance().apply { timeInMillis = parsed.scheduledAtMillis }
        assertEquals(6, scheduledCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(18, scheduledCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `parses relative minutes reminder`() {
        val baseMillis = 1_700_000_000_000L
        val parsed = NaturalLanguageReminderParser.parse(
            rawInput = "In 15 minutes revise physics notes",
            nowMillis = baseMillis
        )

        assertTrue(parsed.timeDetected)
        assertEquals(baseMillis + 15 * 60_000L, parsed.scheduledAtMillis)
        assertEquals(ReminderCategory.STUDY, parsed.category)
    }

    @Test
    fun `parses Hindi Devanagari assignment prompt`() {
        val baseCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 17, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val parsed = NaturalLanguageReminderParser.parse(
            rawInput = "कल सुबह 6 बजे गणित असाइनमेंट पूरा करें",
            nowMillis = baseCal.timeInMillis
        )

        assertTrue(parsed.isAssignment)
        assertEquals(ReminderCategory.ASSIGNMENT, parsed.category)
        assertTrue(parsed.timeDetected)

        val scheduledCal = Calendar.getInstance().apply { timeInMillis = parsed.scheduledAtMillis }
        assertEquals(6, scheduledCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(18, scheduledCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `parses voice follow-up intents accurately`() {
        assertEquals(
            FollowUpCommand.Complete,
            NaturalLanguageReminderParser.parseFollowUpIntent("Mark it complete please")
        )
        assertEquals(
            FollowUpCommand.SnoozeMinutes(15),
            NaturalLanguageReminderParser.parseFollowUpIntent("Snooze for 15 minutes")
        )
        assertEquals(
            FollowUpCommand.TomorrowMorning,
            NaturalLanguageReminderParser.parseFollowUpIntent("Reschedule to tomorrow")
        )
        assertEquals(
            FollowUpCommand.Dismiss,
            NaturalLanguageReminderParser.parseFollowUpIntent("Dismiss alarm")
        )
    }
}
