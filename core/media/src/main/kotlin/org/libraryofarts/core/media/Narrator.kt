package org.libraryofarts.core.media

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Narration for stories, artwork notes and museum stops.
 *
 * Android TTS defaults are often a low-quality, fast, robotic voice. This
 * wrapper picks the best installed English voice, slows the delivery slightly
 * and speaks sentence by sentence with a short breath between them so the
 * result sounds like a gallery audio guide rather than a notification reader.
 */
@Singleton
class Narrator @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var chosenVoice: String? = null

    fun prepare() {
        if (tts != null) return
        tts = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (!ready) return@TextToSpeech
            val engine = tts ?: return@TextToSpeech
            runCatching {
                engine.language = Locale.US
                engine.setSpeechRate(0.91f)
                engine.setPitch(0.98f)
                engine.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                bestVoice(engine)?.let { voice ->
                    engine.voice = voice
                    chosenVoice = voice.name
                }
            }
        }
    }

    private fun bestVoice(engine: TextToSpeech): Voice? {
        val voices = runCatching { engine.voices }.getOrNull().orEmpty()
        if (voices.isEmpty()) return null
        return voices
            .asSequence()
            .filter { it.locale.language.equals("en", true) }
            .filterNot { it.name.contains("robot", true) || it.name.contains("compact", true) }
            .map { voice ->
                val name = voice.name.lowercase(Locale.US)
                val quality = voice.quality.coerceAtLeast(0)
                val latency = voice.latency.coerceAtLeast(0)
                val premium = when {
                    name.contains("neural") || name.contains("wavenet") -> 42
                    name.contains("network") || name.contains("enhanced") -> 30
                    name.contains("google") -> 18
                    name.contains("samsung") -> 12
                    else -> 0
                }
                val region = when (voice.locale.country.uppercase(Locale.US)) {
                    "US" -> 14
                    "GB" -> 10
                    "AU", "CA", "IE", "NZ" -> 6
                    else -> 0
                }
                voice to (premium + quality * 10 + region - latency)
            }
            .maxByOrNull { it.second }
            ?.first
    }

    fun speak(text: String, flush: Boolean = true) {
        prepare()
        if (!ready) return
        val engine = tts ?: return
        val clean = text
            .replace(Regex("\\s+"), " ")
            .trim()
        if (clean.isBlank()) return
        if (flush) engine.stop()
        val sentences = clean
            .split(Regex("(?<=[.!?])\\s+"))
            .filter { it.isNotBlank() }
            .ifEmpty { listOf(clean) }
        val utterance = "loa-narration-${System.nanoTime()}"
        sentences.forEachIndexed { index, sentence ->
            if (flush || index > 0) {
                engine.playSilentUtterance(if (index == 0) 90L else 185L, TextToSpeech.QUEUE_ADD, "$utterance-gap-$index")
            }
            engine.speak(
                sentence,
                if (flush && index == 0) TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_ADD,
                null,
                "$utterance-$index",
            )
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun release() {
        tts?.shutdown()
        tts = null
        ready = false
        chosenVoice = null
    }
}
