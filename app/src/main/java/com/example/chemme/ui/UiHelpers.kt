/**
 * Small helpers shared by the screens: the back arrow, text formatting and character icons.
 */
package com.example.chemme.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.chemme.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Base class for every screen except Home. Shows the back arrow in the top bar and makes it
 * behave like the phone's Back button: return to the previous screen.
 */
open class BackActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

/** Turns stored values into the text shown on screen, in one place so every screen matches. */
object Format {
    private val dateFormatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH)

    /** "2026-11-13" becomes "Fri 13 Nov 2026". */
    fun date(isoDate: String): String = LocalDate.parse(isoDate).format(dateFormatter)

    /** 5.0 becomes "5 h" and 4.5 becomes "4.5 h". */
    fun hours(value: Double): String =
        if (value % 1.0 == 0.0) "${value.toInt()} h" else String.format(Locale.ENGLISH, "%.1f h", value)

    /** 8.35 becomes "8 h 21 min". */
    fun hoursAndMinutes(value: Double): String {
        val totalMinutes = (value * 60).roundToInt()
        return "${totalMinutes / 60} h ${totalMinutes % 60} min"
    }

    /** 21 and 0 become "21:00". */
    fun time(hour: Int, minute: Int): String = String.format(Locale.ENGLISH, "%02d:%02d", hour, minute)
}

/** Maps the iconName stored in the Chemical table to a drawable in res/drawable. */
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
        else -> R.drawable.ic_default // A chemical added to the CSV without its own icon.
    }
}
