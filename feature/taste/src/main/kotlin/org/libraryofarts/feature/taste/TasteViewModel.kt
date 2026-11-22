package org.libraryofarts.feature.taste

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.libraryofarts.core.data.repository.TasteRepository
import org.libraryofarts.core.datastore.SettingsRepository
import org.libraryofarts.core.datastore.UserPreferences
import org.libraryofarts.core.model.TasteProfile

data class TasteUiState(
    val profile: TasteProfile = TasteProfile(),
    val prefs: UserPreferences = UserPreferences(),
    val signalCount: Int = 0,
    val busy: Boolean = false,
)

@HiltViewModel
class TasteViewModel @Inject constructor(
    private val taste: TasteRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val busy = MutableStateFlow(false)
    private val signals = MutableStateFlow(0)

    val state: StateFlow<TasteUiState> = combine(
        taste.profile,
        settings.preferences,
        busy,
        signals,
    ) { profile, prefs, isBusy, signalCount ->
        TasteUiState(profile, prefs, signalCount, isBusy)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasteUiState())

    init {
        viewModelScope.launch {
            signals.value = runCatching { taste.signalCount() }.getOrDefault(0)
        }
    }

    fun setDiscovery(value: Float) = viewModelScope.launch { settings.setDiscoveryRatio(value) }
    fun setLearning(enabled: Boolean) = viewModelScope.launch { settings.setTasteLearningEnabled(enabled) }
    fun mute(key: String, muted: Boolean) = viewModelScope.launch { taste.setMuted(key, muted) }
    fun pin(key: String, pinned: Boolean) = viewModelScope.launch { taste.setPinned(key, pinned) }
    fun remove(key: String) = viewModelScope.launch { taste.removeInterest(key) }

    fun reset() = viewModelScope.launch {
        busy.value = true
        runCatching {
            taste.reset()
            signals.value = 0
        }
        busy.value = false
    }
}
