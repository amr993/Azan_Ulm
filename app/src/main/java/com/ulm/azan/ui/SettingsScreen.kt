package com.ulm.azan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ulm.azan.data.Prayer
import com.ulm.azan.data.PrayerStore
import com.ulm.azan.data.Settings
import com.ulm.azan.util.AppPermissions

@Composable
fun SettingsScreen(
    store: PrayerStore,
    onBack: () -> Unit,
    onChanged: () -> Unit,
    onTestAzan: (Prayer) -> Unit,
    onRequestNotifications: () -> Unit,
) {
    val context = LocalContext.current
    val settings = remember { Settings(context) }

    var enabled by remember { mutableStateOf(settings.enabled) }
    val prayerStates = remember {
        Prayer.azanPrayers.associateWith { mutableStateOf(settings.isPrayerEnabled(it)) }
    }

    val canExact = AppPermissions.canScheduleExactAlarms(context)
    val ignoringBattery = AppPermissions.isIgnoringBatteryOptimizations(context)
    val range = store.dateRange()

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Surface(color = MaterialTheme.colorScheme.primary) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Settings",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onBack) {
                        Text("Back", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }

            Column(Modifier.padding(16.dp)) {

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        ToggleRow(
                            label = "Azan enabled",
                            checked = enabled
                        ) {
                            enabled = it
                            settings.enabled = it
                            onChanged()
                        }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Text(
                            "Play azan for",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        for (p in Prayer.azanPrayers) {
                            val state = prayerStates.getValue(p)
                            ToggleRow(
                                label = "${p.displayName} (${p.german})",
                                checked = state.value && enabled
                            ) {
                                state.value = it
                                settings.setPrayerEnabled(p, it)
                                onChanged()
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Reliability / permissions
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Reliability",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        StatusLine("Exact alarms", canExact)
                        AppPermissions.exactAlarmSettingsIntent(context)?.let { intent ->
                            if (!canExact) {
                                OutlinedButton(
                                    onClick = { context.startActivity(intent) },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Allow exact alarms") }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        StatusLine("Ignoring battery optimization", ignoringBattery)
                        if (!ignoringBattery) {
                            OutlinedButton(
                                onClick = { context.startActivity(AppPermissions.batteryOptimizationIntent(context)) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Exempt from battery optimization") }
                        }
                        if (AppPermissions.needsNotificationPermission()) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onRequestNotifications,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Allow notifications") }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Test",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { onTestAzan(Prayer.DHUHR) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Play standard azan") }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { onTestAzan(Prayer.FAJR) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Play Fajr azan") }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Data",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Stored days: ${store.count()}" +
                                (range?.let { "\nFrom ${it.first} to ${it.second}" } ?: ""),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "To add next month, go back and scan the new sheet. " +
                                "Replace app/src/main/res/raw/azan.mp3 and azan_fajr.mp3 " +
                                "with your own recordings for the real call to prayer.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun StatusLine(label: String, ok: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            if (ok) "On" else "Off",
            color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold
        )
    }
}
