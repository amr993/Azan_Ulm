package com.ulm.azan.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ulm.azan.alarm.AzanPlaybackState
import com.ulm.azan.data.Prayer
import com.ulm.azan.data.PrayerStore
import com.ulm.azan.data.Settings
import com.ulm.azan.location.HomeGeofence
import com.ulm.azan.location.LocationGate
import com.ulm.azan.util.AppPermissions
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SHEET_DATE_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)

@Composable
fun SettingsScreen(
    store: PrayerStore,
    onBack: () -> Unit,
    onChanged: () -> Unit,
    onTestAzan: (Prayer) -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
) {
    val context = LocalContext.current
    val settings = remember { Settings(context) }
    val playing by AzanPlaybackState.playing.collectAsState()

    var enabled by remember { mutableStateOf(settings.enabled) }
    val prayerStates = remember {
        Prayer.azanPrayers.associateWith { mutableStateOf(settings.isPrayerEnabled(it)) }
    }

    var locVersion by remember { mutableIntStateOf(0) }
    var homeMsg by remember { mutableStateOf<String?>(null) }
    var gateEnabled by remember { mutableStateOf(settings.locationGateEnabled) }

    fun captureHome() {
        LocationGate.captureCurrent(context) { lat, lng ->
            homeMsg = if (lat != null && lng != null) {
                settings.setHome(lat, lng)
                HomeGeofence.sync(context)
                "Home location saved."
            } else {
                "Couldn't get a location fix — go outdoors and try again."
            }
            locVersion++
        }
    }

    val fineLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) captureHome()
        locVersion++
    }
    val bgLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        HomeGeofence.sync(context)
        locVersion++
    }

    val canExact = AppPermissions.canScheduleExactAlarms(context)
    val ignoringBattery = AppPermissions.isIgnoringBatteryOptimizations(context)
    val notificationsOn = AppPermissions.areNotificationsEnabled(context)
    val range = store.dateRange()
    val appVersion = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("UNUSED_EXPRESSION") locVersion
    val hasLocation = AppPermissions.hasLocationPermission(context)
    val hasBackground = AppPermissions.hasBackgroundLocation(context)
    val homeSet = settings.homeSet

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            BrandHeader(title = "Settings", arabic = "الإعدادات", onBack = onBack)

            Column(Modifier.padding(16.dp)) {

                SectionCard("Azan", "الأذان") {
                    ToggleRow("Azan enabled", enabled) {
                        enabled = it; settings.enabled = it; onChanged()
                    }
                    Spacer(Modifier.height(4.dp))
                    OrnamentalDivider()
                    Spacer(Modifier.height(8.dp))
                    Text("Play azan for", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    for (p in Prayer.azanPrayers) {
                        val state = prayerStates.getValue(p)
                        ToggleRow("${p.displayName} (${p.german})", state.value && enabled) {
                            state.value = it; settings.setPrayerEnabled(p, it); onChanged()
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                SectionCard("Away from home", "خارج المنزل") {
                    ToggleRow("Mute azan when away (> 1 km)", gateEnabled) {
                        gateEnabled = it; settings.locationGateEnabled = it
                        HomeGeofence.sync(context); locVersion++
                    }
                    Spacer(Modifier.height(6.dp))
                    StatusLine("Home location set", homeSet)
                    StatusLine("Location permission", hasLocation)
                    if (gateEnabled && AppPermissions.needsBackgroundLocation()) {
                        StatusLine("Allowed all the time", hasBackground)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            if (!hasLocation) {
                                fineLocationLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            } else captureHome()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(if (homeSet) "Update home to current location" else "Set home to current location") }

                    if (homeSet) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                settings.clearHome()
                                HomeGeofence.sync(context)
                                homeMsg = "Home cleared."; locVersion++
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Clear home") }
                    }

                    if (gateEnabled && AppPermissions.needsBackgroundLocation() && !hasBackground) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { bgLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Allow location all the time") }
                    }

                    homeMsg?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Within 1 km of home the azan plays normally. Farther away you get a silent " +
                            "reminder instead, and the azan resumes automatically when you return. " +
                            "Android itself watches the home zone (a battery-friendly geofence), so the " +
                            "app never tracks you — it is only told when you leave or come home, and " +
                            "nothing ever leaves your phone. \"Allow all the time\" is required by " +
                            "Android for the geofence to work.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(12.dp))

                SectionCard("Notifications", "الإشعارات") {
                    StatusLine("Notifications allowed", notificationsOn)
                    Spacer(Modifier.height(8.dp))
                    if (AppPermissions.needsNotificationPermission() && !notificationsOn) {
                        OutlinedButton(
                            onClick = onRequestNotifications,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Allow notifications") }
                        Spacer(Modifier.height(8.dp))
                    }
                    OutlinedButton(
                        onClick = onOpenNotificationSettings,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Notification settings") }
                }

                Spacer(Modifier.height(12.dp))

                SectionCard("Reliability & battery", "الموثوقية") {
                    StatusLine("Exact alarms", canExact)
                    AppPermissions.exactAlarmSettingsIntent(context)?.let { intent ->
                        if (!canExact) {
                            OutlinedButton(
                                onClick = { context.startActivity(intent) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Allow exact alarms") }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    StatusLine("Ignoring battery optimization", ignoringBattery)
                    if (!ignoringBattery) {
                        OutlinedButton(
                            onClick = { context.startActivity(AppPermissions.batteryOptimizationIntent(context)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Exempt from battery optimization") }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "The app does no background work between prayers — it only wakes for a few " +
                            "seconds at each prayer time, then sleeps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(12.dp))

                SectionCard("Azan sound", "صوت الأذان") {
                    Text(
                        "Listen to the azan exactly as it will play at prayer time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onTestAzan(Prayer.DHUHR) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(if (playing == Prayer.DHUHR.key) "Stop" else "Preview the azan") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onTestAzan(Prayer.FAJR) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(if (playing == Prayer.FAJR.key) "Stop" else "Preview the Fajr azan") }
                }

                Spacer(Modifier.height(12.dp))

                SectionCard("Prayer times", "مواقيت الصلاة") {
                    Text(
                        range?.let {
                            "Times for ${store.count()} days are saved, " +
                                "until ${it.second.format(SHEET_DATE_FMT)}."
                        } ?: "No prayer times saved yet — scan this month's timetable.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Past months are removed automatically. When the mosque posts a new " +
                            "timetable, scan it from the home screen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(12.dp))

                SectionCard("About", "حول التطبيق") {
                    Text(
                        "Azan Ulm${appVersion?.let { " · version $it" } ?: ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Prayer times of the Al-Salam Mosque (Friedensmoschee) in Ulm. " +
                            "The app works completely offline — it has no internet access and " +
                            "your data never leaves the phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, arabic: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(arabic, style = MaterialTheme.typography.titleSmall, color = Brand.Gold)
            }
            Spacer(Modifier.height(8.dp))
            content()
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
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Spacer(Modifier.height(0.dp))
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
