/**
 * Log Entry: nine questions asked one at a time, used both to create today's entry and to edit one.
 * The answers are kept in this activity until the last step, where they are validated and saved.
 */
package com.example.chemme.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.chemme.R
import com.example.chemme.data.DailyLog
import com.example.chemme.databinding.ActivityLogEntryBinding
import com.example.chemme.logic.AppPrefs
import com.example.chemme.logic.DaylightHelper
import com.example.chemme.logic.LogChoices
import com.example.chemme.logic.LogValidator
import com.example.chemme.logic.LogViewModel
import com.example.chemme.logic.ValidationResult
import java.time.LocalDate
import kotlinx.coroutines.launch

class LogEntryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLogEntryBinding
    private val viewModel: LogViewModel by viewModels()
    private lateinit var daylightHelper: DaylightHelper
    private lateinit var prefs: AppPrefs

    /** The row being edited, or null when this is a new entry. */
    private var existing: DailyLog? = null
    private var logDate: String = LocalDate.now().toString()
    private var daylightHours: Double? = null
    private var askedForLocation = false
    private var loaded = false

    // ---- The answers so far. 0 on a scale and -1 for outdoors mean "not answered yet". ----
    private var step = STEP_MOOD
    private var mood = 0
    private var energy = 0
    private var anxiety = 0
    private var sleepHours = 7.5
    private var caffeineCups = 0
    private var exerciseMinutes = 0
    private var outdoorMinutes = NOT_ANSWERED
    private var mealsEaten = 3
    private var tookVitaminD = false
    private var tookB12 = false
    private var tookIron = false

    /** Everything that differs between the four number questions. */
    private class Stepper(
        val unitId: Int,
        val stepSize: Double,
        val max: Double,
        val shortcuts: List<Int> = emptyList(),
        val shortcutLabelId: Int = 0,
        val hintId: Int = 0,
        val get: () -> Double,
        val set: (Double) -> Unit
    )

    // Asks for location permission; whatever the answer, try to show daylight afterwards.
    private val locationPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { loadDaylight() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        daylightHelper = DaylightHelper(this)
        prefs = AppPrefs(this)

        // After a rotation, carry on from the same step with the same answers.
        val firstCreate = savedInstanceState == null
        savedInstanceState?.let { restoreAnswers(it) }

        setUpListeners()
        render()
        lifecycleScope.launch { loadEntry(firstCreate) }
    }

    override fun onResume() {
        super.onResume()
        // Coming back from Settings: location may have been switched on in the meantime.
        if (loaded && step == STEP_OUTDOOR) refreshDaylight(mayAsk = false)
    }

    override fun onDestroy() {
        daylightHelper.stop()
        super.onDestroy()
    }

    // ------------------------------------------------------------------ set-up

    private fun setUpListeners(): Unit = with(binding) {
        buttonClose.setOnClickListener { finish() } // Discards the answers.
        buttonBack.setOnClickListener { goTo(step - 1) }
        buttonNext.setOnClickListener { if (step == STEP_NOTE) onSaveClicked() else goTo(step + 1) }
        // The phone's Back button goes one question back; on the first question it leaves.
        onBackPressedDispatcher.addCallback(this@LogEntryActivity) {
            if (step > STEP_MOOD) goTo(step - 1) else finish()
        }

        scaleViews().forEachIndexed { index, view ->
            view.setOnClickListener {
                setScale(index + 1)
                render()
            }
        }

        buttonMinus.setOnClickListener { changeStepper(-1) }
        buttonPlus.setOnClickListener { changeStepper(+1) }

        outdoorChips().forEachIndexed { index, chip ->
            chip.setOnClickListener {
                outdoorMinutes = LogChoices.OUTDOOR_MINUTES[index]
                render()
            }
        }
        buttonLocationSettings.setOnClickListener {
            startActivity(Intent(this@LogEntryActivity, SettingsActivity::class.java))
        }

        chipVitaminD.setOnClickListener { tookVitaminD = !tookVitaminD; render() }
        chipB12.setOnClickListener { tookB12 = !tookB12; render() }
        chipIron.setOnClickListener { tookIron = !tookIron; render() }

        editNote.doAfterTextChanged { showNoteCount() }
    }

    private fun scaleViews(): List<TextView> =
        with(binding) { listOf(scale1, scale2, scale3, scale4, scale5) }

    private fun outdoorChips(): List<TextView> =
        with(binding) { listOf(chipOutdoor0, chipOutdoor1, chipOutdoor2, chipOutdoor3) }

    /**
     * Finds the entry to edit: the one named in the Intent, or today's entry if it already exists
     * (so the same date is never logged twice). Otherwise the answers stay at their defaults.
     */
    private suspend fun loadEntry(firstCreate: Boolean) {
        val id = intent.getLongExtra(EXTRA_LOG_ID, 0L)
        existing = if (id != 0L) viewModel.getLog(id) else viewModel.getLogByDate(logDate)

        existing?.let { log ->
            logDate = log.logDate
            if (daylightHours == null) daylightHours = log.daylightHours
            if (firstCreate) fillAnswers(log)
        }
        loaded = true
        render()
        if (step == STEP_OUTDOOR) refreshDaylight(mayAsk = firstCreate)
    }

    private fun fillAnswers(log: DailyLog) {
        mood = log.mood
        energy = log.energy
        anxiety = log.anxiety
        sleepHours = log.sleepHours
        caffeineCups = log.caffeineCups
        exerciseMinutes = log.exerciseMinutes
        outdoorMinutes = log.outdoorMinutes
        mealsEaten = log.mealsEaten
        val taken = log.supplements.orEmpty()
        tookVitaminD = taken.contains("Vitamin D")
        tookB12 = taken.contains("B12")
        tookIron = taken.contains("Iron")
        binding.editNote.setText(log.note.orEmpty())
    }

    // ------------------------------------------------------------------ showing a step

    private fun goTo(newStep: Int) {
        step = newStep.coerceIn(STEP_MOOD, STEP_NOTE)
        binding.textError.isVisible = false
        render()
        // Ask for the location only when its question is reached, so the reason is on screen.
        if (step == STEP_OUTDOOR) refreshDaylight(mayAsk = true)
    }

    /** Redraws the whole screen from the current step and answers. */
    private fun render(): Unit = with(binding) {
        progressSteps.progress = step + 1
        textStep.text = getString(R.string.step_count, step + 1, STEP_COUNT)
        textQuestion.setText(QUESTIONS[step])
        imageMascot.setImageResource(MASCOTS[step])

        val stepper = stepperFor(step)
        panelScale.isVisible = step <= STEP_ANXIETY
        panelStepper.isVisible = stepper != null
        panelSupplements.isVisible = step == STEP_FOOD
        panelOutdoor.isVisible = step == STEP_OUTDOOR
        panelNote.isVisible = step == STEP_NOTE

        if (step <= STEP_ANXIETY) renderScale()
        if (stepper != null) renderStepper(stepper)
        if (step == STEP_OUTDOOR) {
            val chosen = if (outdoorMinutes == NOT_ANSWERED) -1 else LogChoices.outdoorIndex(outdoorMinutes)
            outdoorChips().forEachIndexed { index, chip -> chip.isSelected = index == chosen }
        }
        if (step == STEP_FOOD) {
            chipVitaminD.isSelected = tookVitaminD
            chipB12.isSelected = tookB12
            chipIron.isSelected = tookIron
        }
        if (step == STEP_NOTE) showNoteCount()

        buttonBack.isVisible = step > STEP_MOOD
        buttonNext.setText(if (step == STEP_NOTE) R.string.button_save else R.string.button_next)
        // A question with choices has to be answered before moving on.
        val answered = isAnswered()
        buttonNext.isEnabled = answered
        buttonNext.alpha = if (answered) 1f else 0.4f
    }

    private fun isAnswered(): Boolean = when (step) {
        STEP_MOOD -> mood != 0
        STEP_ENERGY -> energy != 0
        STEP_ANXIETY -> anxiety != 0
        STEP_OUTDOOR -> outdoorMinutes != NOT_ANSWERED
        else -> true
    }

    // ---- 1 to 5 scales (mood, energy, anxiety) ----

    private fun currentScale(): Int = when (step) {
        STEP_MOOD -> mood
        STEP_ENERGY -> energy
        else -> anxiety
    }

    private fun setScale(value: Int) {
        when (step) {
            STEP_MOOD -> mood = value
            STEP_ENERGY -> energy = value
            else -> anxiety = value
        }
    }

    private fun renderScale(): Unit = with(binding) {
        val value = currentScale()
        scaleViews().forEachIndexed { index, view -> view.isSelected = index + 1 == value }
        val (lowId, highId, wordsId) = when (step) {
            STEP_MOOD -> Triple(R.string.scale_mood_low, R.string.scale_mood_high, R.array.mood_words)
            STEP_ENERGY -> Triple(R.string.scale_energy_low, R.string.scale_energy_high, R.array.energy_words)
            else -> Triple(R.string.scale_anxiety_low, R.string.scale_anxiety_high, R.array.anxiety_words)
        }
        textScaleLow.setText(lowId)
        textScaleHigh.setText(highId)
        // The chosen number in words, for example 4 on the mood scale is "Pretty good".
        textScaleValue.text = if (value == 0) "" else resources.getStringArray(wordsId)[value - 1]
    }

    // ---- Number questions (sleep, caffeine, movement, meals) ----

    private fun stepperFor(step: Int): Stepper? = when (step) {
        STEP_SLEEP -> Stepper(
            R.string.unit_hours, stepSize = 0.5, max = 24.0,
            shortcuts = listOf(5, 6, 7, 8, 9), shortcutLabelId = R.string.quick_hours,
            get = { sleepHours }, set = { sleepHours = it }
        )
        STEP_CAFFEINE -> Stepper(
            R.string.unit_cups, stepSize = 1.0, max = 20.0, hintId = R.string.hint_caffeine,
            get = { caffeineCups.toDouble() }, set = { caffeineCups = it.toInt() }
        )
        STEP_EXERCISE -> Stepper(
            R.string.unit_minutes, stepSize = 5.0, max = 1440.0,
            shortcuts = listOf(15, 30, 45, 60), shortcutLabelId = R.string.quick_minutes,
            get = { exerciseMinutes.toDouble() }, set = { exerciseMinutes = it.toInt() }
        )
        STEP_FOOD -> Stepper(
            R.string.unit_meals, stepSize = 1.0, max = 10.0,
            get = { mealsEaten.toDouble() }, set = { mealsEaten = it.toInt() }
        )
        else -> null
    }

    /** Minus and plus stop at the same limits LogValidator checks, so the number is always valid. */
    private fun changeStepper(direction: Int) {
        val stepper = stepperFor(step) ?: return
        stepper.set(LogChoices.stepValue(stepper.get(), stepper.stepSize, direction, 0.0, stepper.max))
        render()
    }

    private fun renderStepper(stepper: Stepper): Unit = with(binding) {
        val value = stepper.get()
        textStepperValue.text = Format.number(value)
        textStepperUnit.setText(stepper.unitId)

        quickChips.removeAllViews()
        quickChips.isVisible = stepper.shortcuts.isNotEmpty()
        for (shortcut in stepper.shortcuts) {
            val chip = Chips.add(quickChips, getString(stepper.shortcutLabelId, shortcut))
            chip.isSelected = value == shortcut.toDouble()
            chip.setOnClickListener {
                stepper.set(shortcut.toDouble())
                render()
            }
        }

        textStepperHint.isVisible = stepper.hintId != 0
        if (stepper.hintId != 0) textStepperHint.setText(stepper.hintId)
    }

    // ---- Daylight card on the outdoors question ----

    private fun refreshDaylight(mayAsk: Boolean) {
        when {
            daylightHours != null -> showDaylight() // Already known, or stored with the entry.
            !prefs.useLocation -> showDaylight(off = true)
            daylightHelper.hasPermission() -> loadDaylight()
            mayAsk && !askedForLocation -> {
                askedForLocation = true
                locationPermission.launch(
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                )
            }
            else -> showDaylight() // Permission denied: the entry can still be saved without daylight.
        }
    }

    /** Asks DaylightHelper for today's daylight; the answer may arrive a moment later. */
    private fun loadDaylight() {
        if (!daylightHelper.hasPermission()) {
            showDaylight()
            return
        }
        binding.textDaylight.setText(R.string.daylight_searching)
        binding.textDaylightNote.setText(R.string.daylight_value_note)
        binding.buttonLocationSettings.isVisible = false
        daylightHelper.requestDaylightHours(LocalDate.parse(logDate)) { hours ->
            daylightHours = hours
            showDaylight()
        }
    }

    /** The three end states: a value, "not available", or location switched off in Settings. */
    private fun showDaylight(off: Boolean = false) = with(binding) {
        val hours = daylightHours
        buttonLocationSettings.isVisible = off
        if (hours != null) {
            textDaylight.text = getString(R.string.daylight_value, Format.hoursAndMinutes(hours))
            textDaylightNote.setText(R.string.daylight_value_note)
        } else {
            textDaylight.setText(R.string.daylight_unavailable)
            textDaylightNote.setText(if (off) R.string.daylight_off_note else R.string.daylight_unavailable_note)
        }
    }

    // ---- Note ----

    /** "49/300" under the box, turning red (with a red outline) once the note is too long. */
    private fun showNoteCount(): Unit = with(binding) {
        val length = editNote.text.toString().trim().length
        val tooLong = length > LogValidator.MAX_NOTE_LENGTH
        textNoteCount.text = getString(R.string.note_count, length, LogValidator.MAX_NOTE_LENGTH)
        textNoteCount.setTextColor(getColor(if (tooLong) R.color.status_error else R.color.text_secondary))
        editNote.setBackgroundResource(if (tooLong) R.drawable.bg_field_error else R.drawable.bg_field)
        if (!tooLong) textError.isVisible = false
    }

    // ------------------------------------------------------------------ saving

    /** Builds a DailyLog from the answers. */
    private fun buildLog(): DailyLog {
        val supplements = listOfNotNull(
            "Vitamin D".takeIf { tookVitaminD },
            "Vitamin B12".takeIf { tookB12 },
            "Iron".takeIf { tookIron }
        ).joinToString(", ")

        return DailyLog(
            logId = existing?.logId ?: 0,
            logDate = logDate,
            mood = mood,
            energy = energy,
            anxiety = anxiety,
            sleepHours = sleepHours,
            caffeineCups = caffeineCups,
            exerciseMinutes = exerciseMinutes,
            outdoorMinutes = outdoorMinutes,
            mealsEaten = mealsEaten,
            supplements = supplements.ifEmpty { null },
            daylightHours = daylightHours,
            note = binding.editNote.text.toString().trim().ifEmpty { null },
            createdAt = existing?.createdAt ?: System.currentTimeMillis()
        )
    }

    private fun onSaveClicked() {
        val log = buildLog()
        binding.buttonNext.isEnabled = false // Prevents a double tap saving twice.
        lifecycleScope.launch {
            val result = viewModel.saveLog(log)
            binding.buttonNext.isEnabled = true

            val savedId = result.savedLogId
            if (savedId == null) {
                // Invalid input: stay on this screen with the answers still in place.
                showErrors(result)
            } else if (intent.getBooleanExtra(EXTRA_FROM_INSIGHTS, false)) {
                // Editing from Insights: go back there and let it show "Entry updated".
                setResult(Activity.RESULT_OK)
                finish()
            } else {
                startActivity(InsightsActivity.intent(this@LogEntryActivity, savedId, justSaved = true))
                finish() // Back from Insights should not return to the finished questions.
            }
        }
    }

    /**
     * Error state. The buttons and chips cannot produce an out-of-range number, so in practice
     * the only message is the note being too long; anything else is listed as a fallback.
     */
    private fun showErrors(result: ValidationResult) = with(binding) {
        textError.text =
            if (result.errors.keys == setOf(LogValidator.FIELD_NOTE)) {
                getString(R.string.message_note_too_long, LogValidator.MAX_NOTE_LENGTH)
            } else {
                getString(R.string.message_fix_errors, result.errors.values.joinToString(", "))
            }
        textError.isVisible = true
    }

    // ------------------------------------------------------------------ rotation

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_STEP, step)
        outState.putIntArray(
            KEY_NUMBERS,
            intArrayOf(mood, energy, anxiety, caffeineCups, exerciseMinutes, outdoorMinutes, mealsEaten)
        )
        outState.putDouble(KEY_SLEEP, sleepHours)
        outState.putBooleanArray(KEY_SUPPLEMENTS, booleanArrayOf(tookVitaminD, tookB12, tookIron))
        outState.putBoolean(KEY_ASKED_LOCATION, askedForLocation)
        daylightHours?.let { outState.putDouble(KEY_DAYLIGHT, it) }
    }

    private fun restoreAnswers(state: Bundle) {
        step = state.getInt(KEY_STEP)
        state.getIntArray(KEY_NUMBERS)?.let { numbers ->
            mood = numbers[0]
            energy = numbers[1]
            anxiety = numbers[2]
            caffeineCups = numbers[3]
            exerciseMinutes = numbers[4]
            outdoorMinutes = numbers[5]
            mealsEaten = numbers[6]
        }
        sleepHours = state.getDouble(KEY_SLEEP, sleepHours)
        state.getBooleanArray(KEY_SUPPLEMENTS)?.let { taken ->
            tookVitaminD = taken[0]
            tookB12 = taken[1]
            tookIron = taken[2]
        }
        askedForLocation = state.getBoolean(KEY_ASKED_LOCATION)
        if (state.containsKey(KEY_DAYLIGHT)) daylightHours = state.getDouble(KEY_DAYLIGHT)
    }

    companion object {
        /** Intent extra: the logId to edit. Absent when creating today's entry. */
        const val EXTRA_LOG_ID = "extra_log_id"
        /** Intent extra: true when opened by the Edit button on the Insights screen. */
        const val EXTRA_FROM_INSIGHTS = "extra_from_insights"

        private const val STEP_MOOD = 0
        private const val STEP_ENERGY = 1
        private const val STEP_ANXIETY = 2
        private const val STEP_SLEEP = 3
        private const val STEP_CAFFEINE = 4
        private const val STEP_EXERCISE = 5
        private const val STEP_OUTDOOR = 6
        private const val STEP_FOOD = 7
        private const val STEP_NOTE = 8
        private const val STEP_COUNT = 9
        private const val NOT_ANSWERED = -1

        /** The question for each step, and the character that asks it. */
        private val QUESTIONS = intArrayOf(
            R.string.question_mood, R.string.question_energy, R.string.question_anxiety,
            R.string.question_sleep, R.string.question_caffeine, R.string.question_exercise,
            R.string.question_outdoor, R.string.question_food, R.string.question_note
        )
        private val MASCOTS = intArrayOf(
            R.drawable.ic_sera, R.drawable.ic_thyra, R.drawable.ic_nora,
            R.drawable.ic_addy, R.drawable.ic_corty, R.drawable.ic_dopa,
            R.drawable.ic_sunny, R.drawable.ic_ferro, R.drawable.ic_sera
        )

        private const val KEY_STEP = "step"
        private const val KEY_NUMBERS = "numbers"
        private const val KEY_SLEEP = "sleep"
        private const val KEY_SUPPLEMENTS = "supplements"
        private const val KEY_ASKED_LOCATION = "asked_location"
        private const val KEY_DAYLIGHT = "daylight"
    }
}
