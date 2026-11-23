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
        PackManifest(
            id = "landscape-room",
            title = "The Landscape Room",
            description = "Eight works about weather, water and the land — from Hokusai's wave to Bruegel's harvest and Van Gogh's cypresses.",
            period = "World",
            genre = "Landscape",
            quality = PackQuality.THUMBNAIL,
            artworkCount = 8,
            sizeBytes = 2_524_442,
            version = 1,
            downloadUrl = "${RELEASE}landscape-room.zip",
            sha256 = "c9f6e32f315c6c7fbb69c47577a5ae6d44b1407d43b67f8dbca59f8227029eb5",
            releasedAt = 1_763_856_000_000,
            licenseSummary = "Public-domain reproductions via Wikimedia Commons; credit lines included in every record.",
        ),
        PackManifest(
            id = "portraits-presences",
            title = "Portraits & Presences",
            description = "Ten faces and the lives behind them: Sargent, David, Rembrandt, Whistler and the quiet psychology of the portrait.",
            period = "World",
            genre = "Portrait",
            quality = PackQuality.THUMBNAIL,
            artworkCount = 10,
            sizeBytes = 2_698_851,
            version = 1,
            downloadUrl = "${RELEASE}portraits-presences.zip",
            sha256 = "a2eddf5db3c8325c5e44d1449e5cee2cf0294573e41f762a18669febb86591db",
            releasedAt = 1_763_856_000_000,
            licenseSummary = "Public-domain reproductions via Wikimedia Commons; credit lines included in every record.",
        ),
        PackManifest(
            id = "rooms-shadows",
            title = "Rooms & Shadows",
            description = "Seven interiors where light does the talking — Vermeer, Van Gogh, Hopper and the drama of an ordinary room.",
            period = "World",
            genre = "Interior",
            quality = PackQuality.THUMBNAIL,
            artworkCount = 7,
            sizeBytes = 1_688_243,
            version = 1,
            downloadUrl = "${RELEASE}rooms-shadows.zip",
            sha256 = "11539efa9c3cdd5bccb3e9325a3fc719791abbe95b313392ac7faa8cc5b8d0d0",
            releasedAt = 1_763_856_000_000,
            licenseSummary = "Public-domain reproductions via Wikimedia Commons; credit lines included in every record.",
        ),
    )

    fun byId(id: String): PackManifest? = packs.firstOrNull { it.id == id }
}
