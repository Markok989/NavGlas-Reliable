package com.example.naviglas.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.example.naviglas.MainActivity
import com.example.naviglas.ui.OverlayVoiceWidget

class NaviGlasAccessibilityService : AccessibilityService() {

    private var overlayVoiceWidget: OverlayVoiceWidget? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastTriggerTime = 0L

    private val defaultNavPackages = setOf(
        "com.google.android.apps.maps",
        "com.google.android.apps.maps.lite",
        "com.waze",
        "net.osmand",
        "net.osmand.plus",
        "com.sygic.aura",
        "com.here.app.maps",
        "com.petal.maps",
        "com.nokia.here"
    )

    override fun onCreate() {
        super.onCreate()
        overlayVoiceWidget = OverlayVoiceWidget(this) {
            // Dodir na mehur pokreće glasovno slušanje
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("EXTRA_TRIGGER_MIC", true)
            }
            startActivity(intent)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            // Ne reagujemo ako je otvoren sam NaviGlas
            if (packageName == applicationContext.packageName) return

            val prefs = applicationContext.getSharedPreferences("NaviGlasPrefs", Context.MODE_PRIVATE)
            val savedApps = prefs.getStringSet("TRIGGER_APPS", null)
            val triggerApps = if (!savedApps.isNullOrEmpty()) savedApps else defaultNavPackages

            val isNavApp = defaultNavPackages.contains(packageName) || triggerApps.contains(packageName)

            if (isNavApp) {
                // Pamćena aktivna navigacija radi automatskog povratka u nju nakon komande
                prefs.edit().putString("LAST_ACTIVE_NAV_PACKAGE", packageName).apply()

                val now = System.currentTimeMillis()
                if (now - lastTriggerTime < 2000L) return
                lastTriggerTime = now

                mainHandler.post {
                    // 2. Odmah prikazujemo lebdeći mehur preko mapa (Google Maps, OsmAnd, Waze itd.)
                    if (Settings.canDrawOverlays(this@NaviGlasAccessibilityService)) {
                        overlayVoiceWidget?.showOverlay("NaviGlas Aktivan")
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        overlayVoiceWidget?.hideOverlay()
        super.onDestroy()
    }

    override fun onInterrupt() {}
}

