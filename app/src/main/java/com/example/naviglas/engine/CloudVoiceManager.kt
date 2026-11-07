package com.example.naviglas.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class CloudVoiceManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val audioRoutingManager = AudioRoutingManager(context)

    /**
     * Sintetizuje prirodan ljudski glas preko Cloud Neural/Gemini TTS servisa.
     */
    suspend fun speakNaturalCloudVoice(
        text: String,
        apiKey: String? = null,
        onStart: () -> Unit = {},
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (apiKey.isNullOrBlank()) {
            onError("Nije podešen API ključ za Gemini/Cloud Neural glas. Koristi se lokalni sintetičar.")
            return
        }

        withContext(Dispatchers.IO) {
            try {
                audioRoutingManager.requestAudioFocus()
                audioRoutingManager.enableCarAudioMode()

                val url = URL("https://texttospeech.googleapis.com/v1/text:synthesize?key=$apiKey")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.doOutput = true

                val jsonInput = JSONObject().apply {
                    put("input", JSONObject().put("text", text))
                    put("voice", JSONObject().apply {
                        put("languageCode", "sr-RS")
                        put("name", "sr-RS-Wavenet-A")
                    })
                    put("audioConfig", JSONObject().apply {
                        put("audioEncoding", "MP3")
                        put("speakingRate", 0.95)
                        put("pitch", 0.0)
                    })
                }

                conn.outputStream.use { os ->
                    os.write(jsonInput.toString().toByteArray(Charsets.UTF_8))
                }

                if (conn.responseCode == 200) {
                    val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonResponse = JSONObject(responseText)
                    val audioContentBase64 = jsonResponse.optString("audioContent")

                    if (!audioContentBase64.isNullOrBlank()) {
                        val audioBytes = Base64.decode(audioContentBase64, Base64.DEFAULT)
                        val tempFile = File(context.cacheDir, "gemini_voice.mp3")
                        FileOutputStream(tempFile).use { fos ->
                            fos.write(audioBytes)
                        }

                        withContext(Dispatchers.Main) {
                            playAudioFile(tempFile, onStart, onComplete)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            onError("Prazan zvučni zapis od Cloud servisa.")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onError("Greška pri povezivanju na Cloud Neural glas (Kod ${conn.responseCode}).")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("Greška: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun playAudioFile(file: File, onStart: () -> Unit, onComplete: () -> Unit) {
        stop()
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(file.absolutePath)
                setOnPreparedListener {
                    onStart()
                    start()
                }
                setOnCompletionListener {
                    audioRoutingManager.abandonAudioFocus()
                    onComplete()
                }
                setOnErrorListener { _, _, _ ->
                    audioRoutingManager.abandonAudioFocus()
                    onComplete()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete()
        }
    }

    fun stop() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
