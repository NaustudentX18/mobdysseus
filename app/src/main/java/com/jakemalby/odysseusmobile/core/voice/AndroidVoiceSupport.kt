package com.jakemalby.odysseusmobile.core.voice

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

fun detectOfflineDictationAvailability(context: Context): OfflineDictationAvailability {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
    if (intent.resolveActivity(context.packageManager) == null) {
        return OfflineDictationAvailability.UNAVAILABLE
    }
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
    ) {
        OfflineDictationAvailability.ON_DEVICE_AVAILABLE
    } else {
        OfflineDictationAvailability.OFFLINE_REQUESTED
    }
}

enum class SpeechPlaybackState {
    INITIALIZING,
    READY,
    SPEAKING,
    UNAVAILABLE,
}

/**
 * Owns TTS and transient audio focus. Calls and other focus owners stop playback instead of
 * allowing assistant speech to continue over them.
 */
class SafeTextSpeaker(
    context: Context,
    private val onStateChanged: (SpeechPlaybackState) -> Unit,
) : AutoCloseable {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (change < 0) stop()
    }
    private val focusRequest: AudioFocusRequest? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setOnAudioFocusChangeListener(focusListener)
            .setWillPauseWhenDucked(true)
            .build()
    } else {
        null
    }
    private var ready = false
    private var closed = false
    private val tts = TextToSpeech(context.applicationContext) { result ->
        ready = result == TextToSpeech.SUCCESS
        onStateChanged(if (ready) SpeechPlaybackState.READY else SpeechPlaybackState.UNAVAILABLE)
    }.apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = onStateChanged(SpeechPlaybackState.SPEAKING)
            override fun onDone(utteranceId: String?) {
                abandonAudioFocus()
                onStateChanged(SpeechPlaybackState.READY)
            }
            @Deprecated("Deprecated by Android")
            override fun onError(utteranceId: String?) {
                abandonAudioFocus()
                onStateChanged(SpeechPlaybackState.READY)
            }
        })
    }

    fun speak(text: String, utteranceId: String): Boolean {
        if (closed || !ready || !VoiceDraftPolicy.canSpeak(text) || !requestAudioFocus()) return false
        val languageResult = tts.setLanguage(Locale.getDefault())
        if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
            languageResult == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            abandonAudioFocus()
            onStateChanged(SpeechPlaybackState.UNAVAILABLE)
            return false
        }
        return tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId) == TextToSpeech.SUCCESS
    }

    fun stop() {
        if (closed) return
        tts.stop()
        abandonAudioFocus()
        onStateChanged(if (ready) SpeechPlaybackState.READY else SpeechPlaybackState.UNAVAILABLE)
    }

    override fun close() {
        if (closed) return
        stop()
        closed = true
        tts.shutdown()
    }

    private fun requestAudioFocus(): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(requireNotNull(focusRequest))
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                focusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
            )
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let(audioManager::abandonAudioFocusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusListener)
        }
    }
}
