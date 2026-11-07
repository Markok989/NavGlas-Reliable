package com.example.naviglas.engine

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

enum class AssistantState { IDLE, LISTENING, PROCESSING, SPEAKING, WAITING_FOR_CONFIRMATION }
interface VoiceEngineCallback {
    fun onStateChanged(state: AssistantState)
    fun onSpeechRecognized(text: String)
    fun onCommandParsed(result: ParsedCommandResult)
    fun onError(errorMessage: String)
    fun onFeedback(text: String) {}
}

class VoiceEngineManager(private val context: Context, private val callback: VoiceEngineCallback) : TextToSpeech.OnInitListener {
    private val handler = Handler(Looper.getMainLooper())
    private val conversation = ConversationController()
    private val audio = AudioRoutingManager(context)
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var destroyed = false
    private var recognitionActive = false
    private var recognitionGeneration = 0
    private var speechId = 0
    private var retries = 0
    private var queuedSpeech: String? = null
    private var rate = 0.92f
    private var pitch = 0.98f
    private var lastSender: String? = null
    private var lastBody: String? = null
    var currentState = AssistantState.IDLE
        private set(value) { field = value; callback.onStateChanged(value) }

    init { initRecognizer(); tts = TextToSpeech(context, this) }

    private fun initRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return
        val generation = ++recognitionGeneration
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                private fun valid() = !destroyed && generation == recognitionGeneration && recognitionActive
                override fun onReadyForSpeech(params: Bundle?) { if (valid()) currentState = if (conversation.awaitingInput) AssistantState.WAITING_FOR_CONFIRMATION else AssistantState.LISTENING }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { if (valid()) currentState = AssistantState.PROCESSING }
                override fun onError(error: Int) {
                    if (!valid()) return
                    recognitionActive = false
                    handler.removeCallbacks(watchdog)
                    if (conversation.awaitingInput && retries++ < 2 && error in setOf(SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
                        handler.postDelayed({ listen() }, 600)
                    } else fail(when (error) {
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Dozvolite pristup mikrofonu u podešavanjima."
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Proverite internet vezu i pokušajte ponovo."
                        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Nisam čuo odgovor. Radnja je otkazana."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Mikrofon je zauzet. Pokušajte ponovo."
                        else -> "Prepoznavanje govora nije uspelo. Pokušajte ponovo."
                    })
                }
                override fun onResults(results: Bundle?) {
                    if (!valid()) return
                    recognitionActive = false
                    handler.removeCallbacks(watchdog)
                    retries = 0
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    callback.onSpeechRecognized(text)
                    refreshSosRecipient()
                    handle(conversation.accept(text))
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }
    private val watchdog = Runnable { if (recognitionActive) fail("Slušanje je isteklo. Radnja je otkazana.") }

    override fun onInit(status: Int) { handler.post {
        if (destroyed) return@post
        if (status != TextToSpeech.SUCCESS) { fail("Govorni izlaz nije dostupan. Proverite sistemska podešavanja glasa."); return@post }
        val language = tts?.setLanguage(Locale.forLanguageTag("sr-RS"))
        if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
            fail("Instalirajte srpski glas u sistemskim podešavanjima govora.")
            return@post
        }
        ttsReady = true
        tts?.setSpeechRate(rate); tts?.setPitch(pitch)
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { handler.post { if (!destroyed && utteranceId == speechId.toString()) currentState = AssistantState.SPEAKING } }
            override fun onDone(utteranceId: String?) { handler.postDelayed({
                if (!destroyed && utteranceId == speechId.toString()) {
                    if (conversation.awaitingInput) listen() else { currentState = AssistantState.IDLE; releaseAudio() }
                }
            }, 450) }
            override fun onError(utteranceId: String?) { handler.post { if (!destroyed && utteranceId == speechId.toString()) fail("Glas nije mogao da se reprodukuje. Radnja je otkazana.") } }
        })
        queuedSpeech?.let { queuedSpeech = null; speak(it) }
    } }

    fun updateContacts(contacts: List<Contact>) { conversation.contacts = contacts }
    fun updateTtsSettings(speechRate: Float, pitch: Float) {
        rate = speechRate.coerceIn(0.5f, 2f); this.pitch = pitch.coerceIn(0.5f, 2f)
        if (ttsReady) { tts?.setSpeechRate(rate); tts?.setPitch(this.pitch) }
    }
    fun startListening() {
        if (destroyed || recognitionActive || currentState == AssistantState.PROCESSING) return
        if (currentState == AssistantState.SPEAKING) { cancel(); return }
        retries = 0
        listen()
    }
    private fun listen() {
        if (destroyed || recognitionActive) return
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { fail("Mikrofon nije dozvoljen."); return }
        if (recognizer == null) { fail("Prepoznavanje govora nije dostupno na telefonu."); return }
        try {
            audio.requestAudioFocus { handler.post { cancel() } }
            recognitionActive = true
            currentState = if (conversation.awaitingInput) AssistantState.WAITING_FOR_CONFIRMATION else AssistantState.LISTENING
            recognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "sr-RS")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            })
            handler.postDelayed(watchdog, 20000)
        } catch (e: Exception) { fail("Ne mogu da pokrenem mikrofon. Proverite dozvole.") }
    }
    fun speak(text: String, utteranceId: String = "NaviGlasTTS") {
        if (destroyed) return
        callback.onFeedback(text)
        if (!ttsReady) { queuedSpeech = text; callback.onError("Glas se priprema. Ako ne progovori, proverite srpski TTS glas."); return }
        try {
            audio.requestAudioFocus { handler.post { cancel() } }
            val id = (++speechId).toString()
            currentState = AssistantState.SPEAKING
            if (tts?.speak(SerbianTextNormalizer.prepareForTts(text), TextToSpeech.QUEUE_FLUSH, Bundle(), id) == TextToSpeech.ERROR) fail("Govorni izlaz nije uspeo.")
        } catch (e: Exception) { fail("Govorni izlaz nije dostupan.") }
    }
    fun submitCommand(command: ParsedCommandResult) {
        cancel(false)
        refreshSosRecipient()
        handle(conversation.submit(command))
    }
    private fun refreshSosRecipient() { conversation.sosRecipient = context.getSharedPreferences("NaviGlasPrefs", Context.MODE_PRIVATE).getString("SOS_NUMBER", "").orEmpty() }
    fun setLastReceivedSms(sender: String, body: String) { lastSender = sender; lastBody = body }
    fun getLastSmsSender(): String? = lastSender
    private fun handle(reply: ConversationReply) {
        val command = reply.command
        when (command?.action) {
            CommandAction.READ_LAST_SMS -> speak(lastBody?.let { "Poslednja poruka od $lastSender glasi: $it" } ?: "Nema nedavno pristiglih poruka.")
            CommandAction.SEND_SOS -> {
                val recipient = context.getSharedPreferences("NaviGlasPrefs", Context.MODE_PRIVATE).getString("SOS_NUMBER", "").orEmpty()
                if (command.phoneNumber == null) handle(conversation.submit(command.copy(contactName = recipient, recipientQuery = recipient)))
                else callback.onCommandParsed(command)
            }
            null -> if (reply.text.isNotBlank()) speak(reply.text)
            else -> callback.onCommandParsed(command)
        }
    }
    fun cancel(announce: Boolean = true) {
        handler.removeCallbacksAndMessages(null)
        queuedSpeech = null; speechId++; recognitionActive = false; recognitionGeneration++
        recognizer?.destroy(); recognizer = null
        tts?.stop(); conversation.cancel(); releaseAudio()
        if (!destroyed) { initRecognizer(); currentState = AssistantState.IDLE; if (announce) callback.onFeedback("Radnja je otkazana.") }
    }
    private fun releaseAudio() { audio.abandonAudioFocus() }
    private fun fail(message: String) { cancel(false); callback.onError(message) }
    fun destroy() { destroyed = true; cancel(false); tts?.shutdown(); tts = null }
}

