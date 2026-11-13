/**
 * Trends: sleep per night as a bar chart, the characters seen most, and one pattern in words,
 * for the last 7 or 30 days.
 */
package com.example.chemme.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.chemme.R
import com.example.chemme.data.ChemicalVisit
import com.example.chemme.data.DailyLog
import com.example.chemme.databinding.ActivityTrendsBinding
import com.example.chemme.databinding.ItemTrendVisitorBinding
import com.example.chemme.logic.LogViewModel
import com.example.chemme.logic.TrendsCalculator
import com.example.chemme.logic.TrendsCalculator.SleepMoodPattern
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class TrendsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTrendsBinding
    private val viewModel: LogViewModel by viewModels()

    /** How many days back the screen looks: 7 or 30. */
    private var period = WEEK
    private var watching: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrendsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        BottomNav.setUp(this, binding.bottomNav, Tab.TRENDS)
        period = savedInstanceState?.getInt(KEY_PERIOD) ?: WEEK

        binding.chip7.setOnClickListener { choosePeriod(WEEK) }
        binding.chip30.setOnClickListener { choosePeriod(MONTH) }
        binding.buttonLog.setOnClickListener { startActivity(Intent(this, LogEntryActivity::class.java)) }
        choosePeriod(period)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_PERIOD, period)
    }

    /**
     * Starts watching the database for the chosen period. The earlier watcher is cancelled first,
     * because the "who showed up" query depends on the period's first day.
     */
    private fun choosePeriod(days: Int) {
        period = days
        binding.chip7.isSelected = days == WEEK
        binding.chip30.isSelected = days == MONTH

        watching?.cancel()
        watching = lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val today = LocalDate.now()
                val from = today.minusDays((days - 1).toLong()).toString()
                // combine gives one update whenever either the days or the characters change.
                combine(viewModel.logs, viewModel.observeVisits(from)) { logs, visits -> logs to visits }
                    .collect { (logs, visits) ->
                        if (logs != null) show(today, logs, visits) // null means still loading.
                    }
            }
        }
    }

    private fun show(today: LocalDate, allLogs: List<DailyLog>, visits: List<ChemicalVisit>) {
        val logs = TrendsCalculator.logsInPeriod(allLogs, today, period)
        // Empty state: nothing was logged in this period.
        binding.emptyState.isVisible = logs.isEmpty()
        binding.content.isVisible = logs.isNotEmpty()
        if (logs.isEmpty()) return

        showSleep(today, logs)
        showVisitors(visits, loggedDays = logs.size)
        binding.textPattern.setText(
            when (TrendsCalculator.sleepMoodPattern(logs)) {
                SleepMoodPattern.TOO_FEW_DAYS -> R.string.trends_pattern_few
                SleepMoodPattern.BETTER_AFTER_MORE_SLEEP -> R.string.trends_pattern_more_sleep
                SleepMoodPattern.NOT_WORSE_AFTER_SHORT_SLEEP -> R.string.trends_pattern_less_sleep
                SleepMoodPattern.NO_CLEAR_LINK -> R.string.trends_pattern_none
            }
        )
    }

    private fun showSleep(today: LocalDate, logs: List<DailyLog>) {
        binding.textSleepSubtitle.text = getString(R.string.trends_sleep_subtitle, period)
        val days = TrendsCalculator.sleepByDay(logs, today, period)
        binding.chartSleep.setBars(
            days.mapIndexed { index, (date, hours) ->
                // A week has room for every weekday letter; a month labels every fifth day.
                val label = when {
                    period == WEEK -> Format.dayName(date).take(1)
                    index % 5 == 0 -> Format.dayNumber(date)
                    else -> ""
                }
                BarChartView.Bar(label, hours)
            }
        )
        // The same numbers as text, for people using a screen reader.
        binding.chartSleep.contentDescription = days
            .filter { it.second != null }
            .joinToString(", ") { (date, hours) -> "${Format.date(date.toString())}: ${Format.hours(hours ?: 0.0)}" }
    }

    /** Up to three rows: the character, "3 of 5 logged days", and a bar in the character's colour. */
    private fun showVisitors(visits: List<ChemicalVisit>, loggedDays: Int) {
        binding.listVisitors.removeAllViews()
        binding.textNoVisitors.isVisible = visits.isEmpty()
        for (visit in visits.take(3)) {
            val row = ItemTrendVisitorBinding.inflate(layoutInflater, binding.listVisitors, true)
            row.imageMascot.setImageResource(ChemicalIcons.drawableFor(visit.iconName))
            row.textName.text = visit.characterName
            row.textDays.text = getString(R.string.trends_days_of, visit.days, loggedDays)
            row.progress.max = loggedDays
            row.progress.progress = visit.days
            row.progress.progressTintList = ColorStateList.valueOf(getColor(ChemicalIcons.colorFor(visit.iconName)))
            row.root.setOnClickListener { startActivity(ChemicalDetailActivity.intent(this, visit.chemicalId)) }
        }
    }

    private companion object {
        const val WEEK = 7
        const val MONTH = 30
        const val KEY_PERIOD = "period"
    }
}
