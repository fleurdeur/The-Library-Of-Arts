package org.libraryofarts.feature.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.libraryofarts.core.common.AppDispatchers
import org.libraryofarts.core.data.repository.ArtworkRepository
import org.libraryofarts.core.data.repository.CollectionRepository
import org.libraryofarts.core.data.repository.MuseumVisitRepository
import org.libraryofarts.core.data.seed.SeedLearning
import org.libraryofarts.core.data.seed.SeedArtworks
import org.libraryofarts.core.model.ArtistConnection
import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.model.ArtworkFilter
import org.libraryofarts.core.model.Museum
import org.libraryofarts.core.model.MuseumAudioStop
import org.libraryofarts.core.model.MuseumCatalog
import org.libraryofarts.core.model.NearbyMuseum
import org.libraryofarts.core.model.SortOrder

enum class ExploreMode(val label: String) {
    BROWSE("Browse"),
    MAP("Museum map"),
    TIME("Time Machine"),
    COLORS("Colour Search"),
    VISIT("Museum Visit"),
}

data class ExploreUiState(
    val filter: ArtworkFilter = ArtworkFilter(),
    val mode: ExploreMode = ExploreMode.BROWSE,
    val savedIds: Set<String> = emptySet(),
    val hiddenGems: List<Artwork> = emptyList(),
    val centuryResults: List<Artwork> = emptyList(),
    val selectedCentury: Int? = null,
    val colorResults: List<Artwork> = emptyList(),
    val nearby: List<NearbyMuseum> = emptyList(),
    val locationState: String? = null,
    val selectedMuseum: Museum? = null,
    val floorGuide: String? = null,
    val tourStops: List<MuseumAudioStop> = emptyList(),
    val museums: List<Museum> = MuseumCatalog.all,
    val artistConnections: List<ArtistConnection> = SeedLearning.artists.flatMap { it.connections }.distinctBy { it.fromArtist + it.toArtist },
    val onlineSources: Int = 0,
    val busy: Boolean = false,
)

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val artworks: ArtworkRepository,
    private val collections: CollectionRepository,
    private val visits: MuseumVisitRepository,
) : ViewModel() {
    private val filter = MutableStateFlow(ArtworkFilter())
    private val mode = MutableStateFlow(ExploreMode.BROWSE)
    private val _ui = MutableStateFlow(ExploreUiState())
    val ui: StateFlow<ExploreUiState> = _ui

    val results: Flow<PagingData<Artwork>> = filter
        .debounce(250)
        .flatMapLatest { artworks.search(it) }

    init {
        viewModelScope.launch(AppDispatchers.io) {
            runCatching { artworks.seedIfNeeded() }
            runCatching { collections.ensureFavorites() }
            runCatching {
                _ui.value = _ui.value.copy(hiddenGems = artworks.hiddenGems(10))
            }
            collections.observeArtworkIds(CollectionRepository.FAVORITES_ID).collect { ids ->
                _ui.value = _ui.value.copy(savedIds = ids.toSet())
            }
        }
        viewModelScope.launch {
            combine(filter, mode) { f, m -> f to m }.collect { (f, m) ->
                _ui.value = _ui.value.copy(filter = f, mode = m)
            }
        }
    }

    fun setMode(value: ExploreMode) {
        mode.value = value
        if (value == ExploreMode.VISIT && _ui.value.nearby.isEmpty()) {
            _ui.value = _ui.value.copy(locationState = "Share your location to find nearby museums. Nothing is stored.")
        }
    }

    fun setQuery(value: String) {
        filter.value = filter.value.copy(query = value)
    }

    fun toggleGenre(value: String) { filter.value = filter.value.toggleGenre(value) }
    fun togglePeriod(value: String) { filter.value = filter.value.togglePeriod(value) }
    fun toggleMuseum(value: String) { filter.value = filter.value.toggleMuseum(value) }
    fun toggleMedium(value: String) { filter.value = filter.value.toggleMedium(value) }
    fun toggleMood(value: String) { filter.value = filter.value.toggleMood(value) }
    fun toggleSubject(value: String) { filter.value = filter.value.toggleSubject(value) }

    fun toggleColor(value: String) {
        filter.value = filter.value.toggleColor(value)
        loadColorResults()
    }

    fun setSort(value: SortOrder) {
        filter.value = filter.value.copy(sort = value)
    }

    fun setCentury(century: Int?) {
        filter.value = filter.value.copy(
            centuryFrom = century,
            centuryTo = century,
            sort = if (century != null) SortOrder.OLDEST else SortOrder.RELEVANCE,
        )
        _ui.value = _ui.value.copy(selectedCentury = century)
        if (century != null) {
            viewModelScope.launch(AppDispatchers.io) {
                val list = artworks.searchOnce(
                    ArtworkFilter(
                        centuryFrom = century,
                        centuryTo = century,
                        sort = SortOrder.OLDEST,
                    ),
                    40,
                )
                _ui.value = _ui.value.copy(centuryResults = list)
            }
        }
    }

    fun clearFilters() {
        filter.value = ArtworkFilter()
        _ui.value = _ui.value.copy(colorResults = emptyList(), selectedCentury = null)
    }

    fun selectColor(value: String?) {
        filter.value = filter.value.copy(colors = value?.let { setOf(it) } ?: emptySet())
        loadColorResults()
    }

    fun loadColorResults() {
        viewModelScope.launch(AppDispatchers.io) {
            val list = artworks.searchOnce(filter.value.copy(sort = SortOrder.MOST_VIEWED), 30)
            _ui.value = _ui.value.copy(colorResults = list)
        }
    }

    fun onLocation(latitude: Double, longitude: Double) {
        val nearby = visits.nearby(latitude, longitude)
        _ui.value = _ui.value.copy(
            nearby = nearby,
            locationState = if (nearby.isEmpty()) "No partner museums within 1,500 km — browse the map instead." else null,
        )
    }

    fun locationUnavailable(message: String) {
        _ui.value = _ui.value.copy(locationState = message)
    }

    fun openMuseum(museum: Museum) {
        val guide = visits.floorGuide(museum)
        val tour = visits.audioTour(museum)
        _ui.value = _ui.value.copy(selectedMuseum = museum, floorGuide = guide, tourStops = tour)
    }

    fun closeMuseum() {
        _ui.value = _ui.value.copy(selectedMuseum = null, floorGuide = null, tourStops = emptyList())
    }

    fun toggleSave(artwork: Artwork) {
        viewModelScope.launch {
            runCatching { collections.ensureFavorites() }
            if (artwork.id in _ui.value.savedIds) {
                collections.remove(CollectionRepository.FAVORITES_ID, artwork.id)
            } else {
                collections.add(CollectionRepository.FAVORITES_ID, artwork.id)
            }
        }
    }

    fun artistWorks(artistName: String): List<Artwork> =
        SeedArtworks.all.filter { it.artist?.name == artistName }
}
