/**
 * Unit tests for the app logic. They run on the computer (no emulator needed):
 * right-click this file in Android Studio and choose Run.
 */
package com.example.chemme

import com.example.chemme.data.DailyLog
import com.example.chemme.data.InsightRule
import com.example.chemme.logic.DaylightHelper
import com.example.chemme.logic.InsightEngine
import com.example.chemme.logic.LogValidator
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
}
