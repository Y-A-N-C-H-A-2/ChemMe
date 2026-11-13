/**
 * Junction table between DailyLog and InsightRule, plus the joined row shape used by the Insights screen.
 */
package com.example.chemme.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * One row means "this rule matched this day". Together the two foreign keys form the primary key,
 * which gives a many-to-many relationship between days and rules.
 * Deleting a DailyLog removes its LogInsight rows automatically (CASCADE).
 */
@Entity(
    tableName = "log_insight",
    primaryKeys = ["logId", "ruleId"],
    foreignKeys = [
        ForeignKey(
            entity = DailyLog::class,
            parentColumns = ["logId"],
            childColumns = ["logId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = InsightRule::class,
            parentColumns = ["ruleId"],
            childColumns = ["ruleId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["ruleId"])]
)
data class LogInsight(
    val logId: Long,
    val ruleId: Long
)

/**
 * Not a table. This is the result of joining log_insight, insight_rule and chemical,
 * so one object holds everything an insight card needs to display.
 */
data class InsightCard(
    val ruleId: Long,
    val message: String,
    val sourceUrl: String,
    val chemicalId: Long,
    val chemicalName: String,
    val characterName: String,
    val iconName: String
)
