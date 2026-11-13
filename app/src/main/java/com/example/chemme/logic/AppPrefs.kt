/**
 * Small settings stored with SharedPreferences (not Room, because they are single values, not rows).
 */
package com.example.chemme.logic

import android.content.Context

/** Reads and writes the choices made on the Settings screen. */
class AppPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("chemme_prefs", Context.MODE_PRIVATE)

    var reminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_REMINDER_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_REMINDER_ENABLED, value).apply()

    var reminderHour: Int
        get() = prefs.getInt(KEY_REMINDER_HOUR, 20)
        set(value) = prefs.edit().putInt(KEY_REMINDER_HOUR, value).apply()

    var reminderMinute: Int
        get() = prefs.getInt(KEY_REMINDER_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_REMINDER_MINUTE, value).apply()

    /** When false the app never asks for or reads the location. */
    var useLocation: Boolean
        get() = prefs.getBoolean(KEY_USE_LOCATION, true)
        set(value) = prefs.edit().putBoolean(KEY_USE_LOCATION, value).apply()

    /** False until the first-launch introduction has been finished. */
    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()

    private companion object {
        const val KEY_REMINDER_ENABLED = "reminder_enabled"
        const val KEY_REMINDER_HOUR = "reminder_hour"
        const val KEY_REMINDER_MINUTE = "reminder_minute"
        const val KEY_USE_LOCATION = "use_location"
        const val KEY_ONBOARDING_DONE = "onboarding_done"
    }
}
