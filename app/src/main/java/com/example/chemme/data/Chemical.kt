/**
 * Room entity for one chemical in the library (serotonin, cortisol, vitamin D and so on).
 */
package com.example.chemme.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Seeded from assets/chemicals.csv on first launch. The user reads these rows but never edits them. */
@Entity(
    tableName = "chemical",
    indices = [Index(value = ["name"], unique = true)]
)
data class Chemical(
    @PrimaryKey(autoGenerate = true) val chemicalId: Long = 0,
    val name: String,           // "Cortisol"
    val characterName: String,  // "Corty"
    val category: String,       // "Hormone", "Neurotransmitter", "Nutrient"
    val role: String,           // what the chemical does
    val supportedBy: String,    // habits linked to it
    val iconName: String,       // drawable name such as "ic_corty"
    val sourceUrl: String       // article the text is based on
)
