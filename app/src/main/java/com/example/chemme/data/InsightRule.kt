/**
 * Room entity for one rule that links a logged habit to a chemical.
 */
package com.example.chemme.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Example: habitField = "sleepHours", comparator = "<", threshold = 6.0 means
 * "show this message when the user slept less than 6 hours".
 * Each rule belongs to exactly one Chemical (one-to-many: Chemical 1 to 0..* InsightRule).
 */
@Entity(
    tableName = "insight_rule",
    foreignKeys = [
        ForeignKey(
            entity = Chemical::class,
            parentColumns = ["chemicalId"],
            childColumns = ["chemicalId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["chemicalId"])]
)
data class InsightRule(
    @PrimaryKey(autoGenerate = true) val ruleId: Long = 0,
    val chemicalId: Long,
    val habitField: String,
    val comparator: String,     // "<" or ">"
    val threshold: Double,
    val message: String,
    val sourceUrl: String
)
