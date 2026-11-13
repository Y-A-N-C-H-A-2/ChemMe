/**
 * The Room database: lists the four tables and hands out the DAOs.
 */
package com.example.chemme.data

import androidx.room.Database
import androidx.room.RoomDatabase

/** Room generates the concrete subclass. Foreign keys are enforced by Room by default. */
@Database(
    entities = [DailyLog::class, Chemical::class, InsightRule::class, LogInsight::class],
    version = 1,
    exportSchema = false
)
abstract class ChemMeDatabase : RoomDatabase() {
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun chemicalDao(): ChemicalDao
    abstract fun insightDao(): InsightDao
}
