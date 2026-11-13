/**
 * Settings: the daily reminder, whether location may be used, and clearing all entries.
 */
package com.example.chemme.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.chemme.R
import com.example.chemme.databinding.ActivitySettingsBinding
import com.example.chemme.logic.AppPrefs
import com.example.chemme.logic.LogViewModel
import com.example.chemme.logic.ReminderScheduler
import kotlinx.coroutines.launch

class SettingsActivity : BackActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: LogViewModel by viewModels()
    private lateinit var prefs: AppPrefs
    private lateinit var scheduler: ReminderScheduler

    // Android 13+ needs the user's permission before any notification can appear.
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) Toast.makeText(this, R.string.message_notifications_denied, Toast.LENGTH_LONG).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpTopBar(binding.topBar, getString(R.string.title_settings))
        prefs = AppPrefs(this)
        scheduler = ReminderScheduler(this)

        // Set the starting positions before attaching listeners, so this does not count as a change.
        binding.switchReminder.isChecked = prefs.reminderEnabled
        binding.switchLocation.isChecked = prefs.useLocation
        showReminderTime()

        binding.switchReminder.setOnCheckedChangeListener { _, isOn -> onReminderChanged(isOn) }
        binding.rowTime.setOnClickListener { pickTime() }
        binding.switchLocation.setOnCheckedChangeListener { _, isOn -> prefs.useLocation = isOn }
        binding.buttonDeleteAll.setOnClickListener { onDeleteAll() }
    }

    private fun onReminderChanged(enabled: Boolean) {
        prefs.reminderEnabled = enabled
        if (enabled) {
            askForNotificationPermission()
            scheduler.schedule(prefs.reminderHour, prefs.reminderMinute)
        } else {
            scheduler.cancel()
        }
    }

    private fun askForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /** Shows the system clock dialog and reschedules the alarm if the reminder is switched on. */
    private fun pickTime() {
        TimePickerDialog(this, { _, hour, minute ->
            prefs.reminderHour = hour
            prefs.reminderMinute = minute
            showReminderTime()
            if (prefs.reminderEnabled) scheduler.schedule(hour, minute)
        }, prefs.reminderHour, prefs.reminderMinute, true).show()
    }

    private fun showReminderTime() {
        binding.textTime.text = Format.time(prefs.reminderHour, prefs.reminderMinute)
    }

    /** Removes every DailyLog (and, through CASCADE, every LogInsight). The library is kept. */
    private fun onDeleteAll() {
        ConfirmDialog.show(this, R.string.dialog_delete_all_title, R.string.dialog_delete_all_message) {
            lifecycleScope.launch {
                viewModel.deleteAllLogs()
                Toast.makeText(this@SettingsActivity, R.string.message_all_deleted, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
