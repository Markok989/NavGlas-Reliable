package com.example.naviglas.engine

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

enum class AssistantState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    WAITING_FOR_CONFIRMATION
}

interface VoiceEngineCallback {
    fun onStateChanged(state: AssistantState)
    fun onSpeechRecognized(text: String)
    fun onCommandParsed(result: ParsedCommandResult)
    fun onError(errorMessage: String)
}

class VoiceEngineManager(
    private val context: Context,
    private val callback: VoiceEngineCallback
) : TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false
    private val audioRoutingManager = AudioRoutingManager(context)
    private val mainHandler = Handler(Looper.getMainLooper())

    var currentState: AssistantState = AssistantState.IDLE
        private set(value) {
            field = value
            callback.onStateChanged(value)
        }

    private var confirmationErrors = 0
    private var destroyed = false
    private var pendingActionToConfirm: ParsedCommandResult? = null
    private var pendingSmsContactName: String? = null
    private var pendingPhoneNumber: String? = null
    private var pendingDisambiguationContacts: List<Contact>? = null
    private var pendingPhoneNumbersContact: Contact? = null
    private var pendingActionAfterDisambiguation: CommandAction? = null
    private var loadedContacts: List<Contact> = emptyList()

    private var lastReceivedSmsSender: String? = null
    private var lastReceivedSmsBody: String? = null

    init {
        initSpeechRecognizer()
        initTts()
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        currentState = AssistantState.LISTENING
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        currentState = AssistantState.PROCESSING
                    }

                    override fun onError(error: Int) {
                        if (hasPendingAction() && confirmationErrors++ < 2 &&
                            (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
                            mainHandler.postDelayed({
                                if (!destroyed) startListeningForConfirmation()
                            }, 500L)
                        } else {
                            clearPendingAction()
                            currentState = AssistantState.IDLE
                            audioRoutingManager.abandonAudioFocus()
                            val msg = when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH -> "Nisam čuo komandu."
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Vreme za govor je isteklo."
                                else -> "Greška pri slušanju."
                            }
                            callback.onError(msg)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spokenText = matches?.firstOrNull() ?: ""
                        callback.onSpeechRecognized(spokenText)
                        processSpokenInput(spokenText)
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    private fun initTts() {
        try {
            textToSpeech = TextToSpeech(context, this, "com.google.android.tts")
        } catch (e: Exception) {
            textToSpeech = TextToSpeech(context, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val srLocale = Locale("sr", "RS")
            val result = textToSpeech?.setLanguage(srLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech?.setLanguage(Locale.forLanguageTag("sr"))
            }

            try {
                val availableVoices = textToSpeech?.voices
                if (availableVoices != null) {
                    val bestVoice = availableVoices.firstOrNull { voice ->
                        (voice.locale.language == "sr" || voice.locale.country == "RS") &&
                        voice.quality >= android.speech.tts.Voice.QUALITY_HIGH
                    } ?: availableVoices.firstOrNull { voice ->
                        voice.locale.language == "sr"
                    }
                    if (bestVoice != null) {
                        textToSpeech?.voice = bestVoice
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            textToSpeech?.setSpeechRate(0.92f)
            textToSpeech?.setPitch(0.98f)

            isTtsReady = true

            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    currentState = AssistantState.SPEAKING
                }

                override fun onDone(utteranceId: String?) {
                    if (destroyed) return
                    mainHandler.postDelayed({
                        if (destroyed) return@postDelayed
                        if (pendingActionToConfirm != null || pendingSmsContactName != null ||
                            pendingDisambiguationContacts != null || pendingPhoneNumbersContact != null) {
                            startListeningForConfirmation()
                        } else {
                            currentState = AssistantState.IDLE
                            audioRoutingManager.abandonAudioFocus()
                        }
                    }, 450L)
                }

                override fun onError(utteranceId: String?) {
                    currentState = AssistantState.IDLE
                    audioRoutingManager.abandonAudioFocus()
                }
            })
        }
    }

    fun updateContacts(contacts: List<Contact>) {
        this.loadedContacts = contacts
    }

    fun updateTtsSettings(speechRate: Float, pitch: Float) {
        if (isTtsReady) {
            try {
                textToSpeech?.setSpeechRate(speechRate)
                textToSpeech?.setPitch(pitch)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun startListening() {
        if (destroyed) return
        if (speechRecognizer == null) {
            callback.onError("Prepoznavanje govora nije dostupno na telefonu.")
            return
        }
        confirmationErrors = 0
        audioRoutingManager.requestAudioFocus()
        audioRoutingManager.enableCarAudioMode()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "sr-RS")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "sr-RS")
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 4000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        mainHandler.post {
            try {
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                callback.onError("Ne mogu da pokrenem mikrofon.")
            }
        }
    }

    private fun startListeningForConfirmation() {
        if (destroyed || !hasPendingAction()) return
        currentState = AssistantState.WAITING_FOR_CONFIRMATION
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "sr-RS")
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 4000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
        }
        mainHandler.post {
            try {
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                currentState = AssistantState.IDLE
            }
        }
    }

    fun speak(text: String, utteranceId: String = "NaviGlasTTS") {
        if (!isTtsReady || textToSpeech == null) return
        audioRoutingManager.requestAudioFocus()
        audioRoutingManager.enableCarAudioMode()

        val normalized = SerbianTextNormalizer.prepareForTts(text)
        @Suppress("DEPRECATION")
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS, "true")
        }

        textToSpeech?.speak(normalized, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun setLastReceivedSms(sender: String, body: String) {
        this.lastReceivedSmsSender = sender
        this.lastReceivedSmsBody = body
    }

    fun getLastSmsSender(): String? = lastReceivedSmsSender

    private fun processSpokenInput(input: String) {
        val lower = SerbianTextNormalizer.stripDiacritics(input).lowercase(Locale.ROOT)
        confirmationErrors = 0
        if (ConfirmationPolicy.isCancellation(input)) {
            clearPendingAction()
            speak("Radnja je otkazana.")
            return
        }

        // 1. Odabir kontakta kada postoji više kontakata sa istim imenom
        if (pendingDisambiguationContacts != null) {
            val list = pendingDisambiguationContacts!!

            val chosenContact = if (lower.contains("prvi") || lower.contains("jedan")) {
                list.getOrNull(0)
            } else if (lower.contains("drugi") || lower.contains("dva")) {
                list.getOrNull(1)
            } else {
                list.firstOrNull { lower.contains(SerbianTextNormalizer.stripDiacritics(it.name).lowercase(Locale.ROOT)) }
            }

            if (chosenContact != null) {
                pendingDisambiguationContacts = null
                handleContactSelected(chosenContact, pendingActionAfterDisambiguation)
            } else {
                speak("Nisam razumeo izbor. Recite puno ime kontakta, prvi, drugi ili otkaži.")
            }
            return
        }

        // 2. Odabir broja telefona ako kontakt ima više od jednog broja
        if (pendingPhoneNumbersContact != null) {
            val contact = pendingPhoneNumbersContact!!

            val chosenNumber = if (lower.contains("drugi") || lower.contains("dva")) {
                contact.phoneNumbers.getOrNull(1)
            } else if (lower == "prvi" || lower == "jedan") {
                contact.phoneNumbers.firstOrNull()
            } else null
            if (chosenNumber == null) {
                speak("Recite prvi ili drugi broj, ili otkaži.")
                return
            }
            pendingPhoneNumbersContact = null
            val finalContact = contact.copy(phoneNumber = chosenNumber)
            handleContactSelected(finalContact, pendingActionAfterDisambiguation)
            return
        }

        // 3. Ako čekamo na tekst poruke
        if (pendingSmsContactName != null) {
            val contactName = pendingSmsContactName!!
            pendingSmsContactName = null

            val parsedSms = ParsedCommandResult(
                action = CommandAction.SEND_SMS,
                contactName = contactName,
                messageText = input,
                phoneNumber = pendingPhoneNumber,
                spokenFeedback = "Napisao sam poruku za $contactName: $input"
            )

            pendingActionToConfirm = parsedSms
            speak("Napisao sam poruku za $contactName: $input. Da li želite da potvrdite slanje?")
            return
        }

        // 4. Ako smo u krugu potvrde
        if (pendingActionToConfirm != null) {
            val action = pendingActionToConfirm!!

            if (lower.contains("procitaj") || lower.contains("ponovi") || lower.contains("sta pise") || lower.contains("tekst")) {
                val messageToRead = action.messageText ?: "Nema teksta."
                speak("Pripremljena poruka za ${action.contactName} glasi: $messageToRead. Da li želite da potvrdite slanje?")
                return
            }

            if (lower.contains("promeni") || lower.contains("izmeni") || lower.contains("ponovo")) {
                pendingActionToConfirm = null
                pendingSmsContactName = action.contactName
                speak("Izgovorite novu poruku za ${action.contactName}.")
                return
            }

            pendingActionToConfirm = null

            if (ConfirmationPolicy.isAffirmative(input)) {
                clearPendingAction()
                callback.onCommandParsed(action)
            } else {
                pendingActionToConfirm = action
                speak("Recite potvrdi za izvršavanje ili otkaži.")
            }
            return
        }

        // 5. Standardno parsiranje komande
        clearPendingAction()
        val parsed = VoiceCommandParser.parseInput(input, loadedContacts)

        if (parsed.action == CommandAction.READ_LAST_SMS) {
            if (!lastReceivedSmsBody.isNullOrBlank()) {
                val smsText = "Poslednja poruka od $lastReceivedSmsSender glasi: $lastReceivedSmsBody"
                speak(smsText)
                callback.onCommandParsed(parsed.copy(spokenFeedback = smsText))
            } else {
                val smsText = "Nema nedavno pristiglih poruka."
                speak(smsText)
                callback.onCommandParsed(parsed.copy(spokenFeedback = smsText))
            }
            return
        }

        if (parsed.action == CommandAction.SEND_SMS || parsed.action == CommandAction.CALL) {
            val contactName = parsed.contactName
            if (!contactName.isNullOrBlank()) {
                val allMatches = ContactMatcher.findAllMatches(contactName, loadedContacts)

                // Ako postoji više kontakata sa istim imenom (npr. Marko Petrović i Marko Nikolić)
                if (allMatches.size > 1) {
                    pendingDisambiguationContacts = allMatches
                    pendingActionAfterDisambiguation = parsed.action
                    val namesStr = allMatches.joinToString(" i ") { it.name }
                    speak("Pronašao sam ${allMatches.size} kontakta: $namesStr. Koji želite da izaberemo: prvi ili drugi?")
                    return
                }

                val matchedContact = allMatches.firstOrNull() ?: loadedContacts.firstOrNull { it.name.contains(contactName, ignoreCase = true) }

                // Ako izabrani kontakt ima više brojeva telefona
                if (matchedContact != null && matchedContact.phoneNumbers.size > 1) {
                    pendingPhoneNumbersContact = matchedContact
                    pendingActionAfterDisambiguation = parsed.action
                    val num1 = matchedContact.phoneNumbers[0]
                    val num2 = matchedContact.phoneNumbers[1]
                    speak("Kontakt ${matchedContact.name} ima više brojeva: prvi je $num1, drugi je $num2. Koji broj želite: prvi ili drugi?")
                    return
                }
            }

            if (parsed.action == CommandAction.SEND_SMS) {
                if (parsed.messageText.isNullOrBlank()) {
                    pendingSmsContactName = parsed.contactName
                    speak(parsed.spokenFeedback)
                } else {
                    pendingActionToConfirm = parsed
                    speak("${parsed.spokenFeedback} Da li želite da potvrdite?")
                }
            } else {
                pendingActionToConfirm = parsed
                speak("${parsed.spokenFeedback} Da li želite da potvrdite?")
            }
        } else {
            speak(parsed.spokenFeedback)
            callback.onCommandParsed(parsed)
        }
    }

    private fun handleContactSelected(contact: Contact, action: CommandAction?) {
        val resolvedAction = action ?: CommandAction.CALL
        if (resolvedAction == CommandAction.SEND_SMS) {
            pendingPhoneNumber = contact.phoneNumber
            pendingSmsContactName = contact.name
            speak("Izabran je kontakt ${contact.name}. Koju poruku želite da mu pošaljete?")
        } else {
            val parsedCall = ParsedCommandResult(
                action = CommandAction.CALL,
                contactName = contact.name,
                phoneNumber = contact.phoneNumber,
                spokenFeedback = "Pozivam ${contact.name} na broj ${contact.phoneNumber}."
            )
            pendingActionToConfirm = parsedCall
            speak("Pozivam ${contact.name} na broj ${contact.phoneNumber}. Da li potvrdite?")
        }
    }

    private fun hasPendingAction() = pendingActionToConfirm != null || pendingSmsContactName != null ||
        pendingDisambiguationContacts != null || pendingPhoneNumbersContact != null

    private fun clearPendingAction() {
        pendingActionToConfirm = null
        pendingSmsContactName = null
        pendingDisambiguationContacts = null
        pendingPhoneNumbersContact = null
        pendingActionAfterDisambiguation = null
        pendingPhoneNumber = null
    }

    fun destroy() {
        destroyed = true
        clearPendingAction()
        mainHandler.removeCallbacksAndMessages(null)
        audioRoutingManager.abandonAudioFocus()
        speechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        audioRoutingManager.disableCarAudioMode()
    }
}
