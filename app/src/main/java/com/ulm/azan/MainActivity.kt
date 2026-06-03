package com.ulm.azan

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.ulm.azan.alarm.AzanService
import com.ulm.azan.alarm.PrayerScheduler
import com.ulm.azan.data.Prayer
import com.ulm.azan.data.PrayerStore
import com.ulm.azan.ocr.ParsedRow
import com.ulm.azan.ocr.PrayerOcrParser
import com.ulm.azan.ui.AzanTheme
import com.ulm.azan.ui.HomeScreen
import com.ulm.azan.ui.ReviewScreen
import com.ulm.azan.ui.SettingsScreen
import java.io.File

enum class Screen { HOME, REVIEW, SETTINGS }

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Seed the bundled month and arm alarms every time the app opens.
        PrayerStore(this).ensureSeeded()
        PrayerScheduler.rescheduleAll(this)

        setContent {
            AzanTheme {
                AppRoot()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Keep alarms fresh if the user changed system permissions while away.
        PrayerScheduler.rescheduleAll(this)
    }

    private fun runOcr(
        uri: Uri,
        onResult: (List<ParsedRow>) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val image = InputImage.fromFilePath(this, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { text -> onResult(PrayerOcrParser.parse(text)) }
                .addOnFailureListener { e -> onError(e.message ?: "Text recognition failed") }
        } catch (e: Exception) {
            onError(e.message ?: "Could not read the image")
        }
    }

    private fun newCameraUri(): Uri {
        val dir = File(cacheDir, "images").apply { mkdirs() }
        val file = File(dir, "scan_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
    }

    @androidx.compose.runtime.Composable
    private fun AppRoot() {
        val store = remember { PrayerStore(this).also { it.ensureSeeded() } }

        var screen by remember { mutableStateOf(Screen.HOME) }
        var parsed by remember { mutableStateOf<List<ParsedRow>>(emptyList()) }
        var scannedImage by remember { mutableStateOf<Uri?>(null) }
        var loading by remember { mutableStateOf(false) }
        var message by remember { mutableStateOf<String?>(null) }
        var dataVersion by remember { mutableIntStateOf(0) }

        // Permission status, recomputed whenever dataVersion bumps (cheap).
        var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

        val notifLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { dataVersion++ }

        val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                scannedImage = uri
                loading = true
                message = null
                runOcr(uri, onResult = {
                    parsed = it
                    loading = false
                    screen = Screen.REVIEW
                    if (it.isEmpty()) message = "No prayer times were detected. Try a clearer photo."
                }, onError = {
                    loading = false
                    message = "Scan failed: $it"
                })
            }
        }

        val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            val uri = pendingCameraUri
            if (success && uri != null) {
                scannedImage = uri
                loading = true
                message = null
                runOcr(uri, onResult = {
                    parsed = it
                    loading = false
                    screen = Screen.REVIEW
                    if (it.isEmpty()) message = "No prayer times were detected. Try a clearer photo."
                }, onError = {
                    loading = false
                    message = "Scan failed: $it"
                })
            }
        }

        when (screen) {
            Screen.HOME -> HomeScreen(
                store = store,
                dataVersion = dataVersion,
                loading = loading,
                message = message,
                onDismissMessage = { message = null },
                onScanGallery = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onScanCamera = {
                    val uri = newCameraUri()
                    pendingCameraUri = uri
                    cameraLauncher.launch(uri)
                },
                onOpenSettings = { screen = Screen.SETTINGS },
                onRequestNotifications = {
                    notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                },
                onTestAzan = { prayer -> testAzan(prayer) }
            )

            Screen.REVIEW -> ReviewScreen(
                rows = parsed,
                imageUri = scannedImage,
                onCancel = { screen = Screen.HOME },
                onSave = { days ->
                    store.merge(days)
                    PrayerScheduler.rescheduleAll(this)
                    dataVersion++
                    message = "Saved ${days.size} day(s) and updated alarms."
                    screen = Screen.HOME
                }
            )

            Screen.SETTINGS -> SettingsScreen(
                store = store,
                onBack = { screen = Screen.HOME },
                onChanged = {
                    PrayerScheduler.rescheduleAll(this)
                    dataVersion++
                },
                onTestAzan = { prayer -> testAzan(prayer) },
                onRequestNotifications = {
                    notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            )
        }
    }

    private fun testAzan(prayer: Prayer) {
        val svc = Intent(this, AzanService::class.java).apply {
            action = AzanService.ACTION_PLAY
            putExtra(AzanService.EXTRA_PRAYER, prayer.key)
        }
        ContextCompat.startForegroundService(this, svc)
    }
}
