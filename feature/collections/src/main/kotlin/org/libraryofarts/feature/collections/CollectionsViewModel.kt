package org.libraryofarts.feature.collections

import android.net.Uri
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.libraryofarts.core.data.repository.ArtworkRepository
import org.libraryofarts.core.data.repository.CollectionRepository
import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.model.Collection

data class CollectionCardUi(
    val collection: Collection,
    val cover: Artwork? = null,
    val count: Int = 0,
)

data class CollectionsUiState(
    val collections: List<CollectionCardUi> = emptyList(),
    val loading: Boolean = true,
    val busy: Boolean = false,
    val message: String? = null,
)

sealed interface ShareRequest {
    data class FileShare(val file: File, val mime: String, val title: String) : ShareRequest
    data class Message(val text: String) : ShareRequest
}

@HiltViewModel
class CollectionsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: CollectionRepository,
    private val artworks: ArtworkRepository,
) : ViewModel() {
    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    private val _share = MutableSharedFlow<ShareRequest>()
    val share: SharedFlow<ShareRequest> = _share

    val state: StateFlow<CollectionsUiState> = repository.collections
        .map { list ->
            list.sortedByDescending { it.isFavorites }.map { collection ->
                val ids = repository.observeArtworkIds(collection.id).first().sortedBy { it }
                val cover = collection.coverArtworkId?.let { artworks.getArtwork(it) }
                    ?: ids.firstOrNull()?.let { artworks.getArtwork(it) }
                CollectionCardUi(collection, cover, ids.size)
            }
        }
        .combine(busy) { cards, isBusy -> CollectionsUiState(cards, loading = false, busy = isBusy, message = message.value) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CollectionsUiState())

    init {
        viewModelScope.launch { runCatching { repository.ensureFavorites() } }
    }

    fun create(name: String, description: String) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        repository.create(name, description)
        message.value = "Collection created."
    }

    fun delete(collection: Collection) = viewModelScope.launch {
        if (collection.isFavorites) return@launch
        repository.delete(collection.id)
    }

    fun exportPdf(collection: Collection) = viewModelScope.launch {
        busy.value = true
        runCatching {
            val file = repository.exportPdf(collection)
            _share.emit(ShareRequest.FileShare(file, "application/pdf", collection.name))
        }.onFailure { _share.emit(ShareRequest.Message(it.message ?: "The PDF could not be made.")) }
        busy.value = false
    }

    fun exportJson(collection: Collection) = viewModelScope.launch {
        busy.value = true
        runCatching {
            val file = repository.exportJson(collection)
            _share.emit(ShareRequest.FileShare(file, "application/vnd.libraryofarts.collection+json", collection.name))
        }.onFailure { _share.emit(ShareRequest.Message(it.message ?: "The collection could not be exported.")) }
        busy.value = false
    }

    fun importJson(uri: Uri) = viewModelScope.launch {
        busy.value = true
        runCatching {
            val file = File(context.cacheDir, "import-${System.currentTimeMillis()}.loa-collection")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: error("That file could not be read.")
            val imported = repository.importJson(file)
            message.value = "Imported “${imported.name}”."
        }.onFailure { _share.emit(ShareRequest.Message(it.message ?: "That file could not be imported.")) }
        busy.value = false
    }

    fun clearMessage() {
        message.value = null
    }

    companion object {
        val MIME = org.libraryofarts.core.model.ExportableCollection.MIME
    }
}
