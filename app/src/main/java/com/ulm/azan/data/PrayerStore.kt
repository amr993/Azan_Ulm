package com.ulm.azan.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

/**
 * Persists prayer times as a JSON file in internal storage.
 * Seeds itself from the bundled June 2026 asset on first launch.
 */
class PrayerStore(private val context: Context) {

    private val file = File(context.filesDir, FILE_NAME)

    /** Copy the bundled month into storage the first time the app runs. */
    fun ensureSeeded() {
        if (file.exists()) return
        try {
            context.assets.open(ASSET_NAME).use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
        } catch (_: Exception) {
            file.writeText(JSONObject().put("days", JSONArray()).toString())
        }
    }

    fun loadAll(): Map<LocalDate, DayTimes> {
        if (!file.exists()) return emptyMap()
        val map = LinkedHashMap<LocalDate, DayTimes>()
        try {
            val root = JSONObject(file.readText())
            val arr = root.optJSONArray("days") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val d = DayTimes.fromJson(arr.getJSONObject(i))
                map[d.date] = d
            }
        } catch (_: Exception) { /* corrupt file -> empty */ }
        return map.toSortedMap()
    }

    fun saveAll(days: Collection<DayTimes>) {
        val arr = JSONArray()
        days.sortedBy { it.date }.forEach { arr.put(it.toJson()) }
        file.writeText(JSONObject().put("days", arr).toString())
    }

    /** Merge new days into the store; same-date entries are overwritten. */
    fun merge(newDays: Collection<DayTimes>) {
        val all = loadAll().toMutableMap()
        newDays.forEach { all[it.date] = it }
        saveAll(all.values)
    }

    fun forDate(date: LocalDate): DayTimes? = loadAll()[date]

    fun dateRange(): Pair<LocalDate, LocalDate>? {
        val all = loadAll()
        if (all.isEmpty()) return null
        val keys = all.keys.sorted()
        return keys.first() to keys.last()
    }

    fun count(): Int = loadAll().size

    companion object {
        private const val FILE_NAME = "prayer_times.json"
        private const val ASSET_NAME = "prayer_times_june_2026.json"
    }
}
