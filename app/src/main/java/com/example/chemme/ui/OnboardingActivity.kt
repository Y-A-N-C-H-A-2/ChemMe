/**
 * First-launch introduction: meet the characters, how the app works, and the two optional extras
 * (location for daylight, the daily reminder). Shown once; MainActivity checks AppPrefs.
 */
package com.example.chemme.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.chemme.R
import com.example.chemme.databinding.ActivityOnboardingBinding
import com.example.chemme.logic.AppPrefs
import com.example.chemme.logic.ReminderScheduler

class OnboardingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var prefs: AppPrefs
    private var page = 0

    // Android shows its own permission dialogs; whatever the answers, carry on to Home.
    private val permissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { openHome() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = AppPrefs(this)
        page = savedInstanceState?.getInt(KEY_PAGE) ?: 0

        binding.textReminderNote.text = getString(
            R.string.onboarding_reminder_note, Format.time(prefs.reminderHour, prefs.reminderMinute)
        )
        binding.buttonNext.setOnClickListener {
            if (page < LAST_PAGE) showPage(page + 1)
            else finishOnboarding(binding.switchLocation.isChecked, binding.switchReminder.isChecked)
        }
        // "Not now" turns both extras off; they can be switched on later in Settings.
        binding.buttonSkip.setOnClickListener { finishOnboarding(useLocation = false, reminder = false) }
        onBackPressedDispatcher.addCallback(this) {
            if (page > 0) showPage(page - 1) else finish()
        }
        showPage(page)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_PAGE, page)
    }

    private fun showPage(newPage: Int) = with(binding) {
        page = newPage
        page1.isVisible = page == 0
        page2.isVisible = page == 1
        page3.isVisible = page == LAST_PAGE
        buttonSkip.isVisible = page == LAST_PAGE
        buttonNext.setText(
            when (page) {
                0 -> R.string.onboarding_start
                1 -> R.string.onboarding_understood
                else -> R.string.onboarding_finish
            }
        )
        // The wide dot marks the current page.
        listOf(dot1, dot2, dot3).forEachIndexed { index, dot ->
            val current = index == page
            dot.setBackgroundResource(if (current) R.drawable.bg_dot_on else R.drawable.bg_dot_off)
            dot.layoutParams = dot.layoutParams.apply {
                width = ((if (current) 24 else 8) * resources.displayMetrics.density).toInt()
            }
        }
    }

    /** Stores the two choices, then asks Android only for the permissions those choices need. */
    private fun finishOnboarding(useLocation: Boolean, reminder: Boolean) {
        prefs.useLocation = useLocation
        prefs.reminderEnabled = reminder
        prefs.onboardingDone = true

        val scheduler = ReminderScheduler(this)
        if (reminder) scheduler.schedule(prefs.reminderHour, prefs.reminderMinute) else scheduler.cancel()

        val wanted = mutableListOf<String>()
        if (useLocation) {
            wanted += Manifest.permission.ACCESS_COARSE_LOCATION
            wanted += Manifest.permission.ACCESS_FINE_LOCATION
        }
        // Before Android 13 notifications need no permission.
        if (reminder && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            wanted += Manifest.permission.POST_NOTIFICATIONS
        }
        if (wanted.isEmpty()) openHome() else permissions.launch(wanted.toTypedArray())
    }

    private fun openHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private companion object {
        const val LAST_PAGE = 2
        const val KEY_PAGE = "page"
    }
}
