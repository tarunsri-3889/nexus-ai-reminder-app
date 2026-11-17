package com.example.domain

import com.example.data.ReminderCategory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ParsedReminder(
    val title: String,
    val originalInput: String,
    val scheduledAtMillis: Long,
    val category: ReminderCategory,
    val isAssignment: Boolean,
    val timeDetected: Boolean,
    val parsedSummary: String,
    val suggestedAdvanceMinutes: Int = if (isAssignment) 45 else 20
)

sealed class FollowUpCommand {
    data object Complete : FollowUpCommand()
    data class SnoozeMinutes(val minutes: Int) : FollowUpCommand()
    data object TomorrowMorning : FollowUpCommand()
    data object Dismiss : FollowUpCommand()
    data object Repeat : FollowUpCommand()
    data class Unknown(val transcript: String) : FollowUpCommand()
}

object NaturalLanguageReminderParser {

    private val assignmentKeywords = listOf(
        "assignment", "homework", "project", "lab", "practical", "worksheet",
        "essay", "submission", "submit", "exam", "test", "quiz", "paper",
        "thesis", "report", "असाइनमेंट", "होमवर्क", "परीक्षा", "प्रोजेक्ट", "कॉपी"
    )

    private val studyKeywords = listOf(
        "study", "revise", "revision", "read", "chapter", "lecture", "notes",
        "class", "practice", "solve", "पढ़ाई", "रिविजन", "अध्याय", "नोट्स"
    )

    private val workKeywords = listOf(
        "meeting", "call", "email", "client", "standup", "deploy", "presentation",
        "office", "deadline", "मीटिंग", "ऑफिस", "काम"
    )

    private val healthKeywords = listOf(
        "medicine", "pill", "water", "workout", "gym", "walk", "run", "doctor",
        "sleep", "meditate", "दवा", "पानी", "व्यायाम", "डॉक्टर"
    )

    fun parse(
        rawInput: String,
        nowMillis: Long = System.currentTimeMillis()
    ): ParsedReminder {
        val trimmed = rawInput.trim()
        val normalized = normalizeDevanagariDigits(trimmed)
        val lower = normalized.lowercase(Locale.getDefault())

        val calendar = Calendar.getInstance().apply {
            timeInMillis = nowMillis
        }

        var timeDetected = false

        // 1. Check relative time ("in 10 minutes", "after 2 hours", "5 minute baad", "10 मिनट में")
        val relativeRegex = Regex(
            """(?:in|after|बाद|में)?\s*(\d{1,3})\s*(minutes?|mins?|min|m|hours?|hrs?|hr|h|मिनट|घंटे|घंटा)\s*(?:baad|bad|mein|me|में|बाद)?"""
        )
        val isExplicitRelative = Regex("""\b(in|after)\s+\d+|(\d+\s*(minutes?|mins?|hours?|hrs?|मिनट|घंटे)\s*(baad|bad|mein|me|में|बाद)?)""").containsMatchIn(lower)
        val relMatch = if (isExplicitRelative) relativeRegex.find(lower) else null

        if (relMatch != null) {
            val amount = relMatch.groupValues[1].toIntOrNull() ?: 10
            val unit = relMatch.groupValues[2]
            val minutesToAdd = when {
                unit.startsWith("h") || unit.startsWith("घंट") -> amount * 60
                else -> amount
            }.coerceAtLeast(1)
            calendar.add(Calendar.MINUTE, minutesToAdd)
            timeDetected = true
        } else {
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            // 2. Check day offset (tomorrow / kal / today / aaj)
            val isTomorrow = Regex("""\b(tomorrow|tmrw|tmr|kal)\b|कल""").containsMatchIn(lower)
            val isToday = Regex("""\b(today|tonight|aaj)\b|आज""").containsMatchIn(lower)

            // 3. Check Hindi time-of-day cues (subah/सुबह, shaam/शाम, raat/रात, dopahar/दोपहर)
            val hasMorningCue = Regex("""\b(subah|morning)\b|सुबह""").containsMatchIn(lower)
            val hasEveningCue = Regex("""\b(shaam|sham|evening|raat|night)\b|शाम|रात""").containsMatchIn(lower)
            val hasAfternoonCue = Regex("""\b(dopahar|afternoon)\b|दोपहर""").containsMatchIn(lower)

            // 4. Match explicit clock time: "at 6 AM", "6:30 pm", "18:00", "6 baje", "6 बजे"
            val clockRegex = Regex(
                """(?:at\s+|@)?\b(\d{1,2})(?::(\d{2}))?\s*(a\.?m\.?|p\.?m\.?|baje|बजे)?\b"""
            )
            val clockMatch = clockRegex.findAll(lower).firstOrNull { match ->
                val hour = match.groupValues[1].toIntOrNull() ?: return@firstOrNull false
                val hasColon = match.groupValues[2].isNotEmpty()
                val meridiem = match.groupValues[3]
                val hasAtPrefix = match.value.trim().startsWith("at") || match.value.trim().startsWith("@")
                hour in 0..23 && (hasColon || meridiem.isNotEmpty() || hasAtPrefix)
            }

            if (clockMatch != null) {
                var hour = clockMatch.groupValues[1].toInt()
                val minute = clockMatch.groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: 0
                val meridiem = clockMatch.groupValues[3].replace(".", "")

                when {
                    meridiem == "pm" && hour in 1..11 -> hour += 12
                    meridiem == "am" && hour == 12 -> hour = 0
                    meridiem.isEmpty() || meridiem == "baje" || meridiem == "बजे" -> {
                        when {
                            hasEveningCue && hour in 1..11 -> hour += 12
                            hasAfternoonCue && hour in 1..6 -> hour += 12
                            hasMorningCue && hour == 12 -> hour = 0
                        }
                    }
                }

                if (hour in 0..23) {
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)
                    if (isTomorrow) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    } else if (!isToday && calendar.timeInMillis <= nowMillis) {
                        // Roll over to next day if specified time already passed today
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    timeDetected = true
                }
            } else if (isTomorrow) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, if (hasEveningCue) 19 else 9)
                calendar.set(Calendar.MINUTE, 0)
                timeDetected = true
            } else {
                // Default fallback: 10 minutes from now so immediate testing works
                calendar.add(Calendar.MINUTE, 10)
            }
        }

        val category = detectCategory(lower)
        val isAssignment = category == ReminderCategory.ASSIGNMENT ||
            assignmentKeywords.any { lower.contains(it) }

        val cleanTitle = extractCleanTitle(trimmed, isAssignment)
        val formattedTime = formatDateTimeShort(calendar.timeInMillis)
        val summary = buildString {
            append(formattedTime)
            if (!timeDetected) append(" (default)")
            append(" • ")
            append(category.displayName)
        }

        return ParsedReminder(
            title = cleanTitle,
            originalInput = trimmed,
            scheduledAtMillis = calendar.timeInMillis,
            category = category,
            isAssignment = isAssignment,
            timeDetected = timeDetected,
            parsedSummary = summary
        )
    }

    fun parseFollowUpIntent(spokenText: String): FollowUpCommand {
        val lower = normalizeDevanagariDigits(spokenText.trim()).lowercase(Locale.getDefault())
        return when {
            lower.contains("complete") || lower.contains("done") || lower.contains("finish") ||
                lower.contains("ho gaya") || lower.contains("पूरा") || lower.contains("हो गया") ->
                FollowUpCommand.Complete

            lower.contains("tomorrow") || lower.contains("kal") || lower.contains("कल") ->
                FollowUpCommand.TomorrowMorning

            lower.contains("snooze") || lower.contains("later") || lower.contains("minute") ||
                lower.contains("hour") || lower.contains("baad") || lower.contains("मिनट") ||
                lower.contains("बाद") -> {
                val mins = Regex("""(\d{1,3})""").find(lower)?.groupValues?.get(1)?.toIntOrNull()
                val resolvedMins = when {
                    mins != null && (lower.contains("hour") || lower.contains("hr") || lower.contains("घंट")) ->
                        mins * 60
                    mins != null -> mins
                    lower.contains("hour") || lower.contains("घंट") -> 60
                    else -> 10
                }
                FollowUpCommand.SnoozeMinutes(resolvedMins.coerceIn(1, 720))
            }

            lower.contains("repeat") || lower.contains("again") || lower.contains("dobara") ||
                lower.contains("दोबारा") ->
                FollowUpCommand.Repeat

            lower.contains("dismiss") || lower.contains("stop") || lower.contains("cancel") ||
                lower.contains("band") || lower.contains("बंद") ->
                FollowUpCommand.Dismiss

            else -> FollowUpCommand.Unknown(spokenText)
        }
    }

    private fun detectCategory(lower: String): ReminderCategory {
        return when {
            assignmentKeywords.any { lower.contains(it) } -> ReminderCategory.ASSIGNMENT
            studyKeywords.any { lower.contains(it) } -> ReminderCategory.STUDY
            workKeywords.any { lower.contains(it) } -> ReminderCategory.WORK
            healthKeywords.any { lower.contains(it) } -> ReminderCategory.HEALTH
            else -> ReminderCategory.PERSONAL
        }
    }

    private fun extractCleanTitle(rawInput: String, isAssignment: Boolean): String {
        if (rawInput.isBlank()) return "Personal Reminder"

        var cleaned = rawInput.trim()

        // Strip common leading command phrases while preserving Hindi/English content
        val leadingPatterns = listOf(
            Regex("""^(?i)(hey\s+nexus[,\s]+|nexus[,\s]+)?(please\s+)?remind\s+me\s+"""),
            Regex("""^(?i)(today|tomorrow|tmrw|tonight|aaj|kal)\s+"""),
            Regex("""^(?i)(in|after)\s+\d{1,3}\s*(minutes?|mins?|min|hours?|hrs?|hr)\s+(to\s+|about\s+)?"""),
            Regex("""^(?i)(at\s+|@)?\d{1,2}(:\d{2})?\s*(a\.?m\.?|p\.?m\.?)?\s+(today|tomorrow|tmrw)?\s*(to\s+|about\s+)?"""),
            Regex("""^(?i)(to\s+|about\s+)""")
        )

        repeat(3) {
            for (pattern in leadingPatterns) {
                cleaned = pattern.replace(cleaned, "").trim()
            }
        }

        // Also strip trailing time phrases like "at 6 AM" or "tomorrow at 8 PM" if title remains non-empty
        val trailingTime = Regex("""(?i)\s+(today|tomorrow|tmrw)?\s*(at\s+|@)\d{1,2}(:\d{2})?\s*(a\.?m\.?|p\.?m\.?)?\s*$""")
        val withoutTrailing = trailingTime.replace(cleaned, "").trim()
        if (withoutTrailing.length >= 3) {
            cleaned = withoutTrailing
        }

        val finalTitle = if (cleaned.length >= 2) cleaned else rawInput.trim()
        val capitalized = finalTitle.replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
        }
        return if (capitalized.isBlank() && isAssignment) "Complete Assignment" else capitalized
    }

    private fun normalizeDevanagariDigits(input: String): String {
        val devanagariZero = '०'
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '०'..'९') {
                sb.append((ch - devanagariZero + '0'.code).toChar())
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatDateTimeShort(millis: Long): String {
        val target = Calendar.getInstance().apply { timeInMillis = millis }
        val today = Calendar.getInstance()
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }

        val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault()).format(target.time)
        return when {
            isSameDay(target, today) -> "Today, $timeFmt"
            isSameDay(target, tomorrow) -> "Tomorrow, $timeFmt"
            else -> SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault()).format(target.time)
        }
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }
}
