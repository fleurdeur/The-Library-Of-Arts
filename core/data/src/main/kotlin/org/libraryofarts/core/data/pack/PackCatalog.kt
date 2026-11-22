package org.libraryofarts.core.data.pack

import org.libraryofarts.core.common.BuildInfo
import org.libraryofarts.core.model.PackManifest
import org.libraryofarts.core.model.PackQuality

/**
 * The public pack catalogue. Pack archives live on your own CDN — Play Asset
 * Delivery's limits make it unsuitable for 500 MB–5 GB art packs. Replace the
 * example CDN with your own and publish signed manifests alongside the files.
 *
 * A publisher produces a pack with:
 *   manifest.json, artworks.jsonl, images/<sourceId>.jpg, scan-index.json
 * then signs the manifest and publishes its SHA-256. See README → Offline packs.
 */
object PackCatalog {
    private const val CDN = BuildInfo.DEFAULT_CDN_BASE

    val packs: List<PackManifest> = listOf(
        PackManifest(
            id = "renaissance-masters",
            title = "Renaissance Masters",
            description = "Leonardo, Botticelli and their contemporaries, with deep-zoom images and curated details.",
            period = "Renaissance",
            genre = "Painting",
            quality = PackQuality.GALLERY,
            artworkCount = 420,
            sizeBytes = 2_400_000_000,
            version = 3,
            downloadUrl = "${CDN}renaissance-masters-v3.zip",
            sha256 = "0000000000000000000000000000000000000000000000000000000000000000",
            releasedAt = 1_760_000_000_000,
            licenseSummary = "Public domain works; museum credit lines included in every record.",
        ),
        PackManifest(
            id = "impressionism",
            title = "Impressionism & After",
            description = "Monet, Renoir, Degas, Cassatt, Van Gogh and Seurat — light, colour and modern life.",
            period = "Impressionism",
            genre = "Painting",
            quality = PackQuality.STANDARD,
            artworkCount = 680,
            sizeBytes = 1_600_000_000,
            version = 5,
            downloadUrl = "${CDN}impressionism-v5.zip",
            sha256 = "0000000000000000000000000000000000000000000000000000000000000000",
            releasedAt = 1_760_000_000_000,
            licenseSummary = "Public domain and CC0 reproductions.",
        ),
        PackManifest(
            id = "dutch-golden-age",
            title = "Dutch Golden Age",
            description = "Rembrandt, Vermeer and the quiet drama of the Dutch interior.",
            period = "Baroque",
            genre = "Painting",
            quality = PackQuality.LOSSLESS,
            artworkCount = 240,
            sizeBytes = 4_600_000_000,
            version = 2,
            downloadUrl = "${CDN}dutch-golden-age-v2.zip",
            sha256 = "0000000000000000000000000000000000000000000000000000000000000000",
            releasedAt = 1_760_000_000_000,
            licenseSummary = "Public domain. High-resolution masters for brushstroke study.",
        ),
        PackManifest(
            id = "world-gems",
            title = "World Gems",
            description = "A compact world tour: Japan, Europe, North America and beyond.",
            period = "World",
            genre = "Mixed",
            quality = PackQuality.THUMBNAIL,
            artworkCount = 900,
            sizeBytes = 520_000_000,
            version = 1,
            downloadUrl = "${CDN}world-gems-v1.zip",
            sha256 = "0000000000000000000000000000000000000000000000000000000000000000",
            releasedAt = 1_760_000_000_000,
            licenseSummary = "Public domain and openly licensed works only.",
        ),
    )

    fun byId(id: String): PackManifest? = packs.firstOrNull { it.id == id }
}
