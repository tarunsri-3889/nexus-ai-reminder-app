package com.example.data

enum class ReminderStatus {
    UPCOMING,
    RINGING,
    SNOOZED,
    OVERDUE,
    COMPLETED,
    CANCELLED,
    DISMISSED
}

enum class ReminderCategory(val displayName: String, val hindiLabel: String) {
    ASSIGNMENT("Assignment", "असाइनमेंट"),
    STUDY("Study", "पढ़ाई"),
    WORK("Work", "काम"),
    HEALTH("Health", "सेहत"),
    PERSONAL("Personal", "निजी")
}
