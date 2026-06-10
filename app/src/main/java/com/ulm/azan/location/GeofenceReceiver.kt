package com.ulm.azan.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.ulm.azan.data.Settings

/**
 * Receives home-zone enter/exit events from the system and caches the result.
 * At prayer time the app reads this cached flag instead of touching location.
 */
class GeofenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_GEOFENCE) return
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_EXIT -> Settings(context).setAwayState(true)
            Geofence.GEOFENCE_TRANSITION_ENTER -> Settings(context).setAwayState(false)
        }
    }

    companion object {
        const val ACTION_GEOFENCE = "com.ulm.azan.action.GEOFENCE"
    }
}
