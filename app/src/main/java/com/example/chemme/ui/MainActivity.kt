/**
 * Home: shows whether today is logged, this week's logged days and the characters seen lately.
 */
package com.example.chemme.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.chemme.R
import com.example.chemme.data.ChemicalVisit
import com.example.chemme.data.HistoryRow
import com.example.chemme.databinding.ActivityMainBinding
import com.example.chemme.databinding.ItemDayBinding
import com.example.chemme.databinding.ItemVisitorBinding
import com.example.chemme.logic.AppPrefs
import com.example.chemme.logic.LogViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: LogViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // First launch: show the introduction instead, and come back here when it is finished.
        if (!AppPrefs(this).onboardingDone) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        BottomNav.setUp(this, binding.bottomNav, Tab.HOME)
        binding.buttonSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }

        // Both Flows come from Room, so Home redraws as soon as an entry is saved or deleted.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.historyRows.collect { rows ->
                        if (rows != null) showDays(rows) // null means still loading.
                    }
                }
                launch {
                    // The last 7 days including today.
                    val from = LocalDate.now().minusDays(6).toString()
                    viewModel.observeVisits(from).collect { visits -> showVisitors(visits) }
                }
            }
        }
    }

    private fun showDays(rows: List<HistoryRow>) {
        val today = LocalDate.now()
        binding.textDate.text = Format.longDate(today)
        showToday(rows.firstOrNull { it.log.logDate == today.toString() })
        showWeek(today, rows.map { it.log.logDate }.toSet())
    }

    /** The big card: either "How was today?" with Log today, or today's numbers with See insights. */
    private fun showToday(row: HistoryRow?) = with(binding) {
        if (row == null) {
            imageHero.setImageResource(R.drawable.ic_sera)
            textHero.setText(R.string.home_greeting)
            chipsToday.isVisible = false
            buttonEdit.isVisible = false
            buttonLog.setText(R.string.button_log_today)
            buttonLog.setOnClickListener { startActivity(Intent(this@MainActivity, LogEntryActivity::class.java)) }
            return@with
        }

        val log = row.log
        // One of today's characters speaks; Sera fills in when no rule matched.
        val icon = row.icons.firstOrNull()
        imageHero.setImageResource(if (icon == null) R.drawable.ic_sera else ChemicalIcons.drawableFor(icon))
        textHero.setText(if (icon == null) R.string.home_logged_no_insight else R.string.home_logged_with_insight)

        chipsToday.isVisible = true
        Chips.show(
            chipsToday,
            listOf(
                getString(R.string.chip_mood, log.mood),
                getString(R.string.chip_sleep, Format.hours(log.sleepHours)),
                resources.getQuantityString(R.plurals.coffees, log.caffeineCups, log.caffeineCups)
            )
        )

        buttonLog.setText(R.string.button_see_insights)
        buttonLog.setOnClickListener { startActivity(InsightsActivity.intent(this@MainActivity, log.logId)) }
        buttonEdit.isVisible = true
        buttonEdit.setOnClickListener {
            startActivity(
                Intent(this@MainActivity, LogEntryActivity::class.java)
                    .putExtra(LogEntryActivity.EXTRA_LOG_ID, log.logId)
            )
        }
    }

    /** Monday to Sunday of the current week: a tick for logged days, a ring around today. */
    private fun showWeek(today: LocalDate, loggedDates: Set<String>) {
        binding.weekStrip.removeAllViews()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        for (offset in 0 until 7) {
            val date = monday.plusDays(offset.toLong())
            val logged = date.toString() in loggedDates
            val day = ItemDayBinding.inflate(layoutInflater, binding.weekStrip, true)
            day.textLetter.text = Format.dayName(date).take(1)
            day.textNumber.text = Format.dayNumber(date)
            day.textNumber.isVisible = !logged
            day.imageCheck.isVisible = logged
            day.circle.setBackgroundResource(
                when {
                    logged -> R.drawable.bg_day_done
                    date == today -> R.drawable.bg_day_today
                    else -> R.drawable.bg_day_empty
                }
            )
            if (date == today && !logged) day.textNumber.setTextColor(getColor(R.color.text_brand))
        }
    }

    /** "Who's been around": the three characters seen on the most days this week. */
    private fun showVisitors(visits: List<ChemicalVisit>) {
        binding.sectionVisitors.isVisible = visits.isNotEmpty()
        binding.rowVisitors.removeAllViews()
        for (visit in visits.take(3)) {
            val card = ItemVisitorBinding.inflate(layoutInflater, binding.rowVisitors, true)
            card.imageMascot.setImageResource(ChemicalIcons.drawableFor(visit.iconName))
            card.textName.text = visit.characterName
            card.textDays.text = resources.getQuantityString(R.plurals.days, visit.days, visit.days)
            card.root.setOnClickListener { startActivity(ChemicalDetailActivity.intent(this, visit.chemicalId)) }
        }
    }
}
