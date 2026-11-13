/**
 * The single doorway to the database. ViewModels call this class and never touch a DAO directly.
 */
package com.example.chemme.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

/** Keeps database details in one place so screens and ViewModels stay simple. */
class ChemMeRepository(
    private val database: ChemMeDatabase,
    private val seedImporter: SeedImporter
) {
    private val logDao = database.dailyLogDao()
    private val chemicalDao = database.chemicalDao()
    private val insightDao = database.insightDao()

    // ---- Daily logs (create, read, update, delete) ----

    fun getLogs(): Flow<List<DailyLog>> = logDao.getAll()

    fun observeLogByDate(date: String): Flow<DailyLog?> = logDao.observeByDate(date)

    suspend fun getLog(id: Long): DailyLog? = logDao.getById(id)

    suspend fun getLogByDate(date: String): DailyLog? = logDao.getByDate(date)

    suspend fun insertLog(log: DailyLog): Long = logDao.insert(log)

    suspend fun updateLog(log: DailyLog) = logDao.update(log)

    /** The CASCADE foreign key removes this log's LogInsight rows as well. */
    suspend fun deleteLog(id: Long) = logDao.deleteById(id)

    suspend fun deleteAllLogs() = logDao.deleteAll()

    // ---- Chemical library (read only) ----

    fun getChemicals(): Flow<List<Chemical>> = chemicalDao.getAll()

    suspend fun getChemical(id: Long): Chemical? = chemicalDao.getById(id)

    // ---- Insight rules and the links to a day ----

    suspend fun getRules(): List<InsightRule> {
        seedIfEmpty() // Rules must exist before the first entry can be analysed.
        return insightDao.getRules()
    }

    /** Replaces the links for one day. Runs in a transaction so an edit never leaves half the old set. */
    suspend fun saveInsights(logId: Long, rules: List<InsightRule>) {
        database.withTransaction {
            insightDao.deleteLinksForLog(logId)
            insightDao.insertLinks(rules.map { LogInsight(logId = logId, ruleId = it.ruleId) })
        }
    }

    suspend fun getInsightCards(logId: Long): List<InsightCard> = insightDao.getRulesForLog(logId)

    suspend fun seedIfEmpty() = seedImporter.importIfEmpty()
}
