package org.libraryofarts.core.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.libraryofarts.core.model.Museum
import org.libraryofarts.core.model.MuseumAudioStop
import org.libraryofarts.core.model.MuseumCatalog
import org.libraryofarts.core.model.NearbyMuseum
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Singleton
class MuseumVisitRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun nearby(latitude: Double, longitude: Double): List<NearbyMuseum> =
        MuseumCatalog.all
            .map { museum -> NearbyMuseum(museum, distanceMeters(latitude, longitude, museum.latitude, museum.longitude)) }
            .filter { it.distanceMeters < 1_500_000 }
            .sortedBy { it.distanceMeters }

    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        return earthRadius * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    fun floorGuide(museum: Museum): String? {
        val path = museum.floorGuideAsset ?: return null
        return runCatching {
            context.assets.open(path).bufferedReader().use { it.readText() }
        }.getOrNull()
    }

    fun audioTour(museum: Museum): List<MuseumAudioStop> = tours[museum.id] ?: defaultTour(museum)

    private fun defaultTour(museum: Museum): List<MuseumAudioStop> = listOf(
        MuseumAudioStop(
            id = "${museum.id}-welcome",
            title = "Welcome to ${museum.shortName}",
            gallery = "Entrance",
            durationSeconds = 45,
            transcript = "Welcome. This short guide is designed for unhurried looking. Choose one work, stand a little closer than usual, and let your eyes adjust before reading anything.",
        ),
    )

    private val tours: Map<String, List<MuseumAudioStop>> = mapOf(
        "met" to listOf(
            MuseumAudioStop(
                "met-great-hall",
                "The Great Hall",
                "Great Hall",
                50,
                "The Met's Great Hall is a threshold: crowds, marble and a view straight into the museum's depth. Notice how the architecture prepares you to look upward and inward at once.",
            ),
            MuseumAudioStop(
                "met-aristotle",
                "Rembrandt's Aristotle",
                "Gallery 616",
                90,
                "Find the philosopher's hand resting on Homer's bust. Ask what it means to touch the past. Rembrandt paints the answer as both reverence and possession.",
            ),
            MuseumAudioStop(
                "met-great-wave",
                "Hokusai's Great Wave",
                "Gallery 232",
                80,
                "Look at Mount Fuji first, then let the wave overtake it. The print is built on a contrast between stillness and motion, not simply on a big wave.",
            ),
        ),
        "rijksmuseum" to listOf(
            MuseumAudioStop(
                "rijks-night-watch",
                "The Night Watch",
                "Gallery 2.5",
                100,
                "Start at the girl in gold. She is the brightest note in a painting about civic pride, and the only figure who looks directly back at us.",
            ),
            MuseumAudioStop(
                "rijks-milkmaid",
                "The Milkmaid",
                "Gallery 2.6",
                80,
                "Vermeer gives a household task the stillness of a still life. Watch the milk, the bread and the nail on the wall: all are painted with the same attention.",
            ),
        ),
        "aic" to listOf(
            MuseumAudioStop(
                "aic-grande-jatte",
                "A Sunday on La Grande Jatte",
                "Gallery 240",
                95,
                "Move close enough to see the dots, then step back until the park assembles itself. Seurat asks you to participate in the act of seeing.",
            ),
            MuseumAudioStop(
                "aic-nighthawks",
                "Nighthawks",
                "Gallery 262",
                85,
                "The diner is lit like an aquarium. Notice that no one looks at anyone else, and that the glass gives us no clear reflection to enter through.",
            ),
        ),
        "getty" to listOf(
            MuseumAudioStop(
                "getty-irises",
                "Van Gogh's Irises",
                "West Pavilion",
                80,
                "Van Gogh painted irises in the asylum garden at Saint-Rémy. The blue-violet flowers and yellow-green ground are complementary colours, each making the other feel more alive.",
            ),
        ),
        "cleveland" to listOf(
            MuseumAudioStop(
                "cleveland-thinker",
                "Rodin's Thinker",
                "Gallery 220",
                70,
                "Walk around the sculpture. From every side the body is doing something different: pressing, twisting, holding. Thought is shown as effort.",
            ),
        ),
    )
}
