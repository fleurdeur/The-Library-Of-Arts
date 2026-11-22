package org.libraryofarts.core.data.repository

import org.libraryofarts.core.common.TimeProvider
import org.libraryofarts.core.data.seed.SeedArtworks
import org.libraryofarts.core.data.seed.SeedLearning
import org.libraryofarts.core.database.FlashcardEntity
import org.libraryofarts.core.database.LearningDao
import org.libraryofarts.core.model.ArtistProfile
import org.libraryofarts.core.model.Flashcard
import org.libraryofarts.core.model.GlossaryTerm
import org.libraryofarts.core.model.Lesson
import org.libraryofarts.core.model.QuizQuestion
import org.libraryofarts.core.model.StoryTour
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class LearningRepository @Inject constructor(
    private val dao: LearningDao,
    private val time: TimeProvider,
) {
    val tours: List<StoryTour> = SeedLearning.tours
    val lessons: List<Lesson> = SeedLearning.lessons
    val glossary: List<GlossaryTerm> = SeedLearning.glossary
    val artistProfiles: List<ArtistProfile> = SeedLearning.artists
    val quizQuestions: List<QuizQuestion> = SeedLearning.quiz

    fun tour(id: String): StoryTour? = tours.firstOrNull { it.id == id }
    fun lesson(id: String): Lesson? = lessons.firstOrNull { it.id == id }
    fun artist(name: String): ArtistProfile? = artistProfiles.firstOrNull { it.artist.name.equals(name, true) }

    fun searchGlossary(query: String): List<GlossaryTerm> {
        if (query.isBlank()) return glossary
        return glossary.filter {
            it.term.contains(query, true) || it.definition.contains(query, true)
        }
    }

    fun quizFor(kind: org.libraryofarts.core.model.QuizKind? = null): List<QuizQuestion> =
        if (kind == null) quizQuestions else quizQuestions.filter { it.kind == kind }

    suspend fun ensureFlashcards() {
        if (dao.observeAll().first().isNotEmpty()) return
        val now = time.nowMillis()
        val cards = SeedArtworks.all.flatMap { artwork ->
            listOf(
                Flashcard(
                    id = "card-artist-${artwork.id}",
                    front = "Who painted “${artwork.title}”?",
                    back = artwork.displayArtist,
                    artworkId = artwork.id,
                    dueAt = now,
                ),
                Flashcard(
                    id = "card-period-${artwork.id}",
                    front = "Which period does “${artwork.title}” belong to?",
                    back = artwork.period ?: "Unknown",
                    artworkId = artwork.id,
                    dueAt = now,
                ),
            )
        }.distinctBy { it.id }
        dao.upsertCards(cards.map { it.toEntity() })
    }

    fun observeDue(limit: Int = 30): Flow<List<Flashcard>> =
        dao.observeAll().map { list ->
            list.filter { it.dueAt <= time.nowMillis() }.take(limit).map { it.toModel() }
        }

    fun observeAll(): Flow<List<Flashcard>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    suspend fun review(card: Flashcard, quality: Int): Flashcard {
        // SM-2 with a gentle floor: a difficult card comes back sooner, never punishes.
        val q = quality.coerceIn(0, 5)
        val ease = (card.easeFactor + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))).coerceAtLeast(1.3)
        val repetitions = if (q < 3) 0 else card.repetitions + 1
        val interval = when {
            q < 3 -> 1
            repetitions == 1 -> 1
            repetitions == 2 -> 6
            else -> (card.intervalDays * ease).toInt().coerceAtLeast(1)
        }
        val updated = card.copy(
            easeFactor = ease,
            intervalDays = interval,
            repetitions = repetitions,
            dueAt = time.nowMillis() + interval * 86_400_000L,
            lapses = card.lapses + if (q < 3) 1 else 0,
        )
        dao.upsertCard(updated.toEntity())
        return updated
    }

    private fun Flashcard.toEntity(): FlashcardEntity = FlashcardEntity(
        id = id,
        front = front,
        back = back,
        artworkId = artworkId,
        easeFactor = easeFactor,
        intervalDays = intervalDays,
        repetitions = repetitions,
        dueAt = dueAt,
        lapses = lapses,
    )

    private fun FlashcardEntity.toModel(): Flashcard = Flashcard(
        id = id,
        front = front,
        back = back,
        artworkId = artworkId,
        easeFactor = easeFactor,
        intervalDays = intervalDays,
        repetitions = repetitions,
        dueAt = dueAt,
        lapses = lapses,
    )
}
