/**
 * Decides which insight rules match a day's entry. Contains no Android code, so it can be unit tested.
 */
package com.example.chemme.logic

import com.example.chemme.data.DailyLog
import com.example.chemme.data.InsightRule

/**
 * Design choice: the rules are data (rows in Room, seeded from rules.csv), and this class only
 * knows how to compare a field with a threshold. Adding an explanation needs no new Kotlin code.
 */
object InsightEngine {

    /** Returns every rule whose condition is true for this log. */
    fun generate(log: DailyLog, rules: List<InsightRule>): List<InsightRule> =
        rules.filter { rule ->
            // A null value means "not known" (for example daylight without location): never match.
            val value = valueOf(log, rule.habitField) ?: return@filter false
            when (rule.comparator) {
                "<" -> value < rule.threshold
                ">" -> value > rule.threshold
                else -> false
            }
        }

    /** Turns a habitField name from the rules table into the matching number from the log. */
    fun valueOf(log: DailyLog, habitField: String): Double? = when (habitField) {
        "mood" -> log.mood.toDouble()
        "energy" -> log.energy.toDouble()
        "anxiety" -> log.anxiety.toDouble()
        "sleepHours" -> log.sleepHours
        "caffeineCups" -> log.caffeineCups.toDouble()
        "exerciseMinutes" -> log.exerciseMinutes.toDouble()
        "outdoorMinutes" -> log.outdoorMinutes.toDouble()
        "mealsEaten" -> log.mealsEaten.toDouble()
        "daylightHours" -> log.daylightHours
        // Supplements are stored as text; these give 1.0 when that supplement was ticked.
        "supplementVitaminD" -> took(log, "Vitamin D")
        "supplementB12" -> took(log, "B12")
        "supplementIron" -> took(log, "Iron")
        else -> null // Unknown field name in rules.csv: ignore the rule instead of crashing.
    }

    private fun took(log: DailyLog, name: String): Double =
        if (log.supplements?.contains(name, ignoreCase = true) == true) 1.0 else 0.0
}
