package org.libraryofarts.feature.games

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.libraryofarts.core.data.repository.ArtworkRepository
import org.libraryofarts.core.data.scan.ScanMatch
import org.libraryofarts.core.data.scan.ScanRepository
import org.libraryofarts.core.data.seed.SeedArtworks
import org.libraryofarts.core.data.seed.SeedLearning
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

    init {
        viewModelScope.launch {
            _hasScanIndex.value = runCatching { scans.hasIndex() }.getOrDefault(false)
            runCatching { artworks.seedIfNeeded() }
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

    fun quiz(kind: QuizKind): List<QuizQuestion> = SeedLearning.quiz.filter { it.kind == kind }
    fun mood(mood: String) = SeedArtworks.all.filter { artwork -> artwork.moods.any { it.equals(mood, true) } }
    fun artworksForCompare(): List<org.libraryofarts.core.model.Artwork> = SeedArtworks.all.filter { it.genre == "Painting" }.take(6)
    fun kidsArtworks() = SeedArtworks.all.filter { it.isHighlight }.take(8)
}

@HiltViewModel
class QuizViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(QuizState())
    val state: StateFlow<QuizState> = _state

    fun start(questions: List<QuizQuestion>) {
        _state.value = QuizState(questions = questions, index = 0)
    }

    fun answer(index: Int) {
        val current = _state.value
        if (current.selected != null || current.questions.isEmpty()) return
        val correct = current.questions[current.index].correctIndex == index
        _state.value = current.copy(
            selected = index,
            correct = correct,
            score = current.score + if (correct) 1 else 0,
        )
    }

    fun next() {
        val current = _state.value
        if (current.index + 1 >= current.questions.size) {
            _state.value = current.copy(finished = true)
        } else {
            _state.value = current.copy(index = current.index + 1, selected = null, correct = null)
        }
    }

    fun restart() {
        _state.value = QuizState(questions = _state.value.questions)
    }
}

data class QuizState(
    val questions: List<QuizQuestion> = emptyList(),
    val index: Int = 0,
    val selected: Int? = null,
    val correct: Boolean? = null,
    val score: Int = 0,
    val finished: Boolean = false,
) {
    val current: QuizQuestion? get() = questions.getOrNull(index)
}
