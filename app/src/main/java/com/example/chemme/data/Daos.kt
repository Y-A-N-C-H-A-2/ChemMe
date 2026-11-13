/**
 * Data access objects. Room generates the implementations of these interfaces at build time.
 * suspend functions run off the main thread; Flow functions keep emitting when the table changes.
 */
package com.example.chemme.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Create, read, update and delete for the user's daily entries. */
@Dao
interface DailyLogDao {
    /** Returns the new row's auto-generated logId. */
    @Insert
    suspend fun insert(log: DailyLog): Long

    @Update
    suspend fun update(log: DailyLog)

    @Query("DELETE FROM daily_log WHERE logId = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM daily_log")
    suspend fun deleteAll()

    /** Newest day first. Emits a new list every time a log is added, changed or removed. */
    @Query("SELECT * FROM daily_log ORDER BY logDate DESC")
    fun getAll(): Flow<List<DailyLog>>

    @Query("SELECT * FROM daily_log WHERE logId = :id")
    suspend fun getById(id: Long): DailyLog?

    @Query("SELECT * FROM daily_log WHERE logDate = :date")
    suspend fun getByDate(date: String): DailyLog?

    /** Used by the Home screen to show whether today has been logged. */
    @Query("SELECT * FROM daily_log WHERE logDate = :date")
    fun observeByDate(date: String): Flow<DailyLog?>
}

/** Read access to the seeded chemical library. */
@Dao
interface ChemicalDao {
    @Insert
    suspend fun insertAll(chemicals: List<Chemical>)

    @Query("SELECT * FROM chemical ORDER BY name")
    fun getAll(): Flow<List<Chemical>>

    @Query("SELECT * FROM chemical")
    suspend fun getAllOnce(): List<Chemical>

    @Query("SELECT * FROM chemical WHERE chemicalId = :id")
    suspend fun getById(id: Long): Chemical?

    @Query("SELECT COUNT(*) FROM chemical")
    suspend fun count(): Int
}

/** Rules, and the links between a day and the rules it matched. */
@Dao
interface InsightDao {
    @Insert
    suspend fun insertRules(rules: List<InsightRule>)

    @Query("SELECT * FROM insight_rule")
    suspend fun getRules(): List<InsightRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLinks(links: List<LogInsight>)

    @Query("DELETE FROM log_insight WHERE logId = :logId")
    suspend fun deleteLinksForLog(logId: Long)

    /** Joins the three tables so each result row is one ready-to-show insight card. */
    @Query(
        """
        SELECT r.ruleId AS ruleId, r.message AS message, r.sourceUrl AS sourceUrl,
               c.chemicalId AS chemicalId, c.name AS chemicalName,
               c.characterName AS characterName, c.iconName AS iconName
        FROM log_insight li
        INNER JOIN insight_rule r ON r.ruleId = li.ruleId
        INNER JOIN chemical c ON c.chemicalId = r.chemicalId
        WHERE li.logId = :logId
        ORDER BY c.name
        """
    )
    suspend fun getRulesForLog(logId: Long): List<InsightCard>
}
