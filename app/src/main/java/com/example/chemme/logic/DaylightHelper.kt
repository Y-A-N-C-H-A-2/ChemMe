/**
 * Uses the device location (GPS or network) to work out how many hours of daylight a date has.
 * Based on the LocationManager approach from the MSD GPS lab.
 */
package com.example.chemme.logic

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Only the latitude is used, and only for the calculation: the location itself is never stored.
 * Nothing is sent over the internet; the sunrise maths runs on the phone.
 */
class DaylightHelper(private val context: Context) {

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var activeListener: LocationListener? = null

    /** True when the user has granted approximate or precise location. */
    fun hasPermission(): Boolean =
        isGranted(Manifest.permission.ACCESS_COARSE_LOCATION) || isGranted(Manifest.permission.ACCESS_FINE_LOCATION)

    private fun isGranted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /** Quick answer from the most recent location the phone already knows; null if there is none. */
    fun getDaylightHours(date: LocalDate): Double? =
        lastKnownLocation()?.let { daylightHoursAt(it.latitude, date) }

    /**
     * Calls onResult once: straight away when a recent location exists, otherwise after the first
     * location update arrives. Gives null when permission is missing or no provider is switched on.
     */
    @SuppressLint("MissingPermission") // hasPermission() is checked first.
    fun requestDaylightHours(date: LocalDate, onResult: (Double?) -> Unit) {
        if (!hasPermission()) {
            onResult(null)
            return
        }
        val known = getDaylightHours(date)
        if (known != null) {
            onResult(known)
            return
        }
        val provider = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .firstOrNull { name -> runCatching { locationManager.isProviderEnabled(name) }.getOrDefault(false) }
        if (provider == null) {
            onResult(null)
            return
        }

        stop() // Never keep two listeners registered.
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                stop() // One fix is enough; stop using the GPS to save battery.
                onResult(daylightHoursAt(location.latitude, date))
            }

            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }
        activeListener = listener
        try {
            locationManager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
        } catch (e: SecurityException) {
            // Precise location was refused and the GPS provider needs it.
            activeListener = null
            onResult(null)
        }
    }

    /** Stops waiting for a location. Call from the activity's onDestroy. */
    fun stop() {
        activeListener?.let { locationManager.removeUpdates(it) }
        activeListener = null
    }

    @SuppressLint("MissingPermission") // hasPermission() is checked first.
    private fun lastKnownLocation(): Location? {
        if (!hasPermission()) return null
        return try {
            locationManager.getProviders(true)
                .mapNotNull { provider -> locationManager.getLastKnownLocation(provider) }
                .maxByOrNull { it.time } // The newest fix from any provider.
        } catch (e: SecurityException) {
            null
        }
    }

    companion object {
        /**
         * Hours between sunrise and sunset for a latitude (in degrees) on a date.
         * Uses the standard sunrise equation: the sun's declination for the day of the year
         * gives the hour angle of sunrise, and the day is twice that angle at 15 degrees per hour.
         * Accurate to within a few minutes, which is plenty for "short day or long day".
         */
        fun daylightHoursAt(latitude: Double, date: LocalDate): Double {
            val toRadians = PI / 180.0
            val declination = 23.44 * sin(2 * PI * (284 + date.dayOfYear) / 365.0)
            val cosHourAngle = -tan(latitude * toRadians) * tan(declination * toRadians)
            // Outside -1..1 the sun never sets (24 h) or never rises (0 h), as near the poles.
            val hourAngle = acos(cosHourAngle.coerceIn(-1.0, 1.0)) / toRadians
            return 2 * hourAngle / 15.0
        }
    }
}
