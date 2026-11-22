package org.libraryofarts.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Collection(
    val id: String,
    val name: String,
    val description: String = "",
    val artworkIds: List<String> = emptyList(),
    val coverArtworkId: String? = null,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
    val isFavorites: Boolean = false,
    val tags: List<String> = emptyList(),
)

@Serializable
data class ExportableCollection(
    val schemaVersion: Int = CURRENT_SCHEMA,
    val app: String = "The Great Library of Arts",
    val exportedAt: Long,
    val collection: Collection,
    val artworks: List<Artwork>,
) {
    companion object {
        const val CURRENT_SCHEMA = 1
        const val MIME = "application/vnd.libraryofarts.collection+json"
        const val EXTENSION = "loa-collection"
    }
}
