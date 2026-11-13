/**
 * Small helpers shared by the screens: the header and bottom navigation, text formatting,
 * character pictures and colours, chips and the confirm dialog.
 */
package com.example.chemme.ui

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.chemme.R
import com.example.chemme.databinding.DialogConfirmBinding
import com.example.chemme.databinding.ViewBottomNavBinding
import com.example.chemme.databinding.ViewTopBarBinding
import java.net.URI
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Base class for screens opened on top of a tab (Insights, Chemical Detail, Settings).
 * They show view_top_bar.xml, whose arrow behaves like the phone's Back button.
 */
open class BackActivity : AppCompatActivity() {
    protected fun setUpTopBar(topBar: ViewTopBarBinding, title: CharSequence) {
        topBar.textTitle.text = title
        topBar.buttonBack.setOnClickListener { finish() }
    }
}

/** The four destinations of the bottom navigation bar. */
enum class Tab { HOME, HISTORY, TRENDS, LIBRARY }

/** Wires up view_bottom_nav.xml. Each tab is its own Activity, opened with an explicit Intent. */
object BottomNav {
    fun setUp(activity: Activity, nav: ViewBottomNavBinding, current: Tab) {
        nav.navHome.isSelected = current == Tab.HOME
        nav.navHistory.isSelected = current == Tab.HISTORY
        nav.navTrends.isSelected = current == Tab.TRENDS
        nav.navLibrary.isSelected = current == Tab.LIBRARY

        nav.navHome.setOnClickListener { open(activity, current, Tab.HOME) }
        nav.navHistory.setOnClickListener { open(activity, current, Tab.HISTORY) }
        nav.navTrends.setOnClickListener { open(activity, current, Tab.TRENDS) }
        nav.navLibrary.setOnClickListener { open(activity, current, Tab.LIBRARY) }
        nav.navLog.setOnClickListener {
            activity.startActivity(Intent(activity, LogEntryActivity::class.java))
        }
    }

    /**
     * Home stays at the bottom of the back stack. Moving between the other tabs replaces the
     * current one, so Back always returns to Home instead of walking through every tab visited.
     */
    @Suppress("DEPRECATION") // overridePendingTransition: the replacement needs API 34.
    private fun open(activity: Activity, current: Tab, target: Tab) {
        if (target == current) return
        if (target != Tab.HOME) {
            val screen = when (target) {
                Tab.HISTORY -> HistoryActivity::class.java
                Tab.TRENDS -> TrendsActivity::class.java
                else -> LibraryActivity::class.java
            }
            activity.startActivity(Intent(activity, screen))
        }
        if (current != Tab.HOME) activity.finish()
        activity.overridePendingTransition(0, 0) // Tabs switch without a slide animation.
    }
}

/** Turns stored values into the text shown on screen, in one place so every screen matches. */
object Format {
    private val shortDate = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
    private val longDate = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH)
    private val dayName = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
    private val monthName = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)

    /** "2026-11-13" becomes "Fri 13 Nov". */
    fun date(isoDate: String): String = LocalDate.parse(isoDate).format(shortDate)

    /** "Friday 13 November", for the top of Home. */
    fun longDate(date: LocalDate): String = date.format(longDate)

    /** "FRI", "13" and "NOV": the three lines of the date block on a History row. */
    fun dayName(date: LocalDate): String = date.format(dayName).uppercase(Locale.ENGLISH)
    fun dayNumber(date: LocalDate): String = date.dayOfMonth.toString()
    fun monthName(date: LocalDate): String = date.format(monthName).uppercase(Locale.ENGLISH)

    /** 5.0 becomes "5" and 4.5 becomes "4.5". */
    fun number(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.ENGLISH, "%.1f", value)

    /** 5.0 becomes "5 h" and 4.5 becomes "4.5 h". */
    fun hours(value: Double): String = "${number(value)} h"

    /** 8.35 becomes "8 h 21 min". */
    fun hoursAndMinutes(value: Double): String {
        val totalMinutes = (value * 60).roundToInt()
        return "${totalMinutes / 60} h ${totalMinutes % 60} min"
    }

    /** 21 and 0 become "21:00". */
    fun time(hour: Int, minute: Int): String = String.format(Locale.ENGLISH, "%02d:%02d", hour, minute)

    /** A short name for the website behind a source link, shown as "Source: Cleveland Clinic". */
    fun sourceName(url: String): String {
        val host = try { URI(url).host.orEmpty() } catch (e: Exception) { "" }
        return when {
            host.endsWith("clevelandclinic.org") -> "Cleveland Clinic"
            host.endsWith("ninds.nih.gov") -> "NINDS (NIH)"
            host.endsWith("ods.od.nih.gov") -> "NIH Office of Dietary Supplements"
            host.endsWith("nih.gov") -> "NIH"
            else -> host.removePrefix("www.")
        }
    }
}

/** Maps the iconName stored in the Chemical table to that character's picture and colour. */
object ChemicalIcons {
    fun drawableFor(iconName: String): Int = when (iconName) {
        "ic_sera" -> R.drawable.ic_sera
        "ic_dopa" -> R.drawable.ic_dopa
        "ic_nora" -> R.drawable.ic_nora
        "ic_corty" -> R.drawable.ic_corty
        "ic_gabby" -> R.drawable.ic_gabby
        "ic_mel" -> R.drawable.ic_mel
        "ic_addy" -> R.drawable.ic_addy
        "ic_sunny" -> R.drawable.ic_sunny
        "ic_bea" -> R.drawable.ic_bea
        "ic_ferro" -> R.drawable.ic_ferro
        "ic_thyra" -> R.drawable.ic_thyra
        else -> R.drawable.ic_default // A chemical added to the CSV without its own character.
    }

    fun colorFor(iconName: String): Int = when (iconName) {
        "ic_sera" -> R.color.mascot_sera
        "ic_dopa" -> R.color.mascot_dopa
        "ic_nora" -> R.color.mascot_nora
        "ic_corty" -> R.color.mascot_corty
        "ic_gabby" -> R.color.mascot_gabby
        "ic_mel" -> R.color.mascot_mel
        "ic_addy" -> R.color.mascot_addy
        "ic_sunny" -> R.color.mascot_sunny
        "ic_bea" -> R.color.mascot_bea
        "ic_ferro" -> R.color.mascot_ferro
        "ic_thyra" -> R.color.mascot_thyra
        else -> R.color.mascot_default
    }
}

/** Creates chips from data (the layout only describes how one chip looks). */
object Chips {
    /** Adds one chip with this text to the end of parent and returns it. */
    fun add(parent: ViewGroup, text: CharSequence): TextView {
        val chip = LayoutInflater.from(parent.context).inflate(R.layout.view_chip, parent, false) as TextView
        chip.text = text
        parent.addView(chip)
        return chip
    }

    /** Replaces whatever chips parent has with one read-only chip per text. */
    fun show(parent: ViewGroup, texts: List<CharSequence>) {
        parent.removeAllViews()
        texts.forEach { add(parent, it) }
    }
}

/** The rounded "are you sure?" dialog used before deleting. Cancel changes nothing. */
object ConfirmDialog {
    fun show(activity: Activity, titleId: Int, messageId: Int, onConfirm: () -> Unit) {
        val binding = DialogConfirmBinding.inflate(activity.layoutInflater)
        val dialog = AlertDialog.Builder(activity, R.style.Theme_ChemMe_Dialog)
            .setView(binding.root)
            .create()
        binding.textTitle.setText(titleId)
        binding.textMessage.setText(messageId)
        binding.buttonCancel.setOnClickListener { dialog.dismiss() }
        binding.buttonConfirm.setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }
        dialog.show()
    }
}
