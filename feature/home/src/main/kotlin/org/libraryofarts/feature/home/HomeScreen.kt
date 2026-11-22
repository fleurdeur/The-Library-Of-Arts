package org.libraryofarts.feature.home

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.libraryofarts.core.designsystem.component.ArtButton
import org.libraryofarts.core.designsystem.component.GlassCard
import org.libraryofarts.core.designsystem.component.LoadingPane
import org.libraryofarts.core.designsystem.component.SoftBackdrop
import org.libraryofarts.core.designsystem.haptic.rememberLibraryHaptics
import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.ui.ArtworkImage
import org.libraryofarts.core.ui.ArtworkWideCard
import org.libraryofarts.core.ui.FeedRow
import org.libraryofarts.core.ui.OfflineBanner

@Composable
fun HomeRoute(
    onArtworkClick: (String) -> Unit,
    onOpenTasteProfile: () -> Unit,
    onOpenExplore: () -> Unit,
    onOpenLibrary: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onArtworkClick = { artwork -> viewModel.recordDailyView(); onArtworkClick(artwork.id) },
        onSave = viewModel::toggleSave,
        onRefresh = viewModel::refresh,
        onOpenTaste = onOpenTasteProfile,
        onOpenExplore = onOpenExplore,
        onOpenLibrary = onOpenLibrary,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onArtworkClick: (Artwork) -> Unit,
    onSave: (Artwork) -> Unit,
    onRefresh: () -> Unit,
    onOpenTaste: () -> Unit,
    onOpenExplore: () -> Unit,
    onOpenLibrary: () -> Unit,
) {
    val haptics = rememberLibraryHaptics()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.error) {
        state.error?.let { snackbar.showSnackbar(it) }
    }
    SoftBackdrop {
        Box(Modifier.fillMaxSize()) {
            if (state.loading && state.content == null) {
                LoadingPane(label = "Opening the gallery…")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp),
                ) {
                    item("top") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(
                                    "THE GREAT LIBRARY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    "of Arts",
                                    style = MaterialTheme.typography.headlineMedium,
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StreakPill(state.content?.streak?.currentStreak ?: 0)
                                Spacer(Modifier.width(8.dp))
                                IconButton(onClick = { haptics.tick(); onOpenLibrary() }) {
                                    Icon(Icons.Default.MenuBook, contentDescription = "Open your library")
                                }
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                                        .clickable { haptics.tick(); onRefresh() },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Refresh your gallery", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    if (!state.online) {
                        item("offline") { OfflineBanner() }
                    }

                    state.content?.let { content ->
                        item("hero") {
                            DailyHero(
                                artwork = content.daily,
                                saved = content.daily.id in state.savedIds,
                                onOpen = { onArtworkClick(content.daily) },
                                onSave = { onSave(content.daily) },
                            )
                        }
                        item("continue") {
                            if (content.continueExploring.isNotEmpty()) {
                                ContinueRow(
                                    artworks = content.continueExploring,
                                    savedIds = state.savedIds,
                                    onOpen = onArtworkClick,
                                    onSave = onSave,
                                )
                            }
                        }
                        if (content.tasteProfile.learningEnabled) {
                            item("taste") {
                                TasteNoteCard(content.tasteProfile.plainLanguageSummary(), onOpenTaste)
                            }
                        }
                        content.sections.forEach { section ->
                            item("section-${section.id}") {
                                FeedRow(
                                    sectionTitle = section.title,
                                    subtitle = section.subtitle,
                                    items = section.items,
                                    savedIds = state.savedIds,
                                    onArtworkClick = onArtworkClick,
                                    onSave = onSave,
                                )
                            }
                        }
                        item("explore-more") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 24.dp),
                            ) {
                                GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onOpenExplore) {
                                    Row(
                                        modifier = Modifier.padding(20.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Keep exploring", style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                "Search 11 museums, travel by century, follow artist connections.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        ArtButton("Open", onOpenExplore)
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "Every image is public domain or openly licensed. Licence, credit and provenance travel with every work.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp),
                                )
                            }
                        }
                    }
                }
            }
            SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun StreakPill(streak: Int) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🔥", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(5.dp))
        Text(
            if (streak > 0) "$streak day${if (streak == 1) "" else "s"}" else "Begin",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun DailyHero(
    artwork: Artwork,
    saved: Boolean,
    onOpen: () -> Unit,
    onSave: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(470.dp)
                .clip(RoundedCornerShape(36.dp))
                .clickable(onClick = onOpen),
        ) {
            ArtworkImage(artwork, Modifier.fillMaxSize())
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.3f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.86f),
                        ),
                    ),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(18.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("TODAY'S ARTWORK", style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(22.dp),
            ) {
                Text(
                    artwork.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "${artwork.displayArtist} · ${artwork.year}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                )
                Text(
                    "${artwork.museum.shortName} · ${artwork.license.shortLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.65f),
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ArtButton(label = if (saved) "Saved" else "Save", onClick = onSave, icon = Icons.Default.AutoAwesome)
                    ArtButton(label = "Look slowly", onClick = onOpen, filled = false)
                }
            }
        }
    }
}

@Composable
private fun ContinueRow(
    artworks: List<Artwork>,
    savedIds: Set<String>,
    onOpen: (Artwork) -> Unit,
    onSave: (Artwork) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column {
                Text("Continue exploring", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Pick up where your eye left off",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(artworks, key = { it.id }) { artwork ->
                ArtworkWideCard(
                    artwork = artwork,
                    onClick = { onOpen(artwork) },
                    isSaved = artwork.id in savedIds,
                    onSave = { onSave(artwork) },
                    modifier = Modifier.width(320.dp),
                )
            }
        }
    }
}

@Composable
private fun TasteNoteCard(summary: String, onOpen: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onOpen) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("What your eye is learning", style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Private to this device · edit or turn it off any time",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
