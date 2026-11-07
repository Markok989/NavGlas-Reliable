package com.example.naviglas.engine

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Technical events only. Never pass transcripts, contacts, phone numbers or SMS bodies here. */
object VoiceDiagnostics {
    private val lock = Any()
    fun event(context: Context, event: String) = synchronized(lock) {
        try {
        val prefs = context.getSharedPreferences("voice_diagnostics", Context.MODE_PRIVATE)
        val time = SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(Date())
        val previous = prefs.getString("events", "").orEmpty().lineSequence().filter { it.isNotBlank() }.toList()
        prefs.edit().putString("events", (previous.takeLast(79) + "$time ${event.replace('\n', ' ').take(160)}").joinToString("\n")).apply()
        } catch (_: Exception) { /* Diagnostics must never interrupt a voice session. */ }
    }
    fun report(context: Context): String = synchronized(lock) {
        val provider = try { Settings.Secure.getString(context.contentResolver, "voice_recognition_service") ?: "sistemski podrazumevani" }
            catch (_: Exception) { "nedostupno" }
        @Suppress("DEPRECATION")
        val version = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        "NaviGlas $version\n${Build.MANUFACTURER} ${Build.MODEL}; Android ${Build.VERSION.RELEASE}; API ${Build.VERSION.SDK_INT}\nPrepoznavanje: $provider\nJezik zahteva: sr-RS\n\n" +
            context.getSharedPreferences("voice_diagnostics", Context.MODE_PRIVATE).getString("events", "Još nema događaja.")
    }
}
