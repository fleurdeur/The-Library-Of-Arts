package org.libraryofarts.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
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
import org.libraryofarts.core.data.repository.TasteRepository
import org.libraryofarts.core.datastore.SettingsRepository
import org.libraryofarts.core.datastore.UserPreferences
import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.model.TasteProfile

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
    private val homeRepository: HomeRepository,
    private val collections: CollectionRepository,
    private val tasteRepository: TasteRepository,
    private val settings: SettingsRepository,
    private val connectivity: ConnectivityObserver,
    private val artworkRepository: ArtworkRepository,
) : ViewModel() {
    private val refreshTick = MutableStateFlow(0)
    private val _saved = MutableStateFlow<Set<String>>(emptySet())
    private val _online = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)
    private val _loading = MutableStateFlow(true)

    val state: StateFlow<HomeUiState> = combine(
        settings.preferences,
        tasteRepository.profile,
        _saved,
        _online,
        refreshTick,
    ) { prefs, profile, saved, online, _ ->
        HomeStateInput(prefs, profile, saved, online)
    }.mapLatest { input ->
        _loading.value = true
        _error.value = null
        runCatching {
            homeRepository.load(input.profile)
        }.fold(
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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch {
            runCatching { collections.ensureFavorites() }
            collections.observeArtworkIds(CollectionRepository.FAVORITES_ID).collect { ids ->
                _saved.value = ids.toSet()
            }
        }
        viewModelScope.launch {
            connectivity.isOnline.collect { _online.value = it }
        }
        viewModelScope.launch {
            runCatching { artworkRepository.seedIfNeeded() }
        }
    }

    fun refresh() {
        refreshTick.value += 1
    }

    fun toggleSave(artwork: Artwork) {
        viewModelScope.launch {
            runCatching { collections.ensureFavorites() }
            if (artwork.id in _saved.value) {
                collections.remove(CollectionRepository.FAVORITES_ID, artwork.id)
            } else {
                collections.add(CollectionRepository.FAVORITES_ID, artwork.id)
            }
        }
    }

    fun recordDailyView() {
        state.value.content?.daily?.let { artwork ->
            viewModelScope.launch { runCatching { artworkRepository.recordView(artwork) } }
        }
    }

    fun recordTasteMute(key: String, muted: Boolean) {
        viewModelScope.launch { tasteRepository.setMuted(key, muted) }
    }

    private data class HomeStateInput(
        val prefs: UserPreferences,
        val profile: TasteProfile,
        val saved: Set<String>,
        val online: Boolean,
    )
}
