/**
 * Unit tests for the app logic. They run on the computer (no emulator needed):
 * right-click this file in Android Studio and choose Run.
 */
package com.example.chemme

import com.example.chemme.data.DailyLog
import com.example.chemme.data.InsightRule
import com.example.chemme.logic.DaylightHelper
import com.example.chemme.logic.InsightEngine
import com.example.chemme.logic.LogChoices
import com.example.chemme.logic.LogValidator
import com.example.chemme.logic.TrendsCalculator
import com.example.chemme.logic.TrendsCalculator.SleepMoodPattern
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicTest {

    /** A sensible day; each test changes only the field it is about. */
    private fun log(
        mood: Int = 4, anxiety: Int = 2, sleepHours: Double = 7.5, caffeineCups: Int = 1,
        daylightHours: Double? = null, supplements: String? = null
    ) = DailyLog(
        logDate = "2026-11-13", mood = mood, energy = 3, anxiety = anxiety, sleepHours = sleepHours,
        caffeineCups = caffeineCups, exerciseMinutes = 20, outdoorMinutes = 30, mealsEaten = 3,
        supplements = supplements, daylightHours = daylightHours
    )

    private fun rule(id: Long, field: String, comparator: String, threshold: Double) = InsightRule(
        ruleId = id, chemicalId = 1, habitField = field, comparator = comparator,
        threshold = threshold, message = "test", sourceUrl = "https://example.com"
    )

    /** A day for the Trends tests, where only the date, mood and sleep matter. */
    private fun day(date: String, mood: Int, sleepHours: Double) =
        log(mood = mood, sleepHours = sleepHours).copy(logDate = date)

    @Test
    fun validInputHasNoErrors() {
        assertTrue(LogValidator.validate(log()).isValid)
    }

    @Test
    fun sleepOfThirtyHoursIsRejected() {
        val result = LogValidator.validate(log(sleepHours = 30.0))
        assertFalse(result.isValid)
        assertEquals("Enter 0 to 24 hours", result.errors[LogValidator.FIELD_SLEEP])
    }

    @Test
    fun emptyNumberBoxIsRejected() {
        val result = LogValidator.validate(log(caffeineCups = LogValidator.MISSING))
        assertTrue(LogValidator.FIELD_CAFFEINE in result.errors)
    }

    @Test
    fun moodOutsideOneToFiveIsRejected() {
        assertTrue(LogValidator.FIELD_MOOD in LogValidator.validate(log(mood = 7)).errors)
    }

    @Test
    fun shortSleepMatchesTheShortSleepRuleOnly() {
        val rules = listOf(rule(1, "sleepHours", "<", 6.0), rule(2, "sleepHours", ">", 6.9))
        val matched = InsightEngine.generate(log(sleepHours = 5.0), rules)
        assertEquals(listOf(1L), matched.map { it.ruleId })
    }

    @Test
    fun valueExactlyOnTheThresholdDoesNotMatch() {
        val rules = listOf(rule(1, "caffeineCups", ">", 2.0))
        assertTrue(InsightEngine.generate(log(caffeineCups = 2), rules).isEmpty())
        assertEquals(1, InsightEngine.generate(log(caffeineCups = 3), rules).size)
    }

    @Test
    fun daylightRuleIsSkippedWhenLocationIsUnknown() {
        val rules = listOf(rule(1, "daylightHours", "<", 9.0))
        assertTrue(InsightEngine.generate(log(daylightHours = null), rules).isEmpty())
        assertEquals(1, InsightEngine.generate(log(daylightHours = 8.4), rules).size)
    }

    @Test
    fun supplementRuleMatchesOnlyWhenTicked() {
        val rules = listOf(rule(1, "supplementVitaminD", ">", 0.0))
        assertEquals(1, InsightEngine.generate(log(supplements = "Vitamin D, Iron"), rules).size)
        assertTrue(InsightEngine.generate(log(supplements = null), rules).isEmpty())
    }

    @Test
    fun unknownFieldNameIsIgnored() {
        assertTrue(InsightEngine.generate(log(), listOf(rule(1, "shoeSize", ">", 0.0))).isEmpty())
    }

    @Test
    fun daylightIsAboutTwelveHoursAtTheEquator() {
        val hours = DaylightHelper.daylightHoursAt(0.0, LocalDate.of(2026, 11, 13))
        assertEquals(12.0, hours, 0.2)
    }

    @Test
    fun dublinHasShortWinterDaysAndLongSummerDays() {
        val dublin = 53.35
        val november = DaylightHelper.daylightHoursAt(dublin, LocalDate.of(2026, 11, 13))
        val june = DaylightHelper.daylightHoursAt(dublin, LocalDate.of(2026, 6, 21))
        assertTrue("November should be about 8 to 9 hours, was $november", november in 8.0..9.0)
        assertTrue("June should be over 16 hours, was $june", june > 16.0)
    }

    @Test
    fun noteOverThreeHundredCharactersIsRejected() {
        val result = LogValidator.validate(log().copy(note = "x".repeat(301)))
        assertEquals(setOf(LogValidator.FIELD_NOTE), result.errors.keys)
        assertTrue(LogValidator.validate(log().copy(note = "x".repeat(300))).isValid)
    }

    @Test
    fun outdoorAnswersAreStoredAsMinutesThatFitTheRules() {
        // "Not at all" and "Under 20 min" must both fall under the 20-minute daylight rules.
        val rules = listOf(rule(1, "outdoorMinutes", "<", 20.0))
        val matches = LogChoices.OUTDOOR_MINUTES.map { minutes ->
            InsightEngine.generate(log().copy(outdoorMinutes = minutes), rules).isNotEmpty()
        }
        assertEquals(listOf(true, true, false, false), matches)
    }

    @Test
    fun storedOutdoorMinutesOpenOnTheMatchingAnswer() {
        assertEquals(listOf(0, 1, 2, 3), LogChoices.OUTDOOR_MINUTES.map { LogChoices.outdoorIndex(it) })
        assertEquals(1, LogChoices.outdoorIndex(19))
        assertEquals(2, LogChoices.outdoorIndex(20))
        assertEquals(2, LogChoices.outdoorIndex(60))
        assertEquals(3, LogChoices.outdoorIndex(61))
    }

    @Test
    fun stepperStopsAtItsLimits() {
        assertEquals(8.0, LogChoices.stepValue(7.5, 0.5, +1, 0.0, 24.0), 0.0)
        assertEquals(0.0, LogChoices.stepValue(0.0, 0.5, -1, 0.0, 24.0), 0.0)
        assertEquals(24.0, LogChoices.stepValue(24.0, 0.5, +1, 0.0, 24.0), 0.0)
    }

    @Test
    fun trendsNeedThreeDaysBeforeNamingAPattern() {
        val logs = listOf(day("2026-11-12", 5, 8.0), day("2026-11-13", 2, 5.0))
        assertEquals(SleepMoodPattern.TOO_FEW_DAYS, TrendsCalculator.sleepMoodPattern(logs))
    }

    @Test
    fun trendsSeeBetterMoodAfterLongerSleep() {
        val logs = listOf(day("2026-11-11", 4, 8.0), day("2026-11-12", 5, 7.0), day("2026-11-13", 2, 5.0))
        assertEquals(SleepMoodPattern.BETTER_AFTER_MORE_SLEEP, TrendsCalculator.sleepMoodPattern(logs))
    }

    @Test
    fun trendsMakeNoClaimWhenEveryNightIsTheSameKind() {
        val logs = listOf(day("2026-11-11", 2, 8.0), day("2026-11-12", 5, 7.0), day("2026-11-13", 3, 9.0))
        assertEquals(SleepMoodPattern.NO_CLEAR_LINK, TrendsCalculator.sleepMoodPattern(logs))
    }

    @Test
    fun sleepChartHasOneSlotPerDayAndGapsForMissingDays() {
        val logs = listOf(day("2026-11-11", 4, 8.0), day("2026-11-13", 2, 5.0), day("2026-10-01", 3, 6.0))
        val today = LocalDate.of(2026, 11, 13)
        val inWeek = TrendsCalculator.logsInPeriod(logs, today, 7)
        assertEquals(2, inWeek.size) // The October day is outside the last 7 days.

        val bars = TrendsCalculator.sleepByDay(inWeek, today, 7)
        assertEquals(7, bars.size)
        assertEquals(LocalDate.of(2026, 11, 7), bars.first().first)
        assertEquals(listOf(null, null, null, null, 8.0, null, 5.0), bars.map { it.second })
    }
}
