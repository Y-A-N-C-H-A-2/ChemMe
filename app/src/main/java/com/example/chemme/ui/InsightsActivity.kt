/**
 * Screen 3, Insights: the cards for one day, with Edit and Delete for that entry.
 */
package com.example.chemme.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chemme.R
import com.example.chemme.databinding.ActivityInsightsBinding
import com.example.chemme.logic.LogViewModel
import kotlinx.coroutines.launch

class InsightsActivity : BackActivity() {
    private lateinit var binding: ActivityInsightsBinding
    private val viewModel: LogViewModel by viewModels()
    private var logId: Long = 0

    // Tapping a card opens the chemical it talks about.
    private val adapter = InsightAdapter { card ->
        startActivity(ChemicalDetailActivity.intent(this, card.chemicalId))
    }

    // Opens Log Entry for editing; when it reports success, show the "Entry updated" banner.
    private val editEntry =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) showBanner(R.string.banner_updated)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInsightsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        logId = intent.getLongExtra(EXTRA_LOG_ID, 0L)

        binding.recyclerInsights.layoutManager = LinearLayoutManager(this)
        binding.recyclerInsights.adapter = adapter

        // Success state: shown only when arriving straight from a save, not after a rotation.
        if (intent.getBooleanExtra(EXTRA_JUST_SAVED, false) && savedInstanceState == null) {
            showBanner(R.string.banner_saved)
        }

        binding.buttonEdit.setOnClickListener {
            editEntry.launch(
                Intent(this, LogEntryActivity::class.java)
                    .putExtra(LogEntryActivity.EXTRA_LOG_ID, logId)
                    .putExtra(LogEntryActivity.EXTRA_FROM_INSIGHTS, true)
            )
        }
        binding.buttonDelete.setOnClickListener { confirmDelete() }
        binding.buttonDone.setOnClickListener { finish() }
    }

    /** Reloading in onResume means the cards are fresh when returning from an edit. */
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { showInsights() }
    }

    private suspend fun showInsights() {
        val log = viewModel.getLog(logId)
        if (log == null) { // The entry no longer exists (for example, all data was cleared).
            finish()
            return
        }
        title = getString(R.string.title_insights_date, Format.date(log.logDate))
        binding.textSummary.text = getString(
            R.string.insights_summary,
            log.mood, log.energy, log.anxiety, Format.hours(log.sleepHours), log.caffeineCups
        )

        val cards = viewModel.getInsights(logId)
        adapter.submitList(cards)
        binding.textEmpty.isVisible = cards.isEmpty()
        binding.textHint.isVisible = cards.isNotEmpty()
    }

    private fun showBanner(textId: Int) {
        binding.textBanner.setText(textId)
        binding.textBanner.isVisible = true
    }

    /** Deleting cannot be undone, so ask first. Cancel leaves everything as it was. */
    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_delete_title)
            .setMessage(R.string.dialog_delete_message)
            .setNegativeButton(R.string.button_cancel, null)
            .setPositiveButton(R.string.button_delete) { _, _ ->
                lifecycleScope.launch {
                    viewModel.deleteLog(logId)
                    Toast.makeText(this@InsightsActivity, R.string.message_deleted, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .show()
    }

    companion object {
        private const val EXTRA_LOG_ID = "extra_log_id"
        private const val EXTRA_JUST_SAVED = "extra_just_saved"

        /** Builds the Intent for this screen so callers cannot forget or misspell an extra. */
        fun intent(context: Context, logId: Long, justSaved: Boolean = false): Intent =
            Intent(context, InsightsActivity::class.java)
                .putExtra(EXTRA_LOG_ID, logId)
                .putExtra(EXTRA_JUST_SAVED, justSaved)
    }
}
