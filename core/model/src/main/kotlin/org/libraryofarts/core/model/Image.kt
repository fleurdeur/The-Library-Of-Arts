package org.libraryofarts.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ArtworkImage(
    val thumbnailUrl: String,
    val fullUrl: String,
    val width: Int,
    val height: Int,
    val iiifBase: String? = null,
    val localPath: String? = null,
    val creditLine: String = "",
    val altText: String = "",
) {
    val aspectRatio: Float
        get() = if (height > 0) width.toFloat() / height.toFloat() else 1f

    /** Builds an IIIF Image API region request when the source supports it. */
    fun iiifRegion(
        x: Int,
        y: Int,
        regionWidth: Int,
        regionHeight: Int,
        outputWidth: Int,
    ): String? = iiifBase?.let { base ->
        val clean = base.trimEnd('/')
        "$clean/$x,$y,$regionWidth,$regionHeight/$outputWidth,/0/default.jpg"
    }
}

@Serializable
data class PaletteColor(
    val argb: Long,
    val hex: String,
    val population: Float = 0f,
    val name: String = "",
)

@Serializable
data class ArtworkLayer(
    val kind: LayerKind,
    val label: String,
    val imageUrl: String,
    val description: String = "",
)

@Serializable
enum class LayerKind(val label: String) {
    VISIBLE_LIGHT("Visible light"),
    X_RAY("X-ray"),
    INFRARED("Infrared"),
    BEFORE_RESTORATION("Before restoration"),
    AFTER_RESTORATION("After restoration"),
}

@Serializable
data class ArtworkAnnotation(
    val id: String,
    val x: Float,
    val y: Float,
    val title: String,
    val body: String,
    val kind: AnnotationKind = AnnotationKind.DETAIL,
)

@Serializable
enum class AnnotationKind(val label: String) {
    SYMBOL("Symbol"),
    EASTER_EGG("Hidden detail"),
    RESTORATION("Restoration note"),
    DETAIL("Detail"),
}

@Serializable
data class AudioTrack(
    val id: String,
    val title: String,
    val url: String,
    val durationMs: Long = 0,
    val kind: AudioKind = AudioKind.COMMENTARY,
    val transcript: String = "",
)

@Serializable
enum class AudioKind(val label: String) {
    COMMENTARY("Curator commentary"),
    MUSIC("Related music"),
    TOUR("Story tour"),
    DESCRIPTION("Audio description"),
}
