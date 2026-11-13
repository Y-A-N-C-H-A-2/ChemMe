/**
 * Checks a DailyLog before it is saved. Contains no Android code, so it can be unit tested on a PC.
 */
package com.example.chemme.logic

import com.example.chemme.data.DailyLog

/**
 * The outcome of a save attempt.
 * errors maps a field name (see LogValidator.FIELD_...) to the message shown under that field.
 * savedLogId is filled in by LogViewModel only when the entry was valid and stored.
 */
data class ValidationResult(
    val errors: Map<String, String>,
    val savedLogId: Long? = null
) {
    val isValid: Boolean get() = errors.isEmpty()
}

/** The same limits as the constraints written on the ERD. */
object LogValidator {
    const val FIELD_MOOD = "mood"
    const val FIELD_ENERGY = "energy"
    const val FIELD_ANXIETY = "anxiety"
    const val FIELD_SLEEP = "sleepHours"
    const val FIELD_CAFFEINE = "caffeineCups"
    const val FIELD_EXERCISE = "exerciseMinutes"
    const val FIELD_OUTDOOR = "outdoorMinutes"
    const val FIELD_MEALS = "mealsEaten"
    const val FIELD_NOTE = "note"

    /** The form uses -1 for a box that was left empty or did not contain a number. */
    const val MISSING = -1
    const val MAX_NOTE_LENGTH = 300

    fun validate(log: DailyLog): ValidationResult {
        val errors = mutableMapOf<String, String>()

        if (log.mood !in 1..5) errors[FIELD_MOOD] = "Choose 1 to 5"
        if (log.energy !in 1..5) errors[FIELD_ENERGY] = "Choose 1 to 5"
        if (log.anxiety !in 1..5) errors[FIELD_ANXIETY] = "Choose 1 to 5"
        if (log.sleepHours < 0.0 || log.sleepHours > 24.0) errors[FIELD_SLEEP] = "Enter 0 to 24 hours"
        if (log.caffeineCups !in 0..20) errors[FIELD_CAFFEINE] = "Enter 0 to 20 cups"
        if (log.exerciseMinutes !in 0..1440) errors[FIELD_EXERCISE] = "Enter 0 to 1440 minutes"
        if (log.outdoorMinutes !in 0..1440) errors[FIELD_OUTDOOR] = "Enter 0 to 1440 minutes"
        if (log.mealsEaten !in 0..10) errors[FIELD_MEALS] = "Enter 0 to 10 meals"
        if ((log.note?.length ?: 0) > MAX_NOTE_LENGTH) errors[FIELD_NOTE] = "Keep the note under 300 characters"

        return ValidationResult(errors)
    }
}
