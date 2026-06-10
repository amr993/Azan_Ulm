package com.ulm.azan.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.ulm.azan.data.Settings
import com.ulm.azan.util.AppPermissions

/**
 * Registers a single geofence around the saved home so the SYSTEM tracks
 * enter/exit instead of the app polling location at every prayer time.
 * The app only wakes when the home boundary is crossed, which keeps the
 * OS from flagging it for repeated background location access.
 */
object HomeGeofence {

    private const val FENCE_ID = "home"
    private const val REQUEST_CODE = 4242

    /** Five minutes of slack lets the system batch checks and save battery. */
    private const val RESPONSIVENESS_MS = 5 * 60 * 1000

    /**
     * Bring the geofence in line with the current settings: registered when the
     * away feature is on and a home is saved, removed otherwise. Safe to call
     * anytime (app open, boot, toggle, permission change).
     */
    fun sync(context: Context) {
        val settings = Settings(context)
        val home = settings.homeLatLng()
        if (settings.locationGateEnabled && home != null &&
            AppPermissions.hasBackgroundLocation(context)
        ) {
            register(context, settings, home.first, home.second)
        } else {
            unregister(context, settings)
        }
    }

    @SuppressLint("MissingPermission")
    private fun register(context: Context, settings: Settings, lat: Double, lng: Double) {
        val fence = Geofence.Builder()
            .setRequestId(FENCE_ID)
            .setCircularRegion(lat, lng, Settings.RADIUS_METERS)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
            )
            .setNotificationResponsiveness(RESPONSIVENESS_MS)
            .build()
        val request = GeofencingRequest.Builder()
            .setInitialTrigger(
                GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_EXIT
            )
            .addGeofence(fence)
            .build()
        try {
            LocationServices.getGeofencingClient(context)
                .addGeofences(request, pendingIntent(context))
                .addOnSuccessListener { settings.geofenceActive = true }
                .addOnFailureListener { settings.geofenceActive = false }
        } catch (_: Exception) {
            // No Play services on this device — LocationGate falls back to a
            // one-off last-known-location read at prayer time.
            settings.geofenceActive = false
        }
    }

    private fun unregister(context: Context, settings: Settings) {
        try {
            LocationServices.getGeofencingClient(context)
                .removeGeofences(pendingIntent(context))
        } catch (_: Exception) {
        }
        settings.geofenceActive = false
        settings.clearAwayState()
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceReceiver::class.java)
            .setAction(GeofenceReceiver.ACTION_GEOFENCE)
        // Must be mutable: the system attaches the transition details as extras.
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }
}
