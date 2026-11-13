/**
 * Screen 2, Log Entry: the form used both to create today's entry and to edit an existing one.
 */
package com.example.chemme.ui

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.chemme.R
import com.example.chemme.data.DailyLog
import com.example.chemme.databinding.ActivityLogEntryBinding
import com.example.chemme.logic.AppPrefs
import com.example.chemme.logic.DaylightHelper
import com.example.chemme.logic.LogValidator
import com.example.chemme.logic.LogViewModel
import com.example.chemme.logic.ValidationResult
import java.time.LocalDate
import kotlinx.coroutines.launch

class LogEntryActivity : BackActivity() {
    private lateinit var binding: ActivityLogEntryBinding
    private val viewModel: LogViewModel by viewModels()
    private lateinit var daylightHelper: DaylightHelper

    /** The row being edited, or null when this is a new entry. */
    private var existing: DailyLog? = null
    private var logDate: String = LocalDate.now().toString()
    private var daylightHours: Double? = null

    // Asks for location permission; whatever the answer, try to show daylight afterwards.
    private val locationPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { loadDaylight() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        daylightHelper = DaylightHelper(this)

        setUpRating(binding.seekMood, binding.textMoodLabel, R.string.label_mood)
        setUpRating(binding.seekEnergy, binding.textEnergyLabel, R.string.label_energy)
        setUpRating(binding.seekAnxiety, binding.textAnxietyLabel, R.string.label_anxiety)

        binding.buttonCancel.setOnClickListener { finish() } // Discards anything typed.
        binding.buttonSave.setOnClickListener { onSaveClicked() }

        // After a rotation Android restores what was typed, so only fill the form the first time.
        val firstCreate = savedInstanceState == null
        lifecycleScope.launch { loadEntry(firstCreate) }
    }

    override fun onDestroy() {
        daylightHelper.stop()
        super.onDestroy()
    }

    /** Keeps a "Mood: 3/5" label in step with its SeekBar (which runs 0 to 4). */
    private fun setUpRating(seekBar: SeekBar, label: TextView, labelText: Int) {
        label.text = getString(labelText, seekBar.progress + 1)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                label.text = getString(labelText, progress + 1)
            }

            override fun onStartTrackingTouch(bar: SeekBar) {}
            override fun onStopTrackingTouch(bar: SeekBar) {}
        })
    }

    /**
     * Finds the entry to edit: the one named in the Intent, or today's entry if it already exists
     * (so the same date is never logged twice). Otherwise the form stays blank for a new entry.
     */
    private suspend fun loadEntry(firstCreate: Boolean) {
        val id = intent.getLongExtra(EXTRA_LOG_ID, 0L)
        existing = if (id != 0L) viewModel.getLog(id) else viewModel.getLogByDate(logDate)

        existing?.let { log ->
            logDate = log.logDate
            daylightHours = log.daylightHours
            if (firstCreate) fillForm(log)
        }
        title = getString(if (existing == null) R.string.title_log_entry else R.string.title_edit_entry)
        binding.textDate.text = Format.date(logDate)

        when {
            daylightHours != null -> showDaylight() // Already stored with the entry.
            !AppPrefs(this).useLocation -> binding.textDaylight.text = getString(R.string.daylight_off)
            daylightHelper.hasPermission() -> loadDaylight()
            firstCreate -> locationPermission.launch(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
            )
            else -> showDaylight()
        }
    }

    private fun fillForm(log: DailyLog) = with(binding) {
        seekMood.progress = log.mood - 1
        seekEnergy.progress = log.energy - 1
        seekAnxiety.progress = log.anxiety - 1
        editSleep.setText(log.sleepHours.toString())
        editCaffeine.setText(log.caffeineCups.toString())
        editExercise.setText(log.exerciseMinutes.toString())
        editOutdoor.setText(log.outdoorMinutes.toString())
        editMeals.setText(log.mealsEaten.toString())
        val taken = log.supplements.orEmpty()
        checkVitaminD.isChecked = taken.contains("Vitamin D")
        checkB12.isChecked = taken.contains("B12")
        checkIron.isChecked = taken.contains("Iron")
        editNote.setText(log.note.orEmpty())
    }

    /** Asks DaylightHelper for today's daylight; the answer may arrive a moment later. */
    private fun loadDaylight() {
        if (!daylightHelper.hasPermission()) {
            showDaylight() // Permission denied: the entry can still be saved without daylight.
            return
        }
        binding.textDaylight.text = getString(R.string.daylight_searching)
        daylightHelper.requestDaylightHours(LocalDate.parse(logDate)) { hours ->
            daylightHours = hours
            showDaylight()
        }
    }

    private fun showDaylight() {
        val hours = daylightHours
        binding.textDaylight.text =
            if (hours == null) getString(R.string.daylight_unavailable)
            else getString(R.string.daylight_value, Format.hoursAndMinutes(hours))
    }

    /** Builds a DailyLog from what is on screen. Empty or non-numeric boxes become -1 (invalid). */
    private fun readForm(): DailyLog = with(binding) {
        val supplements = listOfNotNull(
            "Vitamin D".takeIf { checkVitaminD.isChecked },
            "Vitamin B12".takeIf { checkB12.isChecked },
            "Iron".takeIf { checkIron.isChecked }
        ).joinToString(", ")

        DailyLog(
            logId = existing?.logId ?: 0,
            logDate = logDate,
            mood = seekMood.progress + 1,
            energy = seekEnergy.progress + 1,
            anxiety = seekAnxiety.progress + 1,
            sleepHours = decimal(editSleep),
            caffeineCups = whole(editCaffeine),
            exerciseMinutes = whole(editExercise),
            outdoorMinutes = whole(editOutdoor),
            mealsEaten = whole(editMeals),
            supplements = supplements.ifEmpty { null },
            daylightHours = daylightHours,
            note = editNote.text.toString().trim().ifEmpty { null },
            createdAt = existing?.createdAt ?: System.currentTimeMillis()
        )
    }

    private fun whole(field: EditText): Int =
        field.text.toString().trim().toIntOrNull() ?: LogValidator.MISSING

    /** Accepts both "7.5" and "7,5", because some keyboards type a comma. */
    private fun decimal(field: EditText): Double =
        field.text.toString().trim().replace(',', '.').toDoubleOrNull() ?: LogValidator.MISSING.toDouble()

    private fun onSaveClicked() {
        val log = readForm()
        binding.buttonSave.isEnabled = false // Prevents a double tap saving twice.
        lifecycleScope.launch {
            val result = viewModel.saveLog(log)
            binding.buttonSave.isEnabled = true
            showErrors(result)

            val savedId = result.savedLogId
            if (savedId == null) {
                // Invalid input: stay on this screen with the typed values still in place.
                Toast.makeText(this@LogEntryActivity, R.string.message_fix_errors, Toast.LENGTH_LONG).show()
            } else if (intent.getBooleanExtra(EXTRA_FROM_INSIGHTS, false)) {
                // Editing from Insights: go back there and let it show "Entry updated".
                setResult(Activity.RESULT_OK)
                finish()
            } else {
                startActivity(InsightsActivity.intent(this@LogEntryActivity, savedId, justSaved = true))
                finish() // Back from Insights should not return to the filled-in form.
            }
        }
    }

    /** Shows each field's message under it in red, and hides the lines that have no error. */
    private fun showErrors(result: ValidationResult) = with(binding) {
        val lines = mapOf(
            LogValidator.FIELD_SLEEP to errorSleep,
            LogValidator.FIELD_CAFFEINE to errorCaffeine,
            LogValidator.FIELD_EXERCISE to errorExercise,
            LogValidator.FIELD_OUTDOOR to errorOutdoor,
            LogValidator.FIELD_MEALS to errorMeals,
            LogValidator.FIELD_NOTE to errorNote
        )
        for ((field, line) in lines) {
            val message = result.errors[field]
            line.text = message
            line.isVisible = message != null
        }
    }

    companion object {
        /** Intent extra: the logId to edit. Absent when creating today's entry. */
        const val EXTRA_LOG_ID = "extra_log_id"
        /** Intent extra: true when opened by the Edit button on the Insights screen. */
        const val EXTRA_FROM_INSIGHTS = "extra_from_insights"
    }
}
