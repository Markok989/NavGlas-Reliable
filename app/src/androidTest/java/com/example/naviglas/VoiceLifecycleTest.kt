package com.example.naviglas

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.speech.SpeechRecognizer
import android.speech.RecognizerIntent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.example.naviglas.engine.*
import com.example.naviglas.testsupport.ScriptedRecognitionService
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ConcurrentLinkedQueue

/** Real Android recognizer IPC + scripted provider, without acoustic/network dependencies. */
class VoiceLifecycleTest {
    private data class RecognitionStep(val text: String? = null, val error: Int? = null, val delayMs: Long = 200)
    @get:Rule(order = 0) val permissions = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_CONTACTS)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    private var engine: VoiceEngineManager? = null
    private val commands = CopyOnWriteArrayList<ParsedCommandResult>()
    private val errors = CopyOnWriteArrayList<String>()
    private val states = CopyOnWriteArrayList<AssistantState>()
    private val outputs = CopyOnWriteArrayList<String>()
    private var focus: AudioFocusRequest? = null
    private var outputDelay = 80L
    private var outputFails = false
    private val confirmation = AtomicBoolean(false)

    private fun start(vararg script: RecognitionStep, denied: Boolean = false) {
        val testContext = InstrumentationRegistry.getInstrumentation().context
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "pm grant ${testContext.packageName} android.permission.RECORD_AUDIO"
        ).use { descriptor -> java.io.FileInputStream(descriptor.fileDescriptor).use { it.readBytes() } }
        val queue = ConcurrentLinkedQueue(script.toList())
        // The scripted provider runs in the test APK process. Its script travels as intent extras.
        compose.runOnIdle {
            val base = compose.activity
            val context = if (!denied) base else object : ContextWrapper(base) {
                override fun checkSelfPermission(permission: String): Int = if (permission == Manifest.permission.RECORD_AUDIO) PackageManager.PERMISSION_DENIED else super.checkSelfPermission(permission)
            }
            val component = ComponentName(testContext.packageName, ScriptedRecognitionService::class.java.name)
            engine = VoiceEngineManager(context, object : VoiceEngineCallback {
                override fun onStateChanged(state: AssistantState) { states.add(state) }
                override fun onSpeechRecognized(text: String) { }
                override fun onCommandParsed(result: ParsedCommandResult) { commands.add(result) }
                override fun onError(errorMessage: String) { errors.add(errorMessage) }
                override fun onFeedback(text: String) { outputs.add(text) }
                override fun onConfirmationChanged(available: Boolean) { confirmation.set(available) }
            }, component, object : SpeechOutput {
                override fun speak(text: String, onComplete: () -> Unit) {
                    if (outputFails) throw IllegalStateException("No installed Serbian TTS voice")
                    Handler(Looper.getMainLooper()).postDelayed(onComplete, outputDelay)
                }
                override fun stop() { /* Deliberately allow a stale completion to test cancellation. */ }
            }, recognitionIntentFactory = {
                val step = queue.poll() ?: RecognitionStep()
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra("script.text", step.text)
                    .putExtra("script.error", step.error ?: -1)
                    .putExtra("script.delay", step.delayMs)
            })
            engine!!.updateContacts(listOf(Contact("Marko Petrović", "061111111"), Contact("Marko Nikolić", "062222222"), Contact("Ana", "063333333", listOf("063333333", "064444444"))))
            engine!!.startListening()
        }
    }
    private fun waitFor(condition: () -> Boolean) { compose.waitUntil(12000, condition) }
    @After fun cleanup() { compose.runOnIdle { focus?.let { compose.activity.getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it) }; engine?.destroy() } }

    @Test fun providerTakingAudioFocusDoesNotCancelListening() {
        start(RecognitionStep())
        waitFor { states.count { it == AssistantState.LISTENING } >= 2 || errors.isNotEmpty() }
        assertTrue(errors.toString(), errors.isEmpty())
        compose.runOnIdle {
            focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                .setOnAudioFocusChangeListener { }.build()
            assertEquals(AudioManager.AUDIOFOCUS_REQUEST_GRANTED, compose.activity.getSystemService(AudioManager::class.java).requestAudioFocus(focus!!))
        }
        Thread.sleep(700)
        compose.runOnIdle { assertEquals(AssistantState.LISTENING, engine!!.currentState) }
        assertTrue(errors.toString(), errors.isEmpty())
    }
    @Test fun callNeedsChoiceAndConfirmationThroughRecognizer() {
        start(RecognitionStep("Pozovi Marka"), RecognitionStep("drugi"), RecognitionStep("potvrdi"))
        waitFor { commands.isNotEmpty() || errors.isNotEmpty() }
        assertTrue(errors.toString(), errors.isEmpty())
        assertEquals(1, commands.size)
        assertEquals("062222222", commands.single().phoneNumber)
    }
    @Test fun smsKeepsTextAndSelectedNumberThroughRecognizer() {
        start(RecognitionStep("Pošalji poruku Ani da stižem"), RecognitionStep("drugi"), RecognitionStep("potvrdi"))
        waitFor { commands.isNotEmpty() || errors.isNotEmpty() }
        assertTrue(errors.toString(), errors.isEmpty())
        assertEquals("064444444", commands.single().phoneNumber)
        assertEquals("Stižem.", commands.single().messageText)
    }
    @Test fun spokenCancellationNeverExecutes() {
        start(RecognitionStep("Pozovi Marka Nikolića"), RecognitionStep("otkaži"))
        waitFor { outputs.contains("Radnja je otkazana.") || errors.isNotEmpty() }
        assertTrue(errors.toString(), errors.isEmpty())
        assertTrue(commands.isEmpty())
    }
    @Test fun networkErrorIsVisibleAndReturnsIdle() {
        start(RecognitionStep(error = SpeechRecognizer.ERROR_NETWORK))
        waitFor { errors.isNotEmpty() }
        assertTrue(errors.single().contains("internet", true))
        compose.runOnIdle { assertEquals(AssistantState.IDLE, engine!!.currentState) }
        assertTrue(commands.isEmpty())
    }
    @Test fun deniedMicrophoneDoesNotEnterListening() {
        start(denied = true)
        waitFor { errors.isNotEmpty() }
        assertTrue(errors.single().contains("Mikrofon"))
        assertFalse(states.contains(AssistantState.LISTENING))
    }
    @Test fun repeatedSilenceTerminatesWithoutExecution() {
        start(RecognitionStep("Pozovi Marka Nikolića"), RecognitionStep(error = SpeechRecognizer.ERROR_SPEECH_TIMEOUT), RecognitionStep(error = SpeechRecognizer.ERROR_SPEECH_TIMEOUT), RecognitionStep(error = SpeechRecognizer.ERROR_SPEECH_TIMEOUT))
        waitFor { errors.isNotEmpty() }
        assertTrue(commands.isEmpty())
        compose.runOnIdle { assertEquals(AssistantState.IDLE, engine!!.currentState) }
    }
    @Test fun cancellingSpeechIgnoresLateCompletion() {
        outputDelay = 1600
        start(RecognitionStep("Pozovi Marka Nikolića"))
        waitFor { outputs.isNotEmpty() || errors.isNotEmpty() }
        assertTrue(errors.toString(), errors.isEmpty())
        compose.runOnIdle { engine!!.cancel() }
        Thread.sleep(1900)
        compose.runOnIdle { assertEquals(AssistantState.IDLE, engine!!.currentState) }
        assertTrue(commands.isEmpty())
    }
    @Test fun missingVoicePreservesCommandUntilExplicitScreenConfirmation() {
        outputFails = true
        start(RecognitionStep("Pozovi Marka Nikolića"))
        waitFor { confirmation.get() || errors.isNotEmpty() }
        assertTrue(errors.toString(), errors.isEmpty())
        assertTrue(commands.isEmpty())
        compose.runOnIdle { assertEquals(AssistantState.IDLE, engine!!.currentState); engine!!.confirmPending() }
        waitFor { commands.isNotEmpty() }
        assertEquals("062222222", commands.single().phoneNumber)
    }
    @Test fun missingVoiceAllowsNextMicrophoneTapToConfirm() {
        outputFails = true
        start(RecognitionStep("Pozovi Marka Nikolića"), RecognitionStep("potvrdi"))
        waitFor { confirmation.get() || errors.isNotEmpty() }
        assertTrue(errors.toString(), errors.isEmpty())
        compose.runOnIdle { engine!!.startListening() }
        waitFor { commands.isNotEmpty() || errors.isNotEmpty() }
        assertTrue(errors.toString(), errors.isEmpty())
        assertEquals("062222222", commands.single().phoneNumber)
    }
}
