package com.example.data

import androidx.room.TypeConverter

class NexusConverters {
    @TypeConverter
    fun fromStatus(status: ReminderStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): ReminderStatus =
        runCatching { ReminderStatus.valueOf(value) }.getOrDefault(ReminderStatus.UPCOMING)

    @TypeConverter
    fun fromCategory(category: ReminderCategory): String = category.name

    @TypeConverter
    fun toCategory(value: String): ReminderCategory =
        runCatching { ReminderCategory.valueOf(value) }.getOrDefault(ReminderCategory.PERSONAL)
}
