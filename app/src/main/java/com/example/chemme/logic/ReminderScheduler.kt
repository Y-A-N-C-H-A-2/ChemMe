/**
 * Daily reminder: an alarm that fires once a day and a receiver that shows the notification.
 */
package com.example.chemme.logic

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.chemme.R
import com.example.chemme.ui.LogEntryActivity
import java.util.Calendar

/** Sets and cancels the repeating alarm. Called from the Settings screen. */
class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** Schedules a reminder every day at the given time, replacing any earlier one. */
    fun schedule(hour: Int, minute: Int) {
        val firstTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            // If that time has already passed today, start tomorrow.
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        // Inexact repeating alarms need no special permission; Android may shift them by a few minutes.
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP, firstTime.timeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent()
        )
    }

    fun cancel() {
        alarmManager.cancel(pendingIntent())
    }

    /** The same request code and intent every time, so schedule() replaces and cancel() finds it. */
    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context, REQUEST_CODE, Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private companion object {
        const val REQUEST_CODE = 1001
    }
}

/** Runs when the alarm fires, even if the app is closed, and posts the reminder notification. */
class ReminderReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission") // Checked just below for Android 13+.
    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return // The user has not allowed notifications; stay silent.

        // A channel is required on Android 8+. Creating one that already exists does nothing.
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT
            )
        )

        // Tapping the notification opens the Log Entry screen.
        val openLog = PendingIntent.getActivity(
            context, 0, Intent(context, LogEntryActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(context.getString(R.string.reminder_text))
            .setContentIntent(openLog)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private companion object {
        const val CHANNEL_ID = "daily_reminder"
        const val NOTIFICATION_ID = 1
    }
}
