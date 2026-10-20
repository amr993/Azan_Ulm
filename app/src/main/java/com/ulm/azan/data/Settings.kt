package com.ulm.azan.data

import android.content.Context

/** SharedPreferences-backed settings. */
class Settings(context: Context) {
    private val sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = sp.getBoolean(KEY_ENABLED, true)
        set(v) = sp.edit().putBoolean(KEY_ENABLED, v).apply()

    fun isPrayerEnabled(p: Prayer): Boolean = sp.getBoolean("p_${p.key}", true)
    fun setPrayerEnabled(p: Prayer, v: Boolean) = sp.edit().putBoolean("p_${p.key}", v).apply()

    /** When true, the azan is muted (notification only) while away from home. */
    var locationGateEnabled: Boolean
        get() = sp.getBoolean(KEY_LOC_GATE, false)
        set(v) = sp.edit().putBoolean(KEY_LOC_GATE, v).apply()

    val homeSet: Boolean
        get() = sp.contains(KEY_HOME_LAT) && sp.contains(KEY_HOME_LNG)

    fun setHome(lat: Double, lng: Double) {
        sp.edit()
            .putLong(KEY_HOME_LAT, java.lang.Double.doubleToRawLongBits(lat))
            .putLong(KEY_HOME_LNG, java.lang.Double.doubleToRawLongBits(lng))
            .apply()
    }

    fun homeLatLng(): Pair<Double, Double>? {
        if (!homeSet) return null
        val lat = java.lang.Double.longBitsToDouble(sp.getLong(KEY_HOME_LAT, 0L))
        val lng = java.lang.Double.longBitsToDouble(sp.getLong(KEY_HOME_LNG, 0L))
        return lat to lng
    }

    fun clearHome() = sp.edit().remove(KEY_HOME_LAT).remove(KEY_HOME_LNG).apply()

    /** True while a home geofence is registered with Play services. */
    var geofenceActive: Boolean
        get() = sp.getBoolean(KEY_GEOFENCE_ACTIVE, false)
        set(v) = sp.edit().putBoolean(KEY_GEOFENCE_ACTIVE, v).apply()

    /**
     * Last home-zone state reported by the geofence: true = away, false = at home,
     * null = no transition seen yet (treated as at-home so the azan still plays).
     */
    val awayState: Boolean?
        get() = when (sp.getInt(KEY_AWAY_STATE, AWAY_UNKNOWN)) {
            AWAY_YES -> true
            AWAY_NO -> false
            else -> null
        }

    fun setAwayState(away: Boolean) =
        sp.edit().putInt(KEY_AWAY_STATE, if (away) AWAY_YES else AWAY_NO).apply()

    fun clearAwayState() = sp.edit().remove(KEY_AWAY_STATE).apply()

    companion object {
        const val RADIUS_METERS = 1000f
        private const val KEY_ENABLED = "enabled"
        private const val KEY_LOC_GATE = "loc_gate"
        private const val KEY_HOME_LAT = "home_lat"
        private const val KEY_HOME_LNG = "home_lng"
        private const val KEY_GEOFENCE_ACTIVE = "geofence_active"
        private const val KEY_AWAY_STATE = "away_state"
        private const val AWAY_UNKNOWN = -1
        private const val AWAY_NO = 0
        private const val AWAY_YES = 1
    }
}
