package com.example.naviglas.service

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.*
import androidx.core.app.NotificationCompat
import com.example.naviglas.MainActivity
import com.example.naviglas.R
import com.example.naviglas.engine.*
import kotlinx.coroutines.*

/** The service owns the sole microphone/TTS session. Activities only observe it. */
class VoiceAssistantService : Service(), VoiceEngineCallback {
    lateinit var engine: VoiceEngineManager
        private set
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val observers = mutableSetOf<VoiceEngineCallback>()
    private var feedback = "Dodirnite mikrofon i izgovorite komandu."
    private var recognized = ""
    private var foreground = false
    inner class LocalBinder : Binder() { val service get() = this@VoiceAssistantService }
    override fun onBind(intent: Intent?): IBinder = LocalBinder()
    override fun onCreate() {
        super.onCreate()
        engine = VoiceEngineManager(applicationContext, this)
        refreshContacts()
    }
    fun refreshContacts() { scope.launch {
        val contacts = withContext(Dispatchers.IO) { ContactMatcher.loadContactsFromPhone(this@VoiceAssistantService) }
        engine.updateContacts(contacts)
    } }
    fun addObserver(observer: VoiceEngineCallback) {
        observers.add(observer)
        observer.onStateChanged(engine.currentState); observer.onSpeechRecognized(recognized); observer.onFeedback(feedback)
    }
    fun removeObserver(observer: VoiceEngineCallback) { observers.remove(observer) }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            engine.cancel(false)
            isRunning = false
            foreground = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            observers.toList().forEach { it.onServiceStopped() }
            stopSelf()
            return START_NOT_STICKY
        }
        if (!foreground && !enterForeground()) { stopSelf(); return START_NOT_STICKY }
        when (intent?.action) {
            ACTION_TRIGGER_LISTENING -> engine.startListening()
            ACTION_INCOMING_SMS -> {
                val sender = intent.getStringExtra("EXTRA_SMS_SENDER") ?: "Nepoznati broj"
                engine.setLastReceivedSms(sender, intent.getStringExtra("EXTRA_SMS_BODY").orEmpty())
                if (engine.currentState == AssistantState.IDLE) engine.speak("Stigla je poruka od $sender. Za čitanje recite pročitaj poruku opet.")
            }
            ACTION_INCOMING_CALL -> if (engine.currentState == AssistantState.IDLE) engine.speak("Dolazni poziv od ${intent.getStringExtra("EXTRA_CALLER") ?: "nepoznatog broja"}.")
        }
        return START_NOT_STICKY
    }
    private fun enterForeground(): Boolean {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return false
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "NaviGlas režim vožnje", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stop = PendingIntent.getService(this, 1, Intent(this, VoiceAssistantService::class.java).setAction(ACTION_STOP_SERVICE), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setContentTitle("NaviGlas je spreman")
            .setContentText("Slušanje počinje tek na vaš zahtev")
            .setSmallIcon(R.drawable.ic_launcher_foreground).setContentIntent(open).setOngoing(true)
            .addAction(0, "Zaustavi", stop).build()
        return try {
            if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            else startForeground(NOTIFICATION_ID, notification)
            foreground = true; isRunning = true; true
        } catch (e: Exception) { onError("Režim vožnje nije pokrenut. Otvorite aplikaciju i proverite dozvole."); false }
    }
    override fun onStateChanged(state: AssistantState) { observers.toList().forEach { it.onStateChanged(state) } }
    override fun onSpeechRecognized(text: String) { recognized = text; observers.toList().forEach { it.onSpeechRecognized(text) } }
    override fun onFeedback(text: String) { feedback = text; observers.toList().forEach { it.onFeedback(text) } }
    override fun onError(errorMessage: String) { feedback = errorMessage; observers.toList().forEach { it.onError(errorMessage) } }
    override fun onCommandParsed(result: ParsedCommandResult) { scope.launch {
        val contacts = withContext(Dispatchers.IO) { ContactMatcher.loadContactsFromPhone(this@VoiceAssistantService) }
        try {
            withContext(Dispatchers.IO) {
                ActionExecutor.execute(this@VoiceAssistantService, result, contacts) { message ->
                    scope.launch { engine.speak(message) }
                }
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { onError("Radnja nije mogla da se izvrši. Proverite dozvole aplikacije.") }
    } }
    override fun onDestroy() {
        isRunning = false
        observers.clear(); scope.cancel(); engine.destroy()
        if (foreground) stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
    companion object {
        @Volatile var isRunning = false
            private set
        const val NOTIFICATION_ID = 1001
        private const val CHANNEL = "NaviGlasServiceChannel"
        const val ACTION_START_SERVICE = "ACTION_START_SERVICE"
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
        const val ACTION_TRIGGER_LISTENING = "ACTION_TRIGGER_LISTENING"
        const val ACTION_INCOMING_SMS = "ACTION_INCOMING_SMS"
        const val ACTION_INCOMING_CALL = "ACTION_INCOMING_CALL"
    }
}

