/**
 * Screen 1, Home: shows whether today is logged and opens the other screens with Intents.
 */
package com.example.chemme.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.chemme.R
import com.example.chemme.databinding.ActivityMainBinding
import com.example.chemme.logic.LogViewModel
import java.time.LocalDate
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: LogViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonLog.setOnClickListener { openScreen(LogEntryActivity::class.java) }
        binding.buttonHistory.setOnClickListener { openScreen(HistoryActivity::class.java) }
        binding.buttonLibrary.setOnClickListener { openScreen(LibraryActivity::class.java) }
        binding.buttonSettings.setOnClickListener { openScreen(SettingsActivity::class.java) }

        // Watch today's row in Room, so the card updates as soon as an entry is saved or deleted.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.observeLog(LocalDate.now().toString()).collect { log ->
                    if (log == null) {
                        binding.textToday.text = getString(R.string.home_not_logged)
                        binding.buttonLog.text = getString(R.string.button_log_today)
                    } else {
                        binding.textToday.text =
                            getString(R.string.home_logged, log.mood, Format.hours(log.sleepHours))
                        binding.buttonLog.text = getString(R.string.button_edit_today)
                    }
                }
            }
        }
    }

    /** An explicit Intent names the activity class to start. */
    private fun openScreen(target: Class<*>) {
        startActivity(Intent(this, target))
    }
}
