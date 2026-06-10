package com.ulm.azan.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ulm.azan.data.DayTimes
import com.ulm.azan.data.Prayer
import com.ulm.azan.ocr.ParsedRow
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val HM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val ROW_DATE: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)
private val TIME_RX = Regex("""^([01]?\d|2[0-3]):[0-5]\d$""")

private class EditRow(
    val date: LocalDate,
    val complete: Boolean,
    val values: SnapshotStateMap<Prayer, String>
)

@Composable
fun ReviewScreen(
    rows: List<ParsedRow>,
    imageUri: Uri?,
    onCancel: () -> Unit,
    onSave: (List<DayTimes>) -> Unit,
) {
    val context = LocalContext.current

    val editRows = remember(rows) {
        rows.map { r ->
            val m = mutableStateMapOf<Prayer, String>()
            for (p in Prayer.columns) m[p] = r.times[p]?.format(HM) ?: ""
            EditRow(r.date, r.complete, m)
        }.toMutableStateList()
    }

    var error by remember { mutableStateOf<String?>(null) }

    var preview by remember(imageUri) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(imageUri) {
        preview = imageUri?.let { uri ->
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                    BitmapFactory.decodeStream(stream, null, opts)?.asImageBitmap()
                }
            } catch (_: Exception) { null }
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            BrandHeader(
                title = "Review times",
                arabic = "مراجعة الأوقات",
                subtitle = "Check each day, fix anything, then Save. Empty fields are skipped."
            )

            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                item { Spacer(Modifier.height(12.dp)) }

                preview?.let { bmp ->
                    item {
                        Image(
                            bitmap = bmp,
                            contentDescription = "Scanned sheet",
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }

                error?.let {
                    item {
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                it,
                                Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }

                if (editRows.isEmpty()) {
                    item {
                        Text(
                            "No prayer times were detected. Go back and try a clearer, " +
                                "straight-on photo of the table.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }

                items(editRows, key = { it.date.toString() }) { row ->
                    DayCard(row)
                    Spacer(Modifier.height(10.dp))
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val result = buildDays(editRows)
                            if (result == null) {
                                error = "Some times are not in HH:mm format (00:00–23:59). Please fix the highlighted values."
                            } else if (result.isEmpty()) {
                                error = "Nothing to save."
                            } else {
                                onSave(result)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            "Save ${editRows.size} ${if (editRows.size == 1) "day" else "days"} " +
                                "& schedule azan"
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Cancel") }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun DayCard(row: EditRow) {
    val container =
        if (row.complete) MaterialTheme.colorScheme.surface
        else MaterialTheme.colorScheme.secondaryContainer
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                StarBullet(10)
                Spacer(Modifier.height(0.dp))
                Text(
                    "  " + row.date.format(ROW_DATE) + if (!row.complete) "  · check this row" else "",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(Modifier.height(8.dp))
            val cols = Prayer.columns
            var i = 0
            while (i < cols.size) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeField(row, cols[i], Modifier.weight(1f))
                    if (i + 1 < cols.size) {
                        TimeField(row, cols[i + 1], Modifier.weight(1f))
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(6.dp))
                i += 2
            }
        }
    }
}

@Composable
private fun TimeField(row: EditRow, p: Prayer, modifier: Modifier) {
    val value = row.values[p] ?: ""
    val isBad = value.isNotBlank() && !TIME_RX.matches(value.trim())
    OutlinedTextField(
        value = value,
        onValueChange = { row.values[p] = it },
        label = { Text(if (p == Prayer.SUNRISE) "Sunrise" else p.displayName) },
        singleLine = true,
        isError = isBad,
        modifier = modifier
    )
}

private fun buildDays(editRows: List<EditRow>): List<DayTimes>? {
    val out = ArrayList<DayTimes>()
    for (row in editRows) {
        val m = LinkedHashMap<Prayer, LocalTime>()
        for (p in Prayer.columns) {
            val raw = (row.values[p] ?: "").trim()
            if (raw.isEmpty()) continue
            if (!TIME_RX.matches(raw)) return null
            m[p] = LocalTime.parse(normalize(raw), HM)
        }
        if (m.isNotEmpty()) out.add(DayTimes(row.date, m))
    }
    return out
}

private fun normalize(s: String): String {
    val parts = s.split(":")
    return "%02d:%02d".format(parts[0].toInt(), parts[1].toInt())
}
