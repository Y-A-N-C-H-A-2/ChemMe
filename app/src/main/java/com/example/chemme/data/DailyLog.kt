/**
 * Room entity for one logged day. This is the table the user creates, edits and deletes.
 */
package com.example.chemme.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per calendar day. The UNIQUE index on logDate stops two entries for the same date,
 * so logging a day twice edits the existing row instead of adding a duplicate.
 */
@Entity(
    tableName = "daily_log",
    indices = [Index(value = ["logDate"], unique = true)]
)
data class DailyLog(
    @PrimaryKey(autoGenerate = true) val logId: Long = 0,
    /** ISO date text such as 2026-11-13, so sorting as text also sorts by date. */
    val logDate: String,
    val mood: Int,              // 1 to 5
    val energy: Int,            // 1 to 5
    val anxiety: Int,           // 1 to 5
    val sleepHours: Double,     // 0 to 24
    val caffeineCups: Int,      // 0 to 20
    val exerciseMinutes: Int,   // 0 to 1440
    val outdoorMinutes: Int,    // 0 to 1440
    val mealsEaten: Int,        // 0 to 10
    /** Comma-separated names such as "Vitamin D, Iron"; null when none were taken. */
    val supplements: String? = null,
    /** Hours between sunrise and sunset at the user's location; null when location is unavailable. */
    val daylightHours: Double? = null,
    val note: String? = null,   // max 300 characters
    val createdAt: Long = System.currentTimeMillis()
)
