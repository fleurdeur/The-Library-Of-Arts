package org.libraryofarts.feature.games

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.min
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.libraryofarts.core.data.repository.ArtworkRepository
import org.libraryofarts.core.data.scan.ScanMatch
import org.libraryofarts.core.data.scan.ScanRepository
import org.libraryofarts.core.data.seed.SeedArtworks
import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.model.QuizKind
import org.libraryofarts.core.model.QuizQuestion

sealed interface ScanState {
    data object Idle : ScanState
    data object Scanning : ScanState
    data class Found(val match: ScanMatch) : ScanState
    data class Message(val text: String) : ScanState
}

@HiltViewModel
class GamesViewModel @Inject constructor(
    private val scans: ScanRepository,
    private val artworks: ArtworkRepository,
) : ViewModel() {
    private val _scan = MutableStateFlow<ScanState>(ScanState.Idle)
    val scan: StateFlow<ScanState> = _scan
    private val _hasScanIndex = MutableStateFlow(false)
    val hasScanIndex: StateFlow<Boolean> = _hasScanIndex

    private val _pool = MutableStateFlow<List<Artwork>>(SeedArtworks.all)
    val pool: StateFlow<List<Artwork>> = _pool

    private val _refreshingPool = MutableStateFlow(false)
    val refreshingPool: StateFlow<Boolean> = _refreshingPool

    init {
        viewModelScope.launch {
            _hasScanIndex.value = runCatching { scans.hasIndex() }.getOrDefault(false)
            runCatching { artworks.seedIfNeeded() }
            runCatching { artworks.allCandidates(400) }
                .getOrNull()
                ?.takeIf { it.isNotEmpty() }
                ?.let { _pool.value = it }
            // The games play the whole library: whatever is installed, plus a
            // daily catch-up of museum highlights so network works join in.
            runCatching { artworks.refreshHighlights(force = false) }
            runCatching { artworks.allCandidates(400) }
                .getOrNull()
                ?.takeIf { it.size > _pool.value.size }
                ?.let { _pool.value = it }
        }
    }

    /** Deliberate refresh: pulls the museum highlights again right now. */
    fun refreshPool() {
        if (_refreshingPool.value) return
        viewModelScope.launch {
            _refreshingPool.value = true
            runCatching { artworks.refreshHighlights(force = true) }
            runCatching { artworks.allCandidates(400) }
                .getOrNull()
                ?.takeIf { it.isNotEmpty() }
                ?.let { _pool.value = it }
            _refreshingPool.value = false
        }
    }

    fun identify(bitmap: Bitmap) {
        viewModelScope.launch {
            _scan.value = ScanState.Scanning
            val match = runCatching { scans.identify(bitmap) }.getOrNull()
            _scan.value = if (match != null) {
                ScanState.Found(match)
            } else {
                ScanState.Message(
                    if (_hasScanIndex.value) {
                        "No close match. Try filling the frame with the painting, with even light."
                    } else {
                        "Scanning works offline, but needs an installed scan pack. Install a pack from Settings → Offline packs, or search by title."
                    },
                )
            }
        }
    }

    fun resetScan() {
        _scan.value = ScanState.Idle
    }

    fun mood(mood: String, pool: List<Artwork> = _pool.value): List<Artwork> =
        pool.filter { artwork ->
            artwork.moods.any { it.equals(mood, true) } ||
                artwork.tags.any { it.equals(mood, true) } ||
                artwork.subjects.any { it.equals(mood, true) }
        }

    fun artworksForCompare(pool: List<Artwork> = _pool.value): List<Artwork> {
        val paintings = pool.filter { it.genre == "Painting" }
        val chosen = (paintings + pool).distinctBy { it.id }.take(8)
        return chosen
    }

    fun kidsArtworks(pool: List<Artwork> = _pool.value): List<Artwork> {
        val highlights = pool.filter { it.isHighlight }
        val chosen = if (highlights.size >= 4) highlights else pool
        return chosen
            .filter { it.image.thumbnailUrl.isNotBlank() || it.image.fullUrl.isNotBlank() }
            .ifEmpty { pool }
            .take(12)
    }
}

/**
 * An endless looking game. Each answer generates the next plate from the
 * library that is actually installed on this device, so offline packs and new
 * museum works enter the game automatically. Difficulty grows every three
 * correct answers: more options, tighter detail crops and rarer works.
 */
@HiltViewModel
class QuizViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(QuizState())
    val state: StateFlow<QuizState> = _state

    private var kind: QuizKind = QuizKind.GENERAL
    private var pool: List<Artwork> = emptyList()

    fun startIfNeeded(kind: QuizKind, pool: List<Artwork>) {
        val current = _state.value
        val alreadyPlaying = this.kind == kind && current.questions.isNotEmpty()
        // Seed works arrive first; when the installed/pack library finishes
        // loading, restart an untouched round so the full collection is used.
        if (alreadyPlaying && !(pool.size > this.pool.size && current.answered == 0)) return
        start(kind, pool)
    }

    fun start(kind: QuizKind, pool: List<Artwork>) {
        this.kind = kind
        this.pool = pool.filter { it.title.isNotBlank() }.ifEmpty { SeedArtworks.all }
        val first = generate(0, level = 1)
        _state.value = QuizState(
            kind = kind,
            questions = listOfNotNull(first),
            index = 0,
            level = 1,
            answered = 0,
        )
    }

    fun answer(index: Int) {
        val current = _state.value
        val question = current.current ?: return
        if (current.selected != null) return
        val correct = question.correctIndex == index
        val newStreak = if (correct) current.streak + 1 else 0
        val newScore = current.score + if (correct) 1 else 0
        val newLevel = (newScore / 3 + 1).coerceAtMost(8)
        _state.value = current.copy(
            selected = index,
            correct = correct,
            score = newScore,
            streak = newStreak,
            level = newLevel,
            answered = current.answered + 1,
        )
    }

    fun next() {
        val current = _state.value
        if (current.selected == null) return
        val nextIndex = current.index + 1
        val nextQuestion = generate(nextIndex)
        if (nextQuestion == null) {
            _state.value = current.copy(finished = true)
            return
        }
        _state.value = current.copy(
            questions = current.questions + nextQuestion,
            index = nextIndex,
            selected = null,
            correct = null,
        )
    }

    fun restart() {
        start(kind, pool)
    }

    private fun generate(index: Int, level: Int = _state.value.level): QuizQuestion? {
        if (pool.size < 4) return null
        return when (kind) {
            QuizKind.GUESS_THE_ARTIST -> artistQuestion(index, level)
            QuizKind.SPOT_THE_FAKE -> fakeTitleQuestion(index, level)
            QuizKind.DETAIL_HUNT -> detailQuestion(index, level)
            QuizKind.GENERAL -> generalQuestion(index, level)
        }
    }

    private fun optionCount(level: Int): Int = min(5, 2 + (level + 2) / 3)

    private fun artistQuestion(index: Int, level: Int): QuizQuestion? {
        val artwork = pool.filter { !it.artist?.name.isNullOrBlank() }.randomOrNull() ?: return null
        val correct = artwork.artist?.name ?: return null
        val artists = pool.mapNotNull { it.artist?.name }.filter { !it.equals(correct, true) }.distinct()
        if (artists.size < 2) return null
        val picks = artists.shuffled().take(optionCount(level) - 1)
        val options = (picks + correct).shuffled()
        return QuizQuestion(
            id = "artist-${index}-${artwork.id}",
            prompt = "Who made this work?",
            options = options,
            correctIndex = options.indexOf(correct),
            explanation = "${artwork.title} is by $correct. ${artwork.museum.shortName}, ${artwork.year.ifBlank { "date unrecorded" }}.",
            artworkId = artwork.id,
            kind = kind,
        )
    }

    private fun fakeTitleQuestion(index: Int, level: Int): QuizQuestion? {
        val artwork = pool.randomOrNull() ?: return null
        val realTitle = artwork.title
        val fakes = pool.filter { it.id != artwork.id }
            .map { it.title }
            .distinct()
            .shuffled()
            .take(optionCount(level) - 1)
            .mapIndexed { position, title -> inventTitle(title, position) }
        val options = (fakes + realTitle).shuffled()
        return QuizQuestion(
            id = "fake-${index}-${artwork.id}",
            prompt = "One label is the true title. Three were invented. Which one belongs to this plate?",
            options = options,
            correctIndex = options.indexOf(realTitle),
            explanation = "The true label is “$realTitle”. The other titles were invented for this exercise.",
            artworkId = artwork.id,
            kind = kind,
        )
    }

    private fun inventTitle(title: String, seed: Int): String {
        val words = title.split(' ').filter { it.isNotBlank() }
        if (words.size <= 1) return "Study for $title"
        val replacements = listOf("Nocturne", "Fragment", "Echo", "Study", "Variation")
        val replacement = replacements[seed % replacements.size]
        return (words.dropLast(1) + replacement).joinToString(" ")
    }

    private fun detailQuestion(index: Int, level: Int): QuizQuestion? {
        val artwork = pool.randomOrNull() ?: return null
        val titles = pool.filter { it.id != artwork.id }.map { it.title }.distinct().shuffled()
            .take(optionCount(level) - 1)
        if (titles.size < 2) return null
        val options = (titles + artwork.title).shuffled()
        val random = Random(index * 31 + artwork.id.hashCode())
        return QuizQuestion(
            id = "detail-${index}-${artwork.id}",
            prompt = "This is a detail at ${(2.0f + level * 0.32f).let { "%.1f".format(it) }}×. Which work does it come from?",
            options = options,
            correctIndex = options.indexOf(artwork.title),
            explanation = "It is a detail of “${artwork.title}” by ${artwork.displayArtist}. The full plate is in your library.",
            artworkId = artwork.id,
            kind = kind,
            focusX = 0.26f + random.nextFloat() * 0.48f,
            focusY = 0.24f + random.nextFloat() * 0.48f,
            zoom = 2.0f + level * 0.32f,
        )
    }

    private fun generalQuestion(index: Int, level: Int): QuizQuestion? {
        val artwork = pool.randomOrNull() ?: return null
        val facts = listOfNotNull(
            artwork.museum.name.takeIf { it.isNotBlank() }?.let { museum ->
                Fact(
                    prompt = "Which museum holds this work?",
                    correct = museum,
                    values = pool.map { it.museum.name },
                )
            },
            artwork.yearStart?.let { year ->
                val century = ((year.coerceAtLeast(1) - 1) / 100 + 1)
                Fact(
                    prompt = "In which century was this made?",
                    correct = centuryLabel(century),
                    values = pool.mapNotNull { it.yearStart }.map { centuryLabel((it.coerceAtLeast(1) - 1) / 100 + 1) },
                )
            },
            artwork.medium?.takeIf { it.length in 4..42 }?.let { medium ->
                Fact(
                    prompt = "What material is this work made from?",
                    correct = medium,
                    values = pool.mapNotNull { it.medium }.filter { it.length in 4..42 },
                )
            },
            artwork.displayPeriod?.let { period ->
                Fact(
                    prompt = "Which period does this work belong to?",
                    correct = period,
                    values = pool.mapNotNull { it.displayPeriod },
                )
            },
        ).shuffled()
        for (fact in facts) {
            val distractors = fact.values.filter { !it.equals(fact.correct, true) }.distinct()
            if (distractors.size >= 2) {
                val options = (distractors.shuffled().take(optionCount(level) - 1) + fact.correct).shuffled()
                return QuizQuestion(
                    id = "general-${index}-${artwork.id}",
                    prompt = fact.prompt,
                    options = options,
                    correctIndex = options.indexOf(fact.correct),
                    explanation = "${artwork.title} · ${artwork.displayArtist}, ${artwork.year.ifBlank { "date unrecorded" }} · ${artwork.museum.shortName}.",
                    artworkId = artwork.id,
                    kind = kind,
                )
            }
        }
        return artistQuestion(index, level)
    }

    private fun centuryLabel(century: Int): String {
        val suffix = if (century % 100 in 11..13) {
            "th"
        } else {
            when (century % 10) {
                1 -> "st"
                2 -> "nd"
                3 -> "rd"
                else -> "th"
            }
        }
        return "$century$suffix century"
    }

    private data class Fact(val prompt: String, val correct: String, val values: List<String>)
}

data class QuizState(
    val kind: QuizKind = QuizKind.GENERAL,
    val questions: List<QuizQuestion> = emptyList(),
    val index: Int = 0,
    val selected: Int? = null,
    val correct: Boolean? = null,
    val score: Int = 0,
    val streak: Int = 0,
    val level: Int = 1,
    val answered: Int = 0,
    val finished: Boolean = false,
) {
    val current: QuizQuestion? get() = questions.getOrNull(index)
    val levelProgress: Float get() = (score % 3) / 3f
}
