package com.ulm.azan.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.ulm.azan.data.Settings
import com.ulm.azan.util.AppPermissions

/**
 * Decides whether the azan should play based on distance from the stored home.
 *
 * Preferred path: read the cached home-zone state maintained by [HomeGeofence] —
 * the system tracks enter/exit, so the app performs NO location access at prayer
 * time (and the OS never flags it for background location checks).
 * Fallback (no Play services): a single passive last-known-location read.
 *
 * Fails OPEN: if the feature is off, home is unset, permission is missing, or the
 * location is unknown, it reports not-away so the azan plays normally.
 */
object LocationGate {

    data class Result(val away: Boolean, val known: Boolean, val distanceMeters: Float?)

    fun evaluate(context: Context): Result {
        val settings = Settings(context)
        if (!settings.locationGateEnabled) return Result(false, true, null)
        val home = settings.homeLatLng() ?: return Result(false, false, null)
        if (!AppPermissions.hasLocationPermission(context)) return Result(false, false, null)
        if (settings.geofenceActive) {
            val away = settings.awayState
            return Result(away == true, away != null, null)
        }
        val loc = bestLastKnown(context) ?: return Result(false, false, null)
        val res = FloatArray(1)
        Location.distanceBetween(home.first, home.second, loc.latitude, loc.longitude, res)
        return Result(res[0] > Settings.RADIUS_METERS, true, res[0])
    }

    @SuppressLint("MissingPermission")
    private fun bestLastKnown(context: Context): Location? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        var best: Location? = null
        val providers = try { lm.getProviders(true) } catch (_: Exception) { emptyList<String>() }
        for (p in providers) {
            val l = try { lm.getLastKnownLocation(p) } catch (_: Exception) { null }
            if (l != null && (best == null || l.time > best!!.time)) best = l
        }
        return best
    }

    private fun pickProvider(lm: LocationManager): String {
        return when {
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> LocationManager.PASSIVE_PROVIDER
        }
    }

    /** Capture a fresh location once (used by the 'Set home' button while the app is open). */
    @SuppressLint("MissingPermission")
    fun captureCurrent(context: Context, onResult: (Double?, Double?) -> Unit) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (lm == null || !AppPermissions.hasLocationPermission(context)) {
            onResult(null, null); return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                lm.getCurrentLocation(
                    pickProvider(lm), null, ContextCompat.getMainExecutor(context)
                ) { loc ->
                    if (loc != null) onResult(loc.latitude, loc.longitude)
                    else bestLastKnown(context).let { onResult(it?.latitude, it?.longitude) }
                }
            } catch (_: Exception) {
                bestLastKnown(context).let { onResult(it?.latitude, it?.longitude) }
            }
        } else {
            bestLastKnown(context).let { onResult(it?.latitude, it?.longitude) }
        }
    }
}
