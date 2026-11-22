package org.libraryofarts.feature.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.libraryofarts.core.common.DateFormats
import org.libraryofarts.core.designsystem.component.EmptyState
import org.libraryofarts.core.designsystem.component.GlassCard
import org.libraryofarts.core.designsystem.component.LibraryPill
import org.libraryofarts.core.designsystem.component.SoftBackdrop
import org.libraryofarts.core.model.JournalEntry
import org.libraryofarts.core.model.Mood
import org.libraryofarts.core.ui.BackButton
import org.libraryofarts.core.ui.ScreenTitle

@Composable
fun JournalRoute(
    onBack: () -> Unit,
    onOpenArtwork: (String) -> Unit,
    onOpenToday: () -> Unit,
    viewModel: JournalViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    JournalScreen(
        state = state,
        onBack = onBack,
        onSearch = viewModel::search,
        onMood = viewModel::selectMood,
        onDelete = viewModel::delete,
        onOpenArtwork = onOpenArtwork,
        onOpenToday = onOpenToday,
    )
}

@Composable
fun JournalScreen(
    state: JournalUiState,
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    onMood: (Mood?) -> Unit,
    onDelete: (JournalEntry) -> Unit,
    onOpenArtwork: (String) -> Unit,
    onOpenToday: () -> Unit,
) {
    var entryToDelete by remember { mutableStateOf<JournalEntry?>(null) }
    SoftBackdrop {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp),
        ) {
            item("top") {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BackButton(onBack)
                    Spacer(Modifier.width(10.dp))
                    Text("Your Journal", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "🔒 private",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            item("intro") {
                ScreenTitle(
                    "A record of your looking",
                    "Interpretations stay on this device. Search them, tag them, and watch your eye change over time.",
                )
            }
            item("search") {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onSearch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    placeholder = { Text("Search your words, artists or tags") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { onSearch("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                )
            }
            item("moods") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(Mood.all, key = { it.name }) { mood ->
                        LibraryPill(
                            text = "${mood.emoji} ${mood.label}",
                            selected = state.mood == mood,
                            onClick = { onMood(mood) },
                        )
                    }
                }
            }
            if (state.timeline.isNotEmpty()) {
                item("timeline-header") {
                    Text(
                        "Your reflection timeline",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    )
                }
                item("timeline") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.timeline, key = { it.monthKey }) { point ->
                            GlassCard(
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.width(132.dp),
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(point.label, style = MaterialTheme.typography.labelMedium)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "${point.entries} ${if (point.entries == 1) "entry" else "entries"}",
                                        style = MaterialTheme.typography.headlineSmall,
                                    )
                                    Text(
                                        point.topMood?.let { "${it.emoji} ${it.label}" } ?: "—",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item("stats") {
                if (state.stats.totalEntries > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Stat("${state.stats.totalEntries}", "entries")
                        Stat("${state.stats.distinctArtworks}", "works")
                        Stat(state.stats.topMoods.firstOrNull()?.substringBefore(" ") ?: "—", "top mood")
                    }
                }
            }
            if (state.entries.isEmpty()) {
                item("empty") {
                    EmptyState(
                        title = if (state.query.isNotBlank() || state.mood != null) "Nothing matches yet" else "Your journal is waiting",
                        message = if (state.query.isNotBlank() || state.mood != null) {
                            "Try a different word or mood — your older interpretations are still here."
                        } else {
                            "Open an artwork, write what you notice before reading the label, and it will be kept here — privately."
                        },
                        actionLabel = "Open today's artwork",
                        onAction = onOpenToday,
                    )
                }
            } else {
                items(state.entries, key = { it.id }) { entry ->
                    JournalEntryCard(
                        entry = entry,
                        onClick = { onOpenArtwork(entry.artworkId) },
                        onDelete = { entryToDelete = entry },
                    )
                }
            }
        }
    }

    entryToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("Delete this reflection?") },
            text = { Text("This removes your words from the journal. It cannot be undone, but your other entries are untouched.") },
            confirmButton = {
                TextButton(onClick = { onDelete(entry); entryToDelete = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { entryToDelete = null }) { Text("Keep") } },
        )
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun JournalEntryCard(entry: JournalEntry, onClick: () -> Unit, onDelete: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 7.dp),
        onClick = onClick,
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(width = 92.dp, height = 112.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (entry.artworkImageUrl.isNotBlank()) {
                    coil3.compose.AsyncImage(
                        model = entry.artworkImageUrl,
                        contentDescription = entry.artworkTitle,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text("🖼️", style = MaterialTheme.typography.headlineMedium)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${entry.mood.emoji} ${entry.mood.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        DateFormats.formatDate(entry.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    entry.artworkTitle.ifBlank { "Artwork" },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    entry.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (entry.tags.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        entry.tags.take(3).joinToString("  ") { "#$it" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.align(Alignment.Top)) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Delete this entry",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
