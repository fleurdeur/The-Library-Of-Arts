package org.libraryofarts.core.model

import kotlinx.serialization.Serializable

/**
 * An offline art pack is a single zip archive containing a manifest, a
 * JSON-lines artwork index and an image tree. The manifest is signed by the
 * publisher; the app verifies both the checksum and the Ed25519 signature
 * before a single byte is unpacked.
 */
@Serializable
data class PackManifest(
    val schemaVersion: Int = 1,
    val id: String,
    val title: String,
    val description: String = "",
    val period: String = "",
    val genre: String = "",
    val quality: PackQuality = PackQuality.STANDARD,
    val artworkCount: Int = 0,
    val sizeBytes: Long = 0,
    val version: Int = 1,
    val downloadUrl: String,
    val sha256: String,
    val signature: String? = null,
    val signatureKeyId: String? = null,
    val releasedAt: Long = 0,
    val delta: PackDelta? = null,
    val artworksPath: String = "artworks.jsonl",
    val imagesDir: String = "images",
    val licenseSummary: String = "Public domain and openly licensed works only.",
    val cdnBaseUrl: String = "",
) {
    val qualityLabel: String get() = quality.label
}

@Serializable
data class PackDelta(
    val fromVersion: Int,
    val toVersion: Int,
    val url: String,
    val sizeBytes: Long,
    val sha256: String,
    val signature: String? = null,
)

@Serializable
enum class PackQuality(val label: String, val approxBytesPerImage: Long) {
    THUMBNAIL("Thumbnail (500 MB class)", 250_000),
    STANDARD("Standard (1–2 GB class)", 1_200_000),
    GALLERY("Gallery (3–5 GB class)", 2_800_000),
    LOSSLESS("Lossless masters (largest)", 6_000_000),
}

@Serializable
data class InstalledPack(
    val id: String,
    val title: String,
    val version: Int,
    val period: String,
    val genre: String,
    val quality: PackQuality,
    val artworkCount: Int,
    val sizeBytes: Long,
    val installedAt: Long,
    val localPath: String,
    val checksumVerified: Boolean,
    val signatureVerified: Boolean,
    val deltaApplied: Boolean = false,
) {
    val sizeLabel: String
        get() = when {
            sizeBytes >= 1L * 1024 * 1024 * 1024 -> "%.1f GB".format(sizeBytes / 1024.0 / 1024.0 / 1024.0)
            else -> "%.0f MB".format(sizeBytes / 1024.0 / 1024.0)
        }
}

@Serializable
enum class PackDownloadState {
    IDLE,
    QUEUED,
    DOWNLOADING,
    VERIFYING,
    INSTALLING,
    COMPLETE,
    FAILED,
    PAUSED,
    NEEDS_WIFI,
    NOT_ENOUGH_SPACE,
    CANCELLED,
}

@Serializable
data class PackDownloadProgress(
    val packId: String,
    val state: PackDownloadState,
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = 0,
    val speedBytesPerSecond: Long = 0,
    val error: String? = null,
    val resumable: Boolean = true,
) {
    val fraction: Float
        get() = if (totalBytes > 0) (bytesDownloaded.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
}
