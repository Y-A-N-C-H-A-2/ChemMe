/**
 * Small pure rules used by the log questions and the Trends screen.
 * No Android code here, so everything in this file can be unit tested on a PC.
 */
package com.example.chemme.logic

import com.example.chemme.data.DailyLog
import java.time.LocalDate

/** The four "time outdoors" answers and the minutes each one is stored as. */
object LogChoices {
    /** Not at all, under 20 minutes, 20 to 60 minutes, over an hour. */
    val OUTDOOR_MINUTES = listOf(0, 10, 40, 90)

    /** Which answer a stored number of minutes belongs to, so an old entry opens on the right chip. */
    fun outdoorIndex(minutes: Int): Int = when {
        minutes <= 0 -> 0
        minutes < 20 -> 1
        minutes <= 60 -> 2
        else -> 3
    }

    /** Moves a stepper value one step up or down and keeps it inside its limits. */
    fun stepValue(current: Double, step: Double, direction: Int, min: Double, max: Double): Double =
        (current + step * direction).coerceIn(min, max)
}

/** What the Trends screen works out from the saved days. */
object TrendsCalculator {
    const val GOOD_SLEEP_HOURS = 7.0
    private const val MIN_DAYS = 3
    private const val CLEAR_DIFFERENCE = 0.5

    enum class SleepMoodPattern { TOO_FEW_DAYS, BETTER_AFTER_MORE_SLEEP, NOT_WORSE_AFTER_SHORT_SLEEP, NO_CLEAR_LINK }

    /**
     * Compares the average mood on nights of 7 hours or more with the average on shorter nights.
     * This only describes the user's own entries; it does not claim one causes the other.
     */
    fun sleepMoodPattern(logs: List<DailyLog>): SleepMoodPattern {
        if (logs.size < MIN_DAYS) return SleepMoodPattern.TOO_FEW_DAYS
        val (longNights, shortNights) = logs.partition { it.sleepHours >= GOOD_SLEEP_HOURS }
        if (longNights.isEmpty() || shortNights.isEmpty()) return SleepMoodPattern.NO_CLEAR_LINK
        val difference = longNights.map { it.mood }.average() - shortNights.map { it.mood }.average()
        return when {
            difference >= CLEAR_DIFFERENCE -> SleepMoodPattern.BETTER_AFTER_MORE_SLEEP
            difference <= -CLEAR_DIFFERENCE -> SleepMoodPattern.NOT_WORSE_AFTER_SHORT_SLEEP
            else -> SleepMoodPattern.NO_CLEAR_LINK
        }
    }

    /**
     * One value per calendar day for the chart, oldest first, ending on endDate.
     * A day without an entry is null, so the chart can leave a gap instead of drawing zero.
     */
    fun sleepByDay(logs: List<DailyLog>, endDate: LocalDate, days: Int): List<Pair<LocalDate, Double?>> {
        val sleepByDate = logs.associate { it.logDate to it.sleepHours }
        return (days - 1 downTo 0).map { daysAgo ->
            val date = endDate.minusDays(daysAgo.toLong())
            date to sleepByDate[date.toString()]
        }
    }

    /** The saved days that fall inside the last `days` days ending on endDate. */
    fun logsInPeriod(logs: List<DailyLog>, endDate: LocalDate, days: Int): List<DailyLog> {
        val first = endDate.minusDays((days - 1).toLong()).toString()
        val last = endDate.toString()
        return logs.filter { it.logDate in first..last } // ISO dates compare correctly as text.
    }
}
