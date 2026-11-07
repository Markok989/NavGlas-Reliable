package com.example.naviglas

import android.Manifest
import android.content.Intent
import android.content.Context
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import androidx.activity.compose.BackHandler
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.naviglas.engine.ActionExecutor
import com.example.naviglas.engine.AssistantState
import com.example.naviglas.engine.ContactMatcher
import com.example.naviglas.engine.ParsedCommandResult
import com.example.naviglas.engine.VoiceEngineCallback
import com.example.naviglas.engine.VoiceEngineManager
import com.example.naviglas.service.VoiceAssistantService
import com.example.naviglas.ui.MainDrivingScreen
import com.example.naviglas.ui.NavAppItem
import com.example.naviglas.ui.OverlayVoiceWidget
import com.example.naviglas.ui.PermissionScreen
import com.example.naviglas.ui.SettingsScreen
import com.example.naviglas.ui.theme.DarkBackground
import com.example.naviglas.ui.theme.NaviGlasTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity(), VoiceEngineCallback {

    private var voiceEngineManager: VoiceEngineManager? = null
    private var assistantService: VoiceAssistantService? = null
    private var isBound = false
    private var serviceConnected by mutableStateOf(false)
    private var pendingMicTrigger = false
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = (binder as VoiceAssistantService.LocalBinder).service
            assistantService = service
            voiceEngineManager = service.engine
            serviceConnected = true
            service.addObserver(this@MainActivity)
            service.engine.updateTtsSettings(speechRate, pitch)
            if (pendingMicTrigger) { pendingMicTrigger = false; service.engine.startListening() }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            assistantService = null; voiceEngineManager = null; serviceConnected = false
        }
    }
    private var overlayVoiceWidget: OverlayVoiceWidget? = null

    private var currentState by mutableStateOf(AssistantState.IDLE)
    private var recognizedText by mutableStateOf("")
    private var spokenFeedback by mutableStateOf("")
    private var hasAllPermissions by mutableStateOf(false)
    private var isSettingsOpen by mutableStateOf(false)

    // Podešavanja asistenta, glasa, mehura i navigacije
    private var speechRate by mutableFloatStateOf(0.92f)
    private var pitch by mutableFloatStateOf(0.98f)
    private var overlayAlpha by mutableFloatStateOf(0.95f)
    private var overlaySize by mutableFloatStateOf(64f)
    private var wakeWordEnabled by mutableStateOf(true)
    private var sosNumber by mutableStateOf("")
    private var quickEtaMessage by mutableStateOf("Stižem za desetak minuta, u gužvi sam u vožnji.")
    private var isBluetoothAutoStartEnabled by mutableStateOf(true)

    // Navigacione aplikacije i automatski detektor
    private var selectedNavAppPackage by mutableStateOf("com.google.android.apps.maps")
    private var autoOpenNavAppOnStart by mutableStateOf(false)
    private var triggerAppPackages by mutableStateOf(setOf("com.google.android.apps.maps", "com.waze"))

    // Keširane liste aplikacija za trenutan rad bez lagovanja u UI-ju
    private var cachedLauncherApps by mutableStateOf<List<NavAppItem>>(emptyList())
    private var cachedNavApps by mutableStateOf<List<NavAppItem>>(emptyList())

    // Glavna obavezna dozvola za rad mikrofon-asistenta u vožnji
    private val corePermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO
    )

    private val requiredPermissions: Array<String>
        get() {
            val list = mutableListOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.ANSWER_PHONE_CALLS,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            if (Build.VERSION.SDK_INT >= 33) list.add(Manifest.permission.POST_NOTIFICATIONS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                list.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            return list.toTypedArray()
        }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        checkPermissions()
        if (hasAllPermissions) {
            startAssistantService()
            loadContacts()
            if (autoOpenNavAppOnStart) {
                launchSelectedNavApp()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Podrška za rad preko zaključanog ekrana
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        loadSettings()
        overlayVoiceWidget = OverlayVoiceWidget(this) {
            // Dodir na mehur pokreće glasovno slušanje
            voiceEngineManager?.startListening()
        }

        if (intent?.getBooleanExtra("EXTRA_TRIGGER_MIC", false) == true) {
            pendingMicTrigger = true
        }

        loadTriggerApps()
        loadAppsInBackground()
        checkPermissions()

        setContent {
            NaviGlasTheme {
                BackHandler(enabled = isSettingsOpen) { isSettingsOpen = false; saveSettings() }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground)
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    if (!hasAllPermissions) {
                        PermissionScreen(
                            onRequestPermissions = { requestAllPermissions() },
                            onOpenOverlayPermissionPage = { requestOverlayPermission() },
                            onOpenAppSettingsPage = { openAppSettingsPage() }
                        )
                    } else if (isSettingsOpen) {
                        val navApps = cachedNavApps.ifEmpty { getInstalledNavApps() }
                        val launcherApps = cachedLauncherApps.ifEmpty { getAllLauncherApps() }

                        SettingsScreen(
                            speechRate = speechRate,
                            pitch = pitch,
                            overlayAlpha = overlayAlpha,
                            overlaySize = overlaySize,
                            wakeWordEnabled = wakeWordEnabled,
                            sosNumber = sosNumber,
                            quickEtaMessage = quickEtaMessage,
                            isBluetoothAutoStartEnabled = isBluetoothAutoStartEnabled,
                            selectedNavAppPackage = selectedNavAppPackage,
                            availableNavApps = navApps,
                            autoOpenNavAppOnStart = autoOpenNavAppOnStart,
                            allInstalledApps = launcherApps,
                            triggerAppPackages = triggerAppPackages,
                            onSpeechRateChange = { newRate ->
                                speechRate = newRate
                                voiceEngineManager?.updateTtsSettings(speechRate, pitch)
                            },
                            onPitchChange = { newPitch ->
                                pitch = newPitch
                                voiceEngineManager?.updateTtsSettings(speechRate, pitch)
                            },
                            onOverlayAlphaChange = { newAlpha ->
                                overlayAlpha = newAlpha
                                overlayVoiceWidget?.updateAlpha(newAlpha)
                            },
                            onOverlaySizeChange = { newSize ->
                                overlaySize = newSize
                            },
                            onWakeWordToggle = { wakeWordEnabled = it },
                            onSosNumberChange = { sosNumber = it },
                            onQuickEtaChange = { quickEtaMessage = it },
                            onBluetoothToggle = { isBluetoothAutoStartEnabled = it },
                            onNavAppSelect = { selectedNavAppPackage = it },
                            onAutoOpenNavToggle = { autoOpenNavAppOnStart = it },
                            onTriggerAppToggle = { pkg ->
                                val newSet = triggerAppPackages.toMutableSet()
                                if (newSet.contains(pkg)) {
                                    newSet.remove(pkg)
                                } else {
                                    newSet.add(pkg)
                                }
                                saveTriggerApps(newSet)
                            },
                            onOpenAccessibilitySettings = {
                                try {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            onPlayTestVoice = { sampleText ->
                                voiceEngineManager?.updateTtsSettings(speechRate, pitch)
                                voiceEngineManager?.speak(sampleText)
                            },
                            onOpenSystemTtsSettings = {
                                try {
                                    val intent = Intent("com.android.settings.TTS_SETTINGS")
                                    startActivity(intent)
                                } catch (e: Exception) {
                                    val intent = Intent(Settings.ACTION_SETTINGS)
                                    startActivity(intent)
                                }
                            },
                            onBackClick = { isSettingsOpen = false; saveSettings() }
                        )
                    } else {
                        val navApps = cachedNavApps.ifEmpty { getInstalledNavApps() }
                        val currentNavName = navApps.firstOrNull { it.packageName == selectedNavAppPackage }?.name ?: "Google Maps"

                        MainDrivingScreen(
                            currentState = currentState,
                            recognizedText = recognizedText,
                            spokenFeedback = spokenFeedback,
                            isServiceRunning = serviceConnected,
                            selectedNavAppName = currentNavName,
                            onMicClick = { voiceEngineManager?.startListening() },
                            onCancelClick = { voiceEngineManager?.cancel() },
                            onQuickEtaClick = {
                                val target = voiceEngineManager?.getLastSmsSender()
                                if (target.isNullOrBlank()) spokenFeedback = "Nema poslednjeg pošiljaoca. Recite: pošalji poruku i ime kontakta."
                                else voiceEngineManager?.submitCommand(ParsedCommandResult(
                                    action = com.example.naviglas.engine.CommandAction.SEND_SMS,
                                    contactName = target, recipientQuery = target,
                                    messageText = quickEtaMessage, spokenFeedback = "Pripremam brzi odgovor."
                                ))
                            },
                            onSosClick = {
                                saveSettings()
                                if (sosNumber.isBlank()) spokenFeedback = "Prvo podesite SOS primaoca u podešavanjima."
                                else voiceEngineManager?.submitCommand(ParsedCommandResult(
                                    action = com.example.naviglas.engine.CommandAction.SEND_SOS,
                                    contactName = sosNumber, recipientQuery = sosNumber,
                                    spokenFeedback = "Pripremam SOS poruku."
                                ))
                            },
                            onOpenNavAppClick = { launchSelectedNavApp() },
                            onSettingsClick = { voiceEngineManager?.cancel(false); isSettingsOpen = true }
                        )
                    }
                }
            }
        }
    }

    private fun loadAppsInBackground() {
        lifecycleScope.launch(Dispatchers.IO) {
            val nav = getInstalledNavApps()
            val all = getAllLauncherApps()
            withContext(Dispatchers.Main) {
                cachedNavApps = nav
                cachedLauncherApps = all
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Proveravamo dozvole pri povratku iz podešavanja telefona
        checkPermissions()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("EXTRA_TRIGGER_MIC", false)) {
            if (serviceConnected) voiceEngineManager?.startListening() else pendingMicTrigger = true
        }
    }

    private fun loadTriggerApps() {
        val prefs = getSharedPreferences("NaviGlasPrefs", MODE_PRIVATE)
        val saved = prefs.getStringSet("TRIGGER_APPS", null)
        if (saved != null) {
            triggerAppPackages = saved
        } else {
            saveTriggerApps(setOf("com.google.android.apps.maps", "com.waze"))
        }
    }

    private fun saveTriggerApps(set: Set<String>) {
        triggerAppPackages = set
        val prefs = getSharedPreferences("NaviGlasPrefs", MODE_PRIVATE)
        prefs.edit().putStringSet("TRIGGER_APPS", set).apply()
    }

    private fun getAllLauncherApps(): List<NavAppItem> {
        val pm = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        val list = mutableListOf<NavAppItem>()
        for (ri in resolveInfos) {
            val pkg = ri.activityInfo.packageName
            if (pkg == packageName) continue
            val label = ri.loadLabel(pm).toString()
            if (label.isNotBlank()) {
                list.add(NavAppItem(pkg, label))
            }
        }
        return list.sortedBy { it.name.lowercase() }
    }

    private fun getInstalledNavApps(): List<NavAppItem> {
        val pm = packageManager
        val list = mutableListOf<NavAppItem>()
        val knownApps = listOf(
            "com.google.android.apps.maps" to "Google Maps",
            "com.waze" to "Waze",
            "com.sygic.aura" to "Sygic",
            "com.here.app.maps" to "HERE WeGo",
            "com.petal.maps" to "Petal Maps"
        )

        for ((pkg, name) in knownApps) {
            try {
                pm.getPackageInfo(pkg, 0)
                list.add(NavAppItem(pkg, name))
            } catch (e: Exception) {
                // Nije instalirana
            }
        }

        if (list.isEmpty()) {
            list.add(NavAppItem("com.google.android.apps.maps", "Google Maps"))
        }
        return list
    }

    private fun launchSelectedNavApp() {
        val intent = packageManager.getLaunchIntentForPackage(selectedNavAppPackage)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)

            // Prikazujemo lebdeći mehur (Overlay Widget) preko mape
            if (Settings.canDrawOverlays(this)) {
                overlayVoiceWidget?.showOverlay("NaviGlas Aktivan", alpha = overlayAlpha)
            }
        }
    }

    private fun checkPermissions() {
        // Glavne obavezne dozvole za rad asistenta
        val coreGranted = corePermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        hasAllPermissions = coreGranted
        if (hasAllPermissions) {
            startAssistantService()
            loadContacts()
        }
    }

    private fun requestAllPermissions() { permissionLauncher.launch(requiredPermissions) }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun openAppSettingsPage() {
        try {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadContacts() {
        assistantService?.refreshContacts()
    }

    private fun startAssistantService() {
        if (isBound) return
        val intent = Intent(this, VoiceAssistantService::class.java).setAction(VoiceAssistantService.ACTION_START_SERVICE)
        try {
            ContextCompat.startForegroundService(this, intent)
            isBound = bindService(intent, connection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) { spokenFeedback = "Servis nije pokrenut. Proverite dozvolu za mikrofon." }
    }

    private fun saveSettings() {
        getSharedPreferences("NaviGlasPrefs", MODE_PRIVATE).edit()
            .putFloat("SPEECH_RATE", speechRate).putFloat("PITCH", pitch)
            .putFloat("OVERLAY_ALPHA", overlayAlpha).putFloat("OVERLAY_SIZE", overlaySize)
            .putString("SOS_NUMBER", sosNumber.trim()).putString("QUICK_ETA", quickEtaMessage)
            .putString("NAV_APP", selectedNavAppPackage).putBoolean("AUTO_OPEN_NAV", autoOpenNavAppOnStart)
            .putBoolean("BLUETOOTH_AUTO", isBluetoothAutoStartEnabled).apply()
    }
    private fun loadSettings() {
        val p = getSharedPreferences("NaviGlasPrefs", MODE_PRIVATE)
        speechRate = p.getFloat("SPEECH_RATE", 0.92f); pitch = p.getFloat("PITCH", 0.98f)
        overlayAlpha = p.getFloat("OVERLAY_ALPHA", 0.95f); overlaySize = p.getFloat("OVERLAY_SIZE", 64f)
        sosNumber = p.getString("SOS_NUMBER", "").orEmpty()
        quickEtaMessage = p.getString("QUICK_ETA", quickEtaMessage) ?: quickEtaMessage
        selectedNavAppPackage = p.getString("NAV_APP", selectedNavAppPackage) ?: selectedNavAppPackage
        autoOpenNavAppOnStart = p.getBoolean("AUTO_OPEN_NAV", false)
        isBluetoothAutoStartEnabled = p.getBoolean("BLUETOOTH_AUTO", false)
        wakeWordEnabled = false
    }
    override fun onPause() { saveSettings(); super.onPause() }

    override fun onStateChanged(state: AssistantState) {
        currentState = state
        overlayVoiceWidget?.updateStatus(when (state) {
            AssistantState.LISTENING -> "Slušam..."
            AssistantState.PROCESSING -> "Obrađujem..."
            AssistantState.SPEAKING -> "Govorim..."
            AssistantState.WAITING_FOR_CONFIRMATION -> "Čekam odgovor..."
            else -> "NaviGlas"
        })
    }

    override fun onSpeechRecognized(text: String) {
        recognizedText = text
    }

    override fun onCommandParsed(result: ParsedCommandResult) { voiceEngineManager?.submitCommand(result) }
    override fun onFeedback(text: String) { spokenFeedback = text }

    override fun onError(errorMessage: String) {
        spokenFeedback = errorMessage
    }

    override fun onDestroy() {
        overlayVoiceWidget?.hideOverlay()
        assistantService?.removeObserver(this)
        if (isBound) { unbindService(connection); isBound = false }
        voiceEngineManager = null; assistantService = null
        super.onDestroy()
    }
}

