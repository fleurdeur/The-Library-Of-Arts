package org.libraryofarts.core.media

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Text-to-speech narration for story tours and museum guides. This is a safe,
 * offline-friendly fallback when a museum does not provide recorded audio.
 */
@Singleton
class Narrator @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var tts: TextToSpeech? = null
    private var ready = false

    fun prepare() {
        if (tts != null) return
        tts = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) tts?.language = Locale.getDefault()
        }
    }

    fun speak(text: String, flush: Boolean = true) {
        prepare()
        if (!ready) return
        tts?.speak(text, if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, "loa-narration")
    }

    fun stop() {
        tts?.stop()
    }

    fun release() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}
