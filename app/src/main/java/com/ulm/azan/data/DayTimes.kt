package com.ulm.azan.data

import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Prayer times for a single calendar day. */
data class DayTimes(
    val date: LocalDate,
    val times: Map<Prayer, LocalTime>
) {
    fun time(p: Prayer): LocalTime? = times[p]

    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("date", date.toString()) // ISO yyyy-MM-dd
        for (p in Prayer.columns) {
            times[p]?.let { o.put(p.key, it.format(HM)) }
        }
        return o
    }

    companion object {
        val HM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        fun fromJson(o: JSONObject): DayTimes {
            val date = LocalDate.parse(o.getString("date"))
            val m = LinkedHashMap<Prayer, LocalTime>()
            for (p in Prayer.columns) {
                val s = o.optString(p.key, "")
                if (s.isNotBlank()) {
                    try {
                        m[p] = LocalTime.parse(s, HM)
                    } catch (_: Exception) { /* skip malformed */ }
                }
            }
            return DayTimes(date, m)
        }
    }
}
