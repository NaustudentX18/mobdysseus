package com.jakemalby.odysseusmobile.core.voice

enum class OfflineDictationAvailability(val label: String) {
    ON_DEVICE_AVAILABLE("On-device speech recognition is available"),
    OFFLINE_REQUESTED("Offline recognition requested; Android may still use its configured speech service"),
    UNAVAILABLE("Speech recognition is unavailable on this device"),
}

/** Dictation is always staged as editable text. Sending remains a separate user action. */
object VoiceDraftPolicy {
    fun appendTranscript(existingDraft: String, transcript: String): String =
        listOf(existingDraft.trim(), transcript.trim())
            .filter(String::isNotBlank)
            .joinToString(" ")

    fun canSpeak(text: String): Boolean = text.isNotBlank()
}

