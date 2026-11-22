package org.libraryofarts.feature.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.libraryofarts.core.common.AppDispatchers
import org.libraryofarts.core.data.repository.JournalRepository
import org.libraryofarts.core.data.repository.JournalStats
import org.libraryofarts.core.model.JournalEntry
import org.libraryofarts.core.model.JournalTimelinePoint
import org.libraryofarts.core.model.Mood

data class JournalUiState(
    val entries: List<JournalEntry> = emptyList(),
    val stats: JournalStats = JournalStats(),
    val timeline: List<JournalTimelinePoint> = emptyList(),
    val query: String = "",
    val mood: Mood? = null,
    val loading: Boolean = true,
)

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val repository: JournalRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val mood = MutableStateFlow<Mood?>(null)
    private val stats = MutableStateFlow(JournalStats())
    private val timeline = MutableStateFlow<List<JournalTimelinePoint>>(emptyList())

    private val entriesFlow = combine(query, mood) { q, m -> q to m }
        .flatMapLatest { (q, m) ->
            when {
                q.isNotBlank() -> repository.search(q)
                m != null -> repository.byMood(m)
                else -> repository.entries
            }
        }

    val state: StateFlow<JournalUiState> = combine(
        entriesFlow,
        query,
        mood,
        stats,
        timeline,
    ) { entries, q, m, statistics, months ->
        JournalUiState(entries, statistics, months, q, m, loading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JournalUiState())

    init {
        viewModelScope.launch {
            runCatching {
                stats.value = repository.stats()
                timeline.value = repository.timeline()
            }
        }
    }

    fun search(value: String) {
        query.value = value
    }

    fun selectMood(value: Mood?) {
        mood.value = if (mood.value == value) null else value
    }

    fun delete(entry: JournalEntry) {
        viewModelScope.launch {
            repository.delete(entry)
            stats.value = repository.stats()
            timeline.value = repository.timeline()
        }
    }

    fun refreshStats() {
        viewModelScope.launch {
            stats.value = repository.stats()
            timeline.value = repository.timeline()
        }
    }
}
