package org.libraryofarts.core.model

/**
 * Coordinates for the museum world map. These are facts, not user data.
 */
object MuseumCatalog {
    val met = Museum(
        id = "met",
        name = "The Metropolitan Museum of Art",
        shortName = "The Met",
        city = "New York",
        country = "United States",
        latitude = 40.7794,
        longitude = -73.9632,
        website = "https://www.metmuseum.org",
        floorGuideAsset = "guides/met.txt",
    )
    val aic = Museum(
        id = "aic",
        name = "Art Institute of Chicago",
        shortName = "Art Institute",
        city = "Chicago",
        country = "United States",
        latitude = 41.8796,
        longitude = -87.6237,
        website = "https://www.artic.edu",
        floorGuideAsset = "guides/aic.txt",
    )
    val rijksmuseum = Museum(
        id = "rijksmuseum",
        name = "Rijksmuseum",
        shortName = "Rijksmuseum",
        city = "Amsterdam",
        country = "Netherlands",
        latitude = 52.3600,
        longitude = 4.8852,
        website = "https://www.rijksmuseum.nl",
        floorGuideAsset = "guides/rijksmuseum.txt",
    )
    val cleveland = Museum(
        id = "cleveland",
        name = "Cleveland Museum of Art",
        shortName = "Cleveland",
        city = "Cleveland",
        country = "United States",
        latitude = 41.5089,
        longitude = -81.6114,
        website = "https://www.clevelandart.org",
        floorGuideAsset = "guides/cleveland.txt",
    )
    val smithsonian = Museum(
        id = "smithsonian",
        name = "Smithsonian Institution",
        shortName = "Smithsonian",
        city = "Washington, D.C.",
        country = "United States",
        latitude = 38.8888,
        longitude = -77.0260,
        website = "https://www.si.edu",
        floorGuideAsset = "guides/smithsonian.txt",
    )
    val nga = Museum(
        id = "nga",
        name = "National Gallery of Art",
        shortName = "National Gallery",
        city = "Washington, D.C.",
        country = "United States",
        latitude = 38.8913,
        longitude = -77.0199,
        website = "https://www.nga.gov",
        floorGuideAsset = "guides/nga.txt",
    )
    val europeana = Museum(
        id = "europeana",
        name = "Europeana",
        shortName = "Europeana",
        city = "The Hague",
        country = "Netherlands",
        latitude = 52.0800,
        longitude = 4.3100,
        website = "https://www.europeana.eu",
    )
    val harvard = Museum(
        id = "harvard",
        name = "Harvard Art Museums",
        shortName = "Harvard",
        city = "Cambridge",
        country = "United States",
        latitude = 42.3744,
        longitude = -71.1143,
        website = "https://harvardartmuseums.org",
        floorGuideAsset = "guides/harvard.txt",
    )
    val parisMusees = Museum(
        id = "paris-musees",
        name = "Paris Musées",
        shortName = "Paris Musées",
        city = "Paris",
        country = "France",
        latitude = 48.8606,
        longitude = 2.3376,
        website = "https://www.parismusees.paris.fr",
        floorGuideAsset = "guides/paris-musees.txt",
    )
    val getty = Museum(
        id = "getty",
        name = "The J. Paul Getty Museum",
        shortName = "Getty",
        city = "Los Angeles",
        country = "United States",
        latitude = 34.0780,
        longitude = -118.4740,
        website = "https://www.getty.edu/art",
        floorGuideAsset = "guides/getty.txt",
    )
    val wikimedia = Museum(
        id = "wikimedia",
        name = "Wikimedia Commons",
        shortName = "Wikimedia",
        city = "Worldwide",
        country = "Worldwide",
        latitude = 52.5208,
        longitude = 13.4094,
        website = "https://commons.wikimedia.org",
    )

    val googleArts = Museum(
        id = "google-arts",
        name = "Google Arts & Culture",
        shortName = "Google Arts",
        city = "Worldwide",
        country = "Online",
        latitude = 37.4220,
        longitude = -122.0841,
        website = "https://artsandculture.google.com",
    )

    val all: List<Museum> = listOf(
        met, aic, rijksmuseum, cleveland, smithsonian, nga,
        europeana, harvard, parisMusees, getty, wikimedia, googleArts,
    )

    fun byId(id: String): Museum = all.firstOrNull { it.id == id } ?: europeana
    fun bySource(source: ArtSource): Museum = when (source) {
        ArtSource.MET -> met
        ArtSource.ART_INSTITUTE -> aic
        ArtSource.RIJKSMUSEUM -> rijksmuseum
        ArtSource.CLEVELAND -> cleveland
        ArtSource.SMITHSONIAN -> smithsonian
        ArtSource.NATIONAL_GALLERY -> nga
        ArtSource.EUROPEANA -> europeana
        ArtSource.HARVARD -> harvard
        ArtSource.PARIS_MUSEES -> parisMusees
        ArtSource.GETTY -> getty
        ArtSource.WIKIMEDIA -> wikimedia
        ArtSource.GOOGLE_ARTS -> googleArts
        ArtSource.OFFLINE_PACK -> met
    }
}
