package org.libraryofarts.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.libraryofarts.core.common.AppDispatchers
import org.libraryofarts.core.common.ConnectivityObserver
import org.libraryofarts.core.data.repository.ArtworkRepository
import org.libraryofarts.core.data.repository.CollectionRepository
import org.libraryofarts.core.data.repository.HomeContent
import org.libraryofarts.core.data.repository.HomeRepository
import org.libraryofarts.core.datastore.SettingsRepository
import org.libraryofarts.core.datastore.UserPreferences
import org.libraryofarts.core.model.Artwork

data class HomeUiState(
    val loading: Boolean = true,
    val content: HomeContent? = null,
    val savedIds: Set<String> = emptySet(),
    val preferences: UserPreferences = UserPreferences(),
    val online: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: Lazy<HomeRepository>,
    private val collections: Lazy<CollectionRepository>,
    private val artworkRepository: Lazy<ArtworkRepository>,
    private val settings: SettingsRepository,
    private val connectivity: ConnectivityObserver,
) : ViewModel() {
    private val refreshTick = MutableStateFlow(0)
    private val _saved = MutableStateFlow<Set<String>>(emptySet())
    private val _online = MutableStateFlow(true)
    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing

    val state: StateFlow<HomeUiState> = combine(
        settings.preferences,
        _saved,
        _online,
        refreshTick,
    ) { prefs, saved, online, _ ->
        HomeStateInput(prefs, saved, online)
    }.mapLatest { input ->
        runCatching { homeRepository.get().load() }.fold(
            onSuccess = { content ->
                HomeUiState(
                    loading = false,
                    content = content,
                    savedIds = input.saved,
                    preferences = input.prefs,
                    online = input.online,
                )
            },
            onFailure = { error ->
                HomeUiState(
                    loading = false,
                    savedIds = input.saved,
                    preferences = input.prefs,
                    online = input.online,
                    error = error.message ?: "The library could not be opened just now.",
                )
            },
        )
    }.flowOn(AppDispatchers.io)
        .catch { error ->
            emit(
                HomeUiState(
                    loading = false,
                    error = error.message ?: "The library could not be opened just now.",
                ),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        // Everything touching the encrypted database runs off the main thread
        // and is individually guarded. A damaged keystore or database must
        // never take the whole app down at launch.
        viewModelScope.launch(AppDispatchers.io) {
            runCatching { collections.get().ensureFavorites() }
            runCatching {
                collections.get().observeArtworkIds(CollectionRepository.FAVORITES_ID)
                    .catch { }
                    .collect { ids -> _saved.value = ids.toSet() }
            }
        }
        viewModelScope.launch(AppDispatchers.io) {
            runCatching {
                connectivity.isOnline.collect { isOnline -> _online.value = isOnline }
            }
        }
        viewModelScope.launch(AppDispatchers.io) {
            // Seed only. Nothing reaches the network until the reader asks for
            // new plates with a deliberate pull-to-refresh gesture.
            runCatching { artworkRepository.get().seedIfNeeded() }
        }
    }

    fun refresh() {
        if (_refreshing.value) return
        _refreshing.value = true
        viewModelScope.launch(AppDispatchers.io) {
            runCatching { artworkRepository.get().refreshHighlights(force = true) }
            refreshTick.value += 1
            _refreshing.value = false
        }
    }

    fun toggleSave(artwork: Artwork) {
        viewModelScope.launch(AppDispatchers.io) {
            runCatching {
                val repository = collections.get()
                repository.ensureFavorites()
                if (artwork.id in _saved.value) {
                    repository.remove(CollectionRepository.FAVORITES_ID, artwork.id)
                } else {
                    repository.add(CollectionRepository.FAVORITES_ID, artwork.id)
                }
            }
        }
    }

    fun recordDailyView() {
        state.value.content?.daily?.let { artwork ->
            viewModelScope.launch(AppDispatchers.io) {
                runCatching { artworkRepository.get().recordView(artwork) }
            }
        }
    }

    private data class HomeStateInput(
        val prefs: UserPreferences,
        val saved: Set<String>,
        val online: Boolean,
    )
}
