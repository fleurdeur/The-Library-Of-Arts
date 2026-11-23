package org.libraryofarts.feature.taste

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.libraryofarts.core.designsystem.component.ArtButton
import org.libraryofarts.core.designsystem.component.GlassCard
import org.libraryofarts.core.designsystem.component.SectionHeader
import org.libraryofarts.core.designsystem.component.SoftBackdrop
import org.libraryofarts.core.designsystem.theme.LibraryColors
import org.libraryofarts.core.model.Interest
import org.libraryofarts.core.model.TasteDimension
import org.libraryofarts.core.ui.BackButton
import org.libraryofarts.core.ui.OfflineBanner
import org.libraryofarts.core.ui.ScreenTitle
import kotlin.math.roundToInt

@Composable
fun TasteProfileRoute(
    onBack: () -> Unit,
    viewModel: TasteViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TasteProfileScreen(
        state = state,
        onBack = onBack,
        onDiscoveryChange = viewModel::setDiscovery,
        onLearningChange = viewModel::setLearning,
        onMute = viewModel::mute,
        onPin = viewModel::pin,
        onRemove = viewModel::remove,
        onReset = viewModel::reset,
    )
}

@Composable
fun TasteProfileScreen(
    state: TasteUiState,
    onBack: () -> Unit,
    onDiscoveryChange: (Float) -> Unit,
    onLearningChange: (Boolean) -> Unit,
    onMute: (String, Boolean) -> Unit,
    onPin: (String, Boolean) -> Unit,
    onRemove: (String) -> Unit,
    onReset: () -> Unit,
) {
    var confirmReset by remember { mutableStateOf(false) }
    SoftBackdrop {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 120.dp),
        ) {
            item("top") {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BackButton(onBack)
                    Spacer(Modifier.width(10.dp))
                    Text("Taste Profile", style = MaterialTheme.typography.titleLarge)
                }
            }
            item("intro") {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    Text(
                        "What your eye is learning",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        state.profile.plainLanguageSummary(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(1.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Everything here is computed on this device. Nothing is uploaded, profiled or shared.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            item("discovery") {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Room for surprise", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Keep ${(state.profile.discoveryRatio * 100).roundToInt()}% of your feed outside your established taste. This is your filter-bubble insurance.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Slider(
                            value = state.profile.discoveryRatio,
                            onValueChange = onDiscoveryChange,
                            valueRange = 0.1f..0.6f,
                            steps = 4,
                            enabled = state.prefs.tasteLearningEnabled,
                        )
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("More familiar", style = MaterialTheme.typography.labelSmall)
                            Text("More discovery", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            item("toggle") {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Learn from my viewing", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (state.prefs.tasteLearningEnabled) {
                                    "${state.signalCount} on-device signals, decaying over 45 days."
                                } else {
                                    "Learning is off. Your feed shows a broad mix and no new signals are stored."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = state.prefs.tasteLearningEnabled, onCheckedChange = onLearningChange)
                    }
                }
            }

            if (state.profile.interests.isNotEmpty()) {
                item("interests-header") {
                    SectionHeader(
                        "Interests",
                        subtitle = "Tap the pin to keep an interest, the eye to mute it, the bin to remove it.",
                    )
                }
                val grouped = state.profile.interests.groupBy { it.dimension }
                TasteDimension.entries.forEach { dimension ->
                    val interests = grouped[dimension].orEmpty().take(6)
                    if (interests.isNotEmpty()) {
                        item("dimension-${dimension.name}") {
                            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                                Text(
                                    dimension.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                interests.forEach { interest ->
                                    InterestRow(
                                        interest = interest,
                                        onPin = { onPin(interest.key, !interest.pinned) },
                                        onMute = { onMute(interest.key, !interest.muted) },
                                        onRemove = { onRemove(interest.key) },
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                item("empty") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("No taste profile yet", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Look at a few works and linger. I'll notice genres, artists and periods — gently, and only on this device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item("actions") {
                Column(modifier = Modifier.padding(20.dp)) {
                    ArtButton(
                        label = "Reset what I've learned",
                        onClick = { confirmReset = true },
                        icon = Icons.Default.Restore,
                        filled = false,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Resetting removes your on-device signals and interests. It never deletes your journal or collections.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset your taste profile?") },
            text = { Text("This clears only the viewing signals and interests used to personalise your feed. Your saved works and journal stay exactly as they are.") },
            confirmButton = {
                TextButton(onClick = { confirmReset = false; onReset() }) { Text("Reset learning") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Keep it") }
            },
        )
    }
}

@Composable
private fun InterestRow(
    interest: Interest,
    onPin: () -> Unit,
    onMute: () -> Unit,
    onRemove: () -> Unit,
) {
    val muted = interest.muted
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f))
            .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        interest.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (interest.pinned) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                    )
                    if (interest.pinned) {
                        Spacer(Modifier.width(4.dp))
                        Text("📌", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(interest.weight.coerceIn(0.04f, 1f))
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(
                            if (muted) MaterialTheme.colorScheme.outlineVariant
                            else LibraryColors.Ochre,
                        ),
                )
            }
            IconButton(onClick = onPin) {
                Icon(
                    Icons.Default.PushPin,
                    contentDescription = if (interest.pinned) "Unpin ${interest.label}" else "Pin ${interest.label}",
                    tint = if (interest.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            IconButton(onClick = onMute) {
                Icon(
                    Icons.Default.VisibilityOff,
                    contentDescription = if (muted) "Unmute ${interest.label}" else "Mute ${interest.label}",
                    tint = if (muted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Remove ${interest.label}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
