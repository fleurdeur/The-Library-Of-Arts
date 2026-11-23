package org.libraryofarts.core.data.pack

import org.libraryofarts.core.model.PackManifest
import org.libraryofarts.core.model.PackQuality

/**
 * Published offline packs. The archives and their SHA-256 values are attached
 * to the public `packs-v1` GitHub release, so the catalogue works for anyone
 * who installs the app without any extra configuration. Every pack contains a
 * JSONL artwork index plus public-domain image files; the installer verifies
 * the checksum before unpacking and rejects unsafe paths.
 */
object PackCatalog {
    private const val RELEASE = "https://github.com/fleurdeur/The-Library-Of-Arts/releases/download/packs-v1/"

    val packs: List<PackManifest> = listOf(
        PackManifest(
            id = "starter-salon",
            title = "Starter Salon",
            description = "Twenty-three public-domain touchstones, from Bruegel's harvest to Van Gogh's cypresses. A complete first gallery that works with no signal.",
            period = "World",
            genre = "Mixed",
            quality = PackQuality.THUMBNAIL,
            artworkCount = 23,
            sizeBytes = 6_392_312,
            version = 1,
            downloadUrl = "${RELEASE}starter-salon.zip",
            sha256 = "ce9be93084f762f2f0c5d9449b6861111a6497ea8b3a9c2fc23f3d7f2fe63c10",
            releasedAt = 1_763_856_000_000,
            licenseSummary = "Public-domain reproductions via Wikimedia Commons; credit lines included in every record.",
        ),
        PackManifest(
            id = "impressionism",
            title = "Impressionism & After",
            description = "Monet, Renoir, Cassatt, Seurat and Van Gogh — light, weather and modern life in six close-looking plates.",
            period = "Impressionism",
            genre = "Painting",
            quality = PackQuality.THUMBNAIL,
            artworkCount = 6,
            sizeBytes = 2_178_013,
            version = 1,
            downloadUrl = "${RELEASE}impressionism.zip",
            sha256 = "0829834ca0f53b78dfccc6ec073892ebef35696dfa7484603819c1cd47a7244e",
            releasedAt = 1_763_856_000_000,
            licenseSummary = "Public-domain reproductions via Wikimedia Commons; credit lines included in every record.",
        ),
        PackManifest(
            id = "dutch-golden-age",
            title = "Dutch Golden Age",
            description = "Rembrandt, Vermeer and the quiet drama of the Dutch interior — seven works for slow, low-light looking.",
            period = "Baroque",
            genre = "Painting",
            quality = PackQuality.THUMBNAIL,
            artworkCount = 7,
            sizeBytes = 1_962_309,
            version = 1,
            downloadUrl = "${RELEASE}dutch-golden-age.zip",
            sha256 = "80bb1914409134da194140638f7524ebb4b9e64c5c9b560a0b1ccbff864dfce2",
            releasedAt = 1_763_856_000_000,
            licenseSummary = "Public-domain reproductions via Wikimedia Commons; credit lines included in every record.",
        ),
    )

    fun byId(id: String): PackManifest? = packs.firstOrNull { it.id == id }
}
