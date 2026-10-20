package com.ulm.azan.alarm

import android.content.Context
import android.media.MediaMetadataRetriever
import java.time.LocalDate

/**
 * The du'a played just before the Fajr azan. There are seven recordings,
 * res/raw/fajr_dua_1 … fajr_dua_7, one per weekday (1 = Monday … 7 = Sunday),
 * so exactly one plays each day and the cycle repeats every week.
 *
 * It is started early enough to finish GAP_MS before Fajr, so the azan itself
 * still begins exactly on time.
 */
object FajrDua {
    const val GAP_MS = 2_000L
    private const val FALLBACK_DURATION_MS = 4 * 60 * 1000L

    fun resName(date: LocalDate): String = "fajr_dua_${date.dayOfWeek.value}"

    /** Raw resource id for that date's du'a, or 0 if it isn't bundled. */
    fun resId(context: Context, date: LocalDate): Int =
        context.resources.getIdentifier(resName(date), "raw", context.packageName)

    /** Length of the recording, read from the file itself (works offline). */
    fun durationMs(context: Context, resId: Int): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            context.resources.openRawResourceFd(resId).use { afd ->
                retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: FALLBACK_DURATION_MS
        } catch (_: Exception) {
            FALLBACK_DURATION_MS
        } finally {
            try { retriever.release() } catch (_: Exception) { }
        }
    }
}
