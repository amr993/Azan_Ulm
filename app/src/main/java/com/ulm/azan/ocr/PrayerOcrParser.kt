package com.ulm.azan.ocr

import com.google.mlkit.vision.text.Text
import com.ulm.azan.data.Prayer
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/** One parsed table row, ready for the review screen. */
data class ParsedRow(
    val date: LocalDate,
    val times: Map<Prayer, LocalTime>,
    val complete: Boolean // true when all 6 columns were detected
)

/**
 * Turns an ML Kit OCR result into a list of [ParsedRow].
 *
 * Strategy (robust to most layouts):
 *  1. Flatten OCR into lines, each with a bounding box and its word elements.
 *  2. Cluster lines into table rows by vertical position.
 *  3. In each row, find the date (dd.MM.yyyy) and every HH:mm token.
 *  4. Sort the times left-to-right and map them to the printed column order:
 *     Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha.
 */
object PrayerOcrParser {

    private val DATE = Regex("""(\d{2})\.(\d{2})\.(\d{4})""")
    private val TIME_EXACT = Regex("""^\D*?(\d{1,2}):(\d{2})\D*$""")
    private val TIME_ANY = Regex("""(\d{1,2}):(\d{2})""")
    private val HM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    // ---- Internal, framework-independent geometry types (kept testable) ----
    internal data class Box(val l: Int, val t: Int, val r: Int, val b: Int) {
        val cx get() = (l + r) / 2
        val cy get() = (t + b) / 2
        val h get() = (b - t)
    }

    internal data class LineData(val text: String, val box: Box, val words: List<Pair<String, Box>>)

    /** Public entry point used with ML Kit. */
    fun parse(visionText: Text): List<ParsedRow> {
        val lines = ArrayList<LineData>()
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val bb = line.boundingBox ?: continue
                val words = ArrayList<Pair<String, Box>>()
                for (el in line.elements) {
                    val eb = el.boundingBox ?: continue
                    words.add(el.text to Box(eb.left, eb.top, eb.right, eb.bottom))
                }
                lines.add(LineData(line.text, Box(bb.left, bb.top, bb.right, bb.bottom), words))
            }
        }
        return parseLines(lines)
    }

    /** Core algorithm, independent of ML Kit so it can be unit-tested. */
    internal fun parseLines(lines: List<LineData>): List<ParsedRow> {
        if (lines.isEmpty()) return emptyList()

        val medianH = lines.map { it.box.h }.sorted().let { it[it.size / 2] }.coerceAtLeast(8)
        val threshold = (medianH * 0.7).toInt().coerceAtLeast(6)

        val sorted = lines.sortedBy { it.box.cy }
        val rows = ArrayList<MutableList<LineData>>()
        var current = mutableListOf<LineData>()
        var meanY = Int.MIN_VALUE
        for (ln in sorted) {
            if (current.isEmpty() || abs(ln.box.cy - meanY) <= threshold) {
                current.add(ln)
                meanY = current.map { it.box.cy }.average().toInt()
            } else {
                rows.add(current)
                current = mutableListOf(ln)
                meanY = ln.box.cy
            }
        }
        if (current.isNotEmpty()) rows.add(current)

        val byDate = LinkedHashMap<LocalDate, ParsedRow>()
        for (row in rows) {
            val date = findDate(row) ?: continue

            // Collect time tokens with x-position, preferring word-level boxes.
            val tokens = ArrayList<Pair<Int, String>>() // x -> "HH:mm"
            for (ln in row) {
                for ((text, box) in ln.words) {
                    val m = TIME_EXACT.find(text.trim()) ?: continue
                    val hh = m.groupValues[1].toInt()
                    val mm = m.groupValues[2].toInt()
                    if (hh in 0..23 && mm in 0..59) tokens.add(box.cx to "%02d:%02d".format(hh, mm))
                }
            }
            // Fallback when word boxes are unavailable: scan the line text.
            if (tokens.isEmpty()) {
                for (ln in row) {
                    for (m in TIME_ANY.findAll(ln.text)) {
                        val hh = m.groupValues[1].toInt()
                        val mm = m.groupValues[2].toInt()
                        if (hh in 0..23 && mm in 0..59) tokens.add(ln.box.cx to "%02d:%02d".format(hh, mm))
                    }
                }
            }

            tokens.sortBy { it.first }
            val map = LinkedHashMap<Prayer, LocalTime>()
            for (i in tokens.indices) {
                if (i >= Prayer.columns.size) break
                map[Prayer.columns[i]] = LocalTime.parse(tokens[i].second, HM)
            }
            val complete = map.size == Prayer.columns.size
            byDate[date] = ParsedRow(date, map, complete) // last wins on duplicates
        }
        return byDate.values.sortedBy { it.date }
    }

    private fun findDate(row: List<LineData>): LocalDate? {
        for (ln in row) {
            val m = DATE.find(ln.text) ?: continue
            return try {
                LocalDate.of(m.groupValues[3].toInt(), m.groupValues[2].toInt(), m.groupValues[1].toInt())
            } catch (_: Exception) {
                continue
            }
        }
        return null
    }
}
