package com.example.naviglas.testsupport

import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionService
import android.speech.SpeechRecognizer
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/** Supplies results through Android's SpeechRecognizer binder; never records audio. */
class ScriptedRecognitionService : RecognitionService() {
    private val handler = Handler(Looper.getMainLooper())
    private var focus: AudioFocusRequest? = null
    override fun onStartListening(intent: Intent, callback: Callback) {
        starts.incrementAndGet()
        val step = Step(intent.getStringExtra("script.text"), intent.getIntExtra("script.error", -1).takeIf { it >= 0 }, intent.getLongExtra("script.delay", 200))
        val manager = getSystemService(AudioManager::class.java)
        // Audio focus is taken by the foreground test client, not this test-only process.
        callback.readyForSpeech(Bundle())
        if (step.text != null || step.error != null) handler.postDelayed({
            focus?.let { manager.abandonAudioFocusRequest(it) }; focus = null
            try {
                if (step.error != null) callback.error(step.error)
                else callback.results(Bundle().apply { putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf(step.text!!)) })
            } catch (_: Exception) { }
        }, step.delayMs)
    }
    override fun onCancel(callback: Callback) { cleanup() }
    override fun onStopListening(callback: Callback) { cleanup(); callback.error(SpeechRecognizer.ERROR_NO_MATCH) }
    override fun onDestroy() { cleanup(); super.onDestroy() }
    private fun cleanup() {
        handler.removeCallbacksAndMessages(null)
        focus?.let { getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it) }
        focus = null
    }
    data class Step(val text: String? = null, val error: Int? = null, val delayMs: Long = 200)
    companion object {
        val steps = ConcurrentLinkedQueue<Step>()
        val starts = AtomicInteger()
        @Volatile var focusResult = AudioManager.AUDIOFOCUS_REQUEST_FAILED
        fun reset() { steps.clear(); starts.set(0); focusResult = AudioManager.AUDIOFOCUS_REQUEST_FAILED }
    }
}

