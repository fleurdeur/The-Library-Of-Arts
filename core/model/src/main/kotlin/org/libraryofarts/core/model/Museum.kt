package org.libraryofarts.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Museum(
    val id: String,
    val name: String,
    val shortName: String,
    val city: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val website: String,
    val floorGuideAsset: String? = null,
    val audioTour: List<MuseumAudioStop> = emptyList(),
    val collectionHighlights: List<String> = emptyList(),
) {
    val coordinates: String get() = "%.4f, %.4f".format(latitude, longitude)
}

@Serializable
data class MuseumAudioStop(
    val id: String,
    val title: String,
    val gallery: String,
    val durationSeconds: Int,
    val transcript: String = "",
    val audioUrl: String? = null,
)

@Serializable
data class NearbyMuseum(
    val museum: Museum,
    val distanceMeters: Double,
) {
    val distanceLabel: String
        get() = if (distanceMeters < 1_000) {
            "${distanceMeters.toInt()} m away"
        } else {
            "%.1f km away".format(distanceMeters / 1_000)
        }
}
