package org.libraryofarts.feature.collections

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.libraryofarts.core.data.repository.ArtworkRepository
import org.libraryofarts.core.data.repository.CollectionRepository
import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.model.Collection

data class CollectionDetailUiState(
    val collection: Collection? = null,
    val artworks: List<Artwork> = emptyList(),
    val loading: Boolean = true,
    val busy: Boolean = false,
)

@HiltViewModel
class CollectionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CollectionRepository,
    private val artworkRepository: ArtworkRepository,
) : ViewModel() {
    val collectionId: String = savedStateHandle.get<String>("collectionId")
        ?: savedStateHandle.get<String>("id")
        ?: CollectionRepository.FAVORITES_ID
    private val busy = MutableStateFlow(false)
    private val _share = MutableSharedFlow<ShareRequest>()
    val share: SharedFlow<ShareRequest> = _share

    private val _state = MutableStateFlow(CollectionDetailUiState())
    val state: StateFlow<CollectionDetailUiState> = _state

    init {
        viewModelScope.launch {
            repository.observeCollection(collectionId).collect { collection ->
                val artworks = collection?.let { repository.artworksFor(it) }.orEmpty()
                _state.value = _state.value.copy(
                    collection = collection,
                    artworks = artworks,
                    loading = false,
                    busy = busy.value,
                )
            }
        }
    }

    fun remove(artwork: Artwork) = viewModelScope.launch {
        repository.remove(collectionId, artwork.id)
    }

    fun exportPdf() = viewModelScope.launch {
        val collection = state.value.collection ?: return@launch
        busy.value = true
        runCatching {
            val file = repository.exportPdf(collection)
            _share.emit(ShareRequest.FileShare(file, "application/pdf", collection.name))
        }.onFailure { _share.emit(ShareRequest.Message(it.message ?: "The PDF could not be made.")) }
        busy.value = false
    }

    fun exportJson() = viewModelScope.launch {
        val collection = state.value.collection ?: return@launch
        busy.value = true
        runCatching {
            val file = repository.exportJson(collection)
            _share.emit(ShareRequest.FileShare(file, "application/vnd.libraryofarts.collection+json", collection.name))
        }.onFailure { _share.emit(ShareRequest.Message(it.message ?: "The collection could not be exported.")) }
        busy.value = false
    }

}
