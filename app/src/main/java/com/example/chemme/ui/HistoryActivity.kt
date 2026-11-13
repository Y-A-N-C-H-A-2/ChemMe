/**
 * History: every saved day in a RecyclerView, newest first, or the empty state.
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chemme.data.HistoryRow
import com.example.chemme.databinding.ActivityHistoryBinding
import com.example.chemme.logic.LogViewModel
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding
    private val viewModel: LogViewModel by viewModels()

    // Tapping a row opens that day's insights.
    private val adapter = LogAdapter { row ->
        startActivity(InsightsActivity.intent(this, row.log.logId))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        BottomNav.setUp(this, binding.bottomNav, Tab.HISTORY)

        binding.recyclerLogs.layoutManager = LinearLayoutManager(this)
        binding.recyclerLogs.adapter = adapter
        binding.buttonLog.setOnClickListener { startActivity(Intent(this, LogEntryActivity::class.java)) }

        // The Flow from Room emits again after every insert, update or delete, so the list
        // refreshes by itself when the user comes back from another screen.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.historyRows.collect { rows ->
                    if (rows != null) showRows(rows) // null means still loading.
                }
            }
        }
    }

    private fun showRows(rows: List<HistoryRow>) {
        adapter.submitList(rows)
        showEmptyState(rows.isEmpty())
    }

    /** Swaps between the list and the "No entries yet" message. */
    private fun showEmptyState(empty: Boolean) {
        binding.emptyState.isVisible = empty
        binding.recyclerLogs.isVisible = !empty
    }
}
