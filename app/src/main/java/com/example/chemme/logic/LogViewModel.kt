/**
 * App logic for everything about daily logs: listing, saving with validation, insights and deleting.
 */
package com.example.chemme.logic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chemme.ChemMeApplication
import com.example.chemme.data.ChemicalVisit
import com.example.chemme.data.DailyLog
import com.example.chemme.data.HistoryRow
import com.example.chemme.data.InsightCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Survives rotation and keeps database work out of the activities. */
class LogViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as ChemMeApplication).repository

    /** null means "still loading", so History can tell loading apart from a truly empty list. */
    val logs: StateFlow<List<DailyLog>?> = repository.getLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Every saved day with the faces of its characters, newest first. Used by Home and History.
     * combine() re-runs whenever either table changes; null still means "loading".
     */
    val historyRows: StateFlow<List<HistoryRow>?> =
        combine(repository.getLogs(), repository.getLogIcons()) { logs, icons ->
            val iconsByLog = icons.groupBy({ it.logId }, { it.iconName })
            logs.map { log -> HistoryRow(log, iconsByLog[log.logId].orEmpty()) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        // Import the chemicals and rules in the background the first time the app runs.
        viewModelScope.launch { repository.seedIfEmpty() }
    }

    fun observeLog(date: String): Flow<DailyLog?> = repository.observeLogByDate(date)

    /** The characters seen on logged days from fromDate (an ISO date) onwards, most frequent first. */
    fun observeVisits(fromDate: String): Flow<List<ChemicalVisit>> = repository.getVisits(fromDate)

    suspend fun getLog(id: Long): DailyLog? = repository.getLog(id)

    suspend fun getLogByDate(date: String): DailyLog? = repository.getLogByDate(date)

    /**
     * The core workflow: validate, store, then work out and store the insights.
     * Nothing is written when validation fails.
     */
    suspend fun saveLog(log: DailyLog): ValidationResult {
        val result = LogValidator.validate(log)
        if (!result.isValid) return result

        // Edge case: a second entry for a date that already has one updates the existing row.
        val existingId = if (log.logId != 0L) log.logId else repository.getLogByDate(log.logDate)?.logId
        val savedId = if (existingId == null) {
            repository.insertLog(log)
        } else {
            repository.updateLog(log.copy(logId = existingId))
            existingId
        }

        val matchedRules = InsightEngine.generate(log, repository.getRules())
        repository.saveInsights(savedId, matchedRules)
        return result.copy(savedLogId = savedId)
    }

    suspend fun getInsights(id: Long): List<InsightCard> = repository.getInsightCards(id)

    suspend fun deleteLog(id: Long) = repository.deleteLog(id)

    suspend fun deleteAllLogs() = repository.deleteAllLogs()
}
