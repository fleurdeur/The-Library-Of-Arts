package org.libraryofarts.feature.artwork

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.libraryofarts.core.common.AppDispatchers
import org.libraryofarts.core.data.engine.EyesFirstEngine
import org.libraryofarts.core.data.export.ShareCardRenderer
import org.libraryofarts.core.data.export.ShareFont
import org.libraryofarts.core.data.export.ShareLayout
import org.libraryofarts.core.data.export.ShareRatio
import org.libraryofarts.core.data.repository.ArtworkRepository
import org.libraryofarts.core.data.repository.CollectionRepository
import org.libraryofarts.core.data.repository.JournalRepository
import org.libraryofarts.core.media.AudioPlayerController
import org.libraryofarts.core.media.Narrator
import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.model.ArtworkLayer
import org.libraryofarts.core.model.AudioTrack
import org.libraryofarts.core.model.EyesFirstComparison
import org.libraryofarts.core.model.JournalEntry
import org.libraryofarts.core.model.Mood

data class ArtworkUiState(
    val loading: Boolean = true,
    val artwork: Artwork? = null,
    val related: List<Artwork> = emptyList(),
    val journal: List<JournalEntry> = emptyList(),
    val saved: Boolean = false,
    val error: String? = null,
    val interpretation: String = "",
    val comparison: EyesFirstComparison? = null,
    val comparing: Boolean = false,
    val activeLayer: ArtworkLayer? = null,
    val audioTrack: AudioTrack? = null,
    val audioPlaying: Boolean = false,
    val brushstroke: Boolean = false,
    val journalSaved: Boolean = false,
    val lens: org.libraryofarts.core.model.StudyLens = org.libraryofarts.core.model.StudyLens.NONE,
)

@HiltViewModel
class ArtworkViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val artworks: ArtworkRepository,
    private val collections: CollectionRepository,
    private val journal: JournalRepository,
    private val eyesFirst: EyesFirstEngine,
    private val audio: AudioPlayerController,
    private val narrator: Narrator,
    private val shareRenderer: ShareCardRenderer,
) : ViewModel() {
    // The navigation graph declares this argument as `id`; older builds read
    // `artworkId`, which is always absent when the screen is opened from Home,
    // Explore or a deep link. Accept both so an existing route keeps working.
    val artworkId: String = savedStateHandle.get<String>("artworkId")
        ?: savedStateHandle.get<String>("id")
        ?: ""
    private val _state = MutableStateFlow(ArtworkUiState())
    val state: StateFlow<ArtworkUiState> = _state.asStateFlow()
    private var sessionStarted = System.currentTimeMillis()
    private var zoomed = false
    private val _shareFile = MutableStateFlow<java.io.File?>(null)
    val shareFile: StateFlow<java.io.File?> = _shareFile.asStateFlow()

    init {
        viewModelScope.launch(AppDispatchers.io) {
            runCatching {
                val artwork = artworks.getArtwork(artworkId)
                if (artwork == null) {
                    _state.value = _state.value.copy(loading = false, error = "That artwork is not in the library yet.")
                    return@runCatching
                }
                _state.value = _state.value.copy(
                    loading = false,
                    artwork = artwork,
                    related = artworks.related(artwork),
                )
                artworks.recordView(artwork)
                sessionStarted = System.currentTimeMillis()
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    loading = false,
                    error = error.message ?: "The artwork could not be loaded.",
                )
            }
        }
        viewModelScope.launch {
            runCatching { collections.ensureFavorites() }
            collections.observeArtworkIds(CollectionRepository.FAVORITES_ID).collectLatest { ids ->
                _state.value = _state.value.copy(saved = artworkId in ids)
            }
        }
        viewModelScope.launch {
            journal.forArtwork(artworkId).collectLatest { entries ->
                _state.value = _state.value.copy(journal = entries)
            }
        }
        viewModelScope.launch {
            audio.current.collectLatest { track ->
                _state.value = _state.value.copy(audioTrack = track)
            }
        }
        viewModelScope.launch {
            audio.playing.collectLatest { playing ->
                _state.value = _state.value.copy(audioPlaying = playing)
            }
        }
    }

    fun recordSession() {
        val artwork = _state.value.artwork ?: return
        val dwell = System.currentTimeMillis() - sessionStarted
        if (dwell < 800) return
        viewModelScope.launch(AppDispatchers.io) {
            runCatching { artworks.recordView(artwork, dwellMillis = dwell, zoomed = zoomed) }
        }
        sessionStarted = System.currentTimeMillis()
    }

    fun markZoomed() {
        zoomed = true
        _state.value.artwork?.let { artwork ->
            viewModelScope.launch(AppDispatchers.io) { runCatching { artworks.recordZoom(artwork) } }
        }
    }

    fun toggleSave() {
        val artwork = _state.value.artwork ?: return
        viewModelScope.launch {
            runCatching { collections.ensureFavorites() }
            if (_state.value.saved) {
                collections.remove(CollectionRepository.FAVORITES_ID, artwork.id)
            } else {
                collections.add(CollectionRepository.FAVORITES_ID, artwork.id)
            }
        }
    }

    fun compareInterpretation(text: String) {
        val artwork = _state.value.artwork ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(comparing = true)
            val result = runCatching { eyesFirst.compare(text, artwork) }.getOrNull()
            _state.value = _state.value.copy(comparing = false, comparison = result, interpretation = text)
        }
    }

    fun saveInterpretation(text: String, mood: Mood, tags: List<String>) {
        val artwork = _state.value.artwork ?: return
        viewModelScope.launch {
            runCatching {
                journal.saveInterpretation(
                    artworkId = artwork.id,
                    artworkTitle = artwork.title,
                    imageUrl = artwork.image.thumbnailUrl,
                    text = text,
                    mood = mood,
                    tags = (tags + artwork.tags.take(2)).distinct(),
                )
            }
            _state.value = _state.value.copy(journalSaved = true)
        }
    }

    fun toggleBrushstroke() {
        _state.value = _state.value.copy(brushstroke = !_state.value.brushstroke)
    }

    fun selectLayer(layer: ArtworkLayer?) {
        _state.value = _state.value.copy(activeLayer = layer)
    }

    fun selectLens(lens: org.libraryofarts.core.model.StudyLens) {
        _state.value = _state.value.copy(lens = lens)
    }

    fun play(track: AudioTrack) {
        audio.play(track)
    }

    fun toggleAudio() {
        audio.toggle()
    }

    fun narrate() {
        val artwork = _state.value.artwork ?: return
        val text = buildString {
            append(artwork.title).append(". ")
            append(artwork.displayArtist).append(", ").append(artwork.year).append(". ")
            append(artwork.description.orEmpty())
        }
        narrator.speak(text)
    }

    fun stopNarration() = narrator.stop()

    fun renderShareCard(words: String, layout: ShareLayout, ratio: ShareRatio, font: ShareFont) {
        val artwork = _state.value.artwork ?: return
        viewModelScope.launch(AppDispatchers.io) {
            runCatching { shareRenderer.renderToFile(artwork, words, layout, ratio, font) }
                .onSuccess { _shareFile.value = it }
        }
    }

    fun clearShareFile() {
        _shareFile.value = null
    }

    fun clearJournalSaved() {
        _state.value = _state.value.copy(journalSaved = false)
    }

    override fun onCleared() {
        recordSession()
        narrator.stop()
        super.onCleared()
    }
}
