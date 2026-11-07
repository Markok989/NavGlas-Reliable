package com.example.naviglas.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager

/** Uses Android's selected audio route instead of forcing SCO or speakerphone. */
class AudioRoutingManager(context: Context) {
    private val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var request: AudioFocusRequest? = null
    private var held = false
    private var onLoss: () -> Unit = {}
    fun requestAudioFocus(onFocusLoss: () -> Unit = {}): Boolean {
        onLoss = onFocusLoss
        if (held) return true
        val focus = request ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setAcceptsDelayedFocusGain(false)
            .setOnAudioFocusChangeListener { change ->
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    held = false
                    onLoss()
                }
            }.build().also { request = it }
        held = manager.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return held
    }
    fun abandonAudioFocus() { request?.let { manager.abandonAudioFocusRequest(it) }; held = false }
    // Compatibility for the unused cloud voice adapter; route selection belongs to Android.
    fun enableCarAudioMode(preferBluetooth: Boolean = true) { }
    fun disableCarAudioMode() { abandonAudioFocus() }
}

