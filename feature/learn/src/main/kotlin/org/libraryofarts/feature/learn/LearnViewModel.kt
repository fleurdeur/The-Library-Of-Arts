package org.libraryofarts.feature.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.libraryofarts.core.data.repository.ArtworkRepository
import org.libraryofarts.core.data.repository.LearningRepository
import org.libraryofarts.core.model.Flashcard
import org.libraryofarts.core.model.GlossaryTerm

@HiltViewModel
class LearnViewModel @Inject constructor(
    val repository: LearningRepository,
    private val artworks: ArtworkRepository,
) : ViewModel() {
    private val _glossaryQuery = MutableStateFlow("")
    val glossaryQuery: StateFlow<String> = _glossaryQuery

    val dueCards: StateFlow<List<Flashcard>> = repository.observeDue(40)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { runCatching { repository.ensureFlashcards() } }
    }

    fun setGlossaryQuery(value: String) {
        _glossaryQuery.value = value
    }

    fun glossary(query: String): List<GlossaryTerm> = repository.searchGlossary(query)

    fun tour(id: String) = repository.tour(id)
    fun lesson(id: String) = repository.lesson(id)
    fun artist(name: String) = repository.artist(name)

    fun review(card: Flashcard, quality: Int) {
        viewModelScope.launch { repository.review(card, quality) }
    }

    suspend fun artworkById(id: String) = artworks.getArtwork(id)
}
