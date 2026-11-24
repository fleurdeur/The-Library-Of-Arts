package org.libraryofarts.core.model

import kotlinx.serialization.Serializable

@Serializable
data class StoryTour(
    val id: String,
    val title: String,
    val subtitle: String,
    val artworkIds: List<String>,
    val stops: List<TourStop>,
    val minutes: Int,
    val coverUrl: String = "",
)

@Serializable
data class TourStop(
    val artworkId: String,
    val title: String,
    val narration: String,
    val audioUrl: String? = null,
    val seconds: Int = 60,
)

@Serializable
data class Lesson(
    val id: String,
    val title: String,
    val category: LessonCategory,
    val minutes: Int,
    val summary: String,
    val sections: List<LessonSection>,
    val relatedMovement: String? = null,
)

@Serializable
enum class LessonCategory(val label: String) {
    MOVEMENT("Movements"),
    TECHNIQUE("Techniques"),
    READING("How to read a painting"),
    MATERIALS("Materials"),
    HISTORY("Art history"),
}

@Serializable
data class LessonSection(
    val heading: String,
    val body: String,
)

@Serializable
data class GlossaryTerm(
    val term: String,
    val definition: String,
    val category: String,
    val related: List<String> = emptyList(),
)

@Serializable
data class ArtistProfile(
    val artist: Artist,
    val summary: String,
    val movements: List<String>,
    val keyWorks: List<String>,
    val connections: List<ArtistConnection> = emptyList(),
)

@Serializable
data class ArtistConnection(
    val fromArtist: String,
    val toArtist: String,
    val relation: ArtistRelation,
    val note: String = "",
)

@Serializable
enum class ArtistRelation(val label: String) {
    TEACHER("Teacher"),
    STUDENT("Student"),
    RIVAL("Rival"),
    INFLUENCE("Influence"),
    COLLABORATOR("Collaborator"),
}

@Serializable
data class Flashcard(
    val id: String,
    val front: String,
    val back: String,
    val artworkId: String? = null,
    val easeFactor: Double = 2.5,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
    val dueAt: Long = 0,
    val lapses: Int = 0,
)

@Serializable
data class QuizQuestion(
    val id: String,
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val artworkId: String? = null,
    val kind: QuizKind = QuizKind.GENERAL,
    val focusX: Float = 0.5f,
    val focusY: Float = 0.5f,
    val zoom: Float = 1f,
)

@Serializable
enum class QuizKind(val label: String) {
    GENERAL("Daily quiz"),
    GUESS_THE_ARTIST("Guess the artist"),
    SPOT_THE_FAKE("Spot the fake"),
    DETAIL_HUNT("Detail hunt"),
}
