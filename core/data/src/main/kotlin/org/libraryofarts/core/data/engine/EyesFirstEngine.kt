package org.libraryofarts.core.data.engine

import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.model.EyesFirstComparison

/**
 * "Your Eyes First": a warm comparison between what the viewer wrote and the
 * art-historical reading. The primary path is an optional on-device language
 * model installed by the user (see [OnDeviceLanguageModel]); when no model is
 * present this deterministic, rule-based engine runs. Nothing is uploaded.
 */
class EyesFirstEngine(
    private val model: OnDeviceLanguageModel? = null,
) {
    private val stopWords = setOf(
        "the", "and", "that", "this", "with", "from", "have", "has", "was", "were",
        "are", "for", "but", "not", "you", "your", "its", "it's", "they", "their",
        "there", "here", "what", "when", "which", "into", "like", "just", "very",
        "look", "looks", "see", "seems", "feel", "feels", "about", "because", "could",
        "would", "should", "really", "more", "than", "then", "also", "some", "such",
    )

    suspend fun compare(userText: String, artwork: Artwork): EyesFirstComparison {
        val generated = model?.takeIf { it.isAvailable() }?.generate(buildPrompt(userText, artwork))
        val rule = ruleBased(userText, artwork)
        return if (generated != null) {
            rule.copy(encouragement = generated, usedOnDeviceModel = true)
        } else {
            rule
        }
    }

    private fun ruleBased(userText: String, artwork: Artwork): EyesFirstComparison {
        val userTokens = tokens(userText)
        val artText = buildString {
            append(artwork.description.orEmpty()).append(' ')
            append(artwork.title).append(' ')
            append(artwork.tags.joinToString(" ")).append(' ')
            append(artwork.annotations.joinToString(" ") { "${it.title} ${it.body}" })
            append(artwork.medium.orEmpty())
        }
        val artTokens = tokens(artText)
        val overlap = userTokens.intersect(artTokens)
        val significantOverlap = overlap.filter { it.length > 4 }.take(6)

        val matches = significantOverlap.map { word ->
            val annotation = artwork.annotations.firstOrNull { annotation ->
                annotation.title.contains(word, true) || annotation.body.contains(word, true)
            }
            if (annotation != null) {
                "You noticed “$word” — the same detail curators highlight: ${annotation.title}."
            } else {
                "You noticed “$word”, which the art-historical reading also draws attention to."
            }
        }.toMutableList()

        artwork.annotations.take(1).forEach { annotation ->
            if (matches.size < 4 && userTokens.any { annotation.title.contains(it, true) }) {
                matches += "Your attention to “${annotation.title}” matches a curated note: ${annotation.body}"
            }
        }

        val original = tokens(userText)
            .filterNot { it in artTokens }
            .filterNot { it in stopWords }
            .distinct()
            .take(4)
            .map { "“$it” is your own lens — the museum text doesn't use that word, and that's worth keeping." }

        val encouragement = when {
            userText.isBlank() -> "There is no wrong way to begin. Write one thing you notice, and the looking will follow."
            matches.isNotEmpty() && original.isNotEmpty() ->
                "You combined close observation with a personal reading. That is exactly what art historians do: look first, then argue."
            matches.isNotEmpty() ->
                "You saw several things the curators also noticed. Your eye is already tuned to this picture's quiet signals."
            original.isNotEmpty() ->
                "You brought a fresh vocabulary to this work. Original noticing is not a mistake; it is how new readings begin."
            else ->
                "You stayed with the work long enough to put it into words. That is the whole practice."
        }

        val reading = artwork.description?.takeIf { it.isNotBlank() }
            ?: "Curators read this work through its ${artwork.genre?.lowercase() ?: "form"}, its ${artwork.period ?: "historical moment"} and the choices of ${artwork.displayArtist}."

        val score = (matches.size * 18 + original.size * 9 + if (userText.length > 120) 12 else 4)
            .coerceIn(10, 100)

        return EyesFirstComparison(
            matches = matches.take(4),
            originalInsights = original,
            encouragement = encouragement,
            artHistoricalReading = reading,
            score = score,
            usedOnDeviceModel = false,
        )
    }

    private fun buildPrompt(userText: String, artwork: Artwork): String = buildString {
        appendLine("You are a warm, precise art educator. The viewer has not yet seen the official label.")
        appendLine("Artwork: ${artwork.title} by ${artwork.displayArtist}, ${artwork.year}.")
        appendLine("Curatorial description: ${artwork.description.orEmpty()}")
        appendLine("Viewer's interpretation: $userText")
        appendLine("In 2 short sentences, name what they noticed that matches the art-historical reading and celebrate one original insight. Never grade or shame.")
    }

    private fun tokens(text: String): Set<String> = text
        .lowercase()
        .replace(Regex("[^a-zà-ÿ0-9'\\-\\s]"), " ")
        .split(Regex("\\s+"))
        .map { it.trim('\'', '-') }
        .filter { it.length > 2 && it !in stopWords }
        .toSet()
}

interface OnDeviceLanguageModel {
    fun isAvailable(): Boolean
    suspend fun generate(prompt: String): String?
}

/**
 * Checks for a user-installed local model pack. Models are large, so they are
 * optional downloads; the app never ships or phones home with one.
 */
class LocalModelPack(private val modelDirectory: java.io.File) : OnDeviceLanguageModel {
    override fun isAvailable(): Boolean = modelDirectory.listFiles()
        ?.any { it.name.endsWith(".task") || it.name.endsWith(".gguf") } == true

    override suspend fun generate(prompt: String): String? = null
}
