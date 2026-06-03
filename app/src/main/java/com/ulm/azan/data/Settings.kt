package com.ulm.azan.data

import android.content.Context

/** Simple SharedPreferences-backed settings. */
class Settings(context: Context) {
    private val sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = sp.getBoolean(KEY_ENABLED, true)
        set(v) = sp.edit().putBoolean(KEY_ENABLED, v).apply()

    fun isPrayerEnabled(p: Prayer): Boolean = sp.getBoolean("p_${p.key}", true)

    fun setPrayerEnabled(p: Prayer, v: Boolean) = sp.edit().putBoolean("p_${p.key}", v).apply()

    companion object {
        private const val KEY_ENABLED = "enabled"
    }
}
