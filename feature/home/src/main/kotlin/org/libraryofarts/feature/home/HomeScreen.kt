package org.libraryofarts.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.libraryofarts.core.designsystem.component.ArtButton
import org.libraryofarts.core.designsystem.component.LoadingPane
import org.libraryofarts.core.designsystem.component.SoftBackdrop
import org.libraryofarts.core.designsystem.haptic.rememberLibraryHaptics
import org.libraryofarts.core.designsystem.theme.EditorialFamily
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
                    item("masthead") {
                        Masthead(
                            streak = state.content?.streak?.currentStreak ?: 0,
                            onOpenLibrary = { haptics.tick(); onOpenLibrary() },
                            onRefresh = { haptics.tick(); onRefresh() },
                        )
                    }

                    if (!state.online) {
                        item("offline") { OfflineBanner() }
                    }

                    state.error?.let { message ->
                        item("error") {
                            ErrorNotice(message = message, onRetry = onRefresh)
                        }
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
                        if (content.continueExploring.isNotEmpty()) {
                            item("continue") {
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
                                TasteNote(summary = content.tasteProfile.plainLanguageSummary(), onOpen = onOpenTaste)
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
                            OpenStacks(onOpenExplore = onOpenExplore)
                        }
                    }
                }
            }
            SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun Masthead(streak: Int, onOpenLibrary: () -> Unit, onRefresh: () -> Unit) {
    val today = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "VOL. 01  ·  DAILY EDITION",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                today.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "The Great\nLibrary of Arts",
            modifier = Modifier.padding(horizontal = 20.dp),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "A museum for slow looking",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (streak > 0) {
                    Text(
                        "$streak DAY${if (streak == 1) "" else "S"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                }
                SquareIconButton(Icons.Default.MenuBook, "Open your library", onOpenLibrary)
                Spacer(Modifier.width(6.dp))
                SquareIconButton(Icons.Default.Refresh, "Refresh the gallery", onRefresh)
            }
        }
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun SquareIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(1.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ErrorNotice(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp))
            .padding(16.dp),
    ) {
        Text(
            "THE LIBRARY NEEDS A MOMENT",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(6.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        ArtButton("Try again", onRetry, filled = false)
    }
}

@Composable
private fun DailyHero(
    artwork: Artwork,
    saved: Boolean,
    onOpen: () -> Unit,
    onSave: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "CAT. 001  ·  TODAY'S WORK",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "OPEN PLATE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.82f)
                .clip(RoundedCornerShape(1.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onOpen),
        ) {
            ArtworkImage(artwork, Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(16.dp))
        Text(
            artwork.title,
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 33.sp, lineHeight = 37.sp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "${artwork.displayArtist}${if (artwork.year.isNotBlank()) " · ${artwork.year}" else ""}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "${artwork.museum.shortName}  ·  ${artwork.license.shortLabel}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
        artwork.description?.let { description ->
            Spacer(Modifier.height(12.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ArtButton(
                label = "Look closely",
                onClick = onOpen,
                icon = Icons.AutoMirrored.Filled.ArrowForward,
            )
            ArtButton(label = if (saved) "Saved" else "Save", onClick = onSave, filled = false)
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
    Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
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
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(artworks, key = { it.id }) { artwork ->
                ArtworkWideCard(
                    artwork = artwork,
                    onClick = { onOpen(artwork) },
                    isSaved = artwork.id in savedIds,
                    onSave = { onSave(artwork) },
                    modifier = Modifier.width(330.dp),
                )
            }
        }
    }
}

@Composable
private fun TasteNote(summary: String, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 22.dp)
            .clickable(onClick = onOpen),
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(104.dp)
                .background(MaterialTheme.colorScheme.primary),
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "WHAT YOUR EYE IS LEARNING",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(7.dp))
            Text(
                summary,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = EditorialFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                ),
            )
            Spacer(Modifier.height(7.dp))
            Text(
                "Private to this device · edit or turn it off any time",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun OpenStacks(onOpenExplore: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp)
            .background(MaterialTheme.colorScheme.onBackground)
            .clickable(onClick = onOpenExplore)
            .padding(24.dp),
    ) {
        Text(
            "THE OPEN STACKS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primaryContainer,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "There is always\nanother room.",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.background,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Search every museum, travel by century, follow a colour or an artist's hand.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.75f),
        )
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "ENTER THE COLLECTION",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.background,
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.background,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
