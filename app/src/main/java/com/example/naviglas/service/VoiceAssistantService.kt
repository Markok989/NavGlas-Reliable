package com.example.naviglas.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.naviglas.MainActivity
import com.example.naviglas.R
import com.example.naviglas.engine.ActionExecutor
import com.example.naviglas.engine.AssistantState
import com.example.naviglas.engine.ContactMatcher
import com.example.naviglas.engine.ParsedCommandResult
import com.example.naviglas.engine.VoiceEngineCallback
import com.example.naviglas.engine.VoiceEngineManager

class VoiceAssistantService : Service(), VoiceEngineCallback {

    private lateinit var voiceEngineManager: VoiceEngineManager
    private var isServiceRunning = false

    override fun onCreate() {
        super.onCreate()
        voiceEngineManager = VoiceEngineManager(this, this)
        loadContacts()
    }

    private fun loadContacts() {
        val contacts = ContactMatcher.loadContactsFromPhone(this)
        voiceEngineManager.updateContacts(contacts)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        // Obavezno pozivamo startForeground Notification ako servis već nije aktivan
        if (!isServiceRunning && action != ACTION_STOP_SERVICE) {
            startForegroundServiceNotification()
            isServiceRunning = true
        }

        when (action) {
            ACTION_START_SERVICE -> {
                // Već je osigurano startForeground
            }

            ACTION_STOP_SERVICE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
                isServiceRunning = false
            }

            ACTION_TRIGGER_LISTENING -> {
                voiceEngineManager.startListening()
            }

            ACTION_INCOMING_SMS -> {
                val sender = intent.getStringExtra("EXTRA_SMS_SENDER") ?: "Nepoznati broj"
                val body = intent.getStringExtra("EXTRA_SMS_BODY") ?: ""
                voiceEngineManager.setLastReceivedSms(sender, body)
                val textToSpeak = "Stigla je nova SMS poruka od $sender: $body. Ako želite da je čujete ponovo, izgovorite: Pročitaj poruku opet."
                voiceEngineManager.speak(textToSpeak)
            }

            ACTION_INCOMING_CALL -> {
                val caller = intent.getStringExtra("EXTRA_CALLER") ?: "Nepoznati broj"
                val textToSpeak = "Dolazni poziv od: $caller. Želite li da se javite ili odbijete?"
                voiceEngineManager.speak(textToSpeak)
            }
        }

        return START_STICKY
    }

    private fun startForegroundServiceNotification() {
        val channelId = "NaviGlasServiceChannel"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "NaviGlas Glasovni Asistent",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("NaviGlas je aktivan")
            .setContentText("Spreman za glasovne komande u vožnji")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStateChanged(state: AssistantState) {}

    override fun onSpeechRecognized(text: String) {}

    override fun onCommandParsed(result: ParsedCommandResult) {
        val contacts = ContactMatcher.loadContactsFromPhone(this)
        ActionExecutor.execute(this, result, contacts) { feedback ->
            voiceEngineManager.speak(feedback)
        }
    }

    override fun onError(errorMessage: String) {}

    override fun onDestroy() {
        voiceEngineManager.destroy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_SERVICE = "ACTION_START_SERVICE"
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
        const val ACTION_TRIGGER_LISTENING = "ACTION_TRIGGER_LISTENING"
        const val ACTION_INCOMING_SMS = "ACTION_INCOMING_SMS"
        const val ACTION_INCOMING_CALL = "ACTION_INCOMING_CALL"
    }
}
