package org.libraryofarts.feature.artwork

import android.app.WallpaperManager
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.libraryofarts.core.common.DateFormats
import org.libraryofarts.core.data.export.ShareFont
import org.libraryofarts.core.data.export.ShareLayout
import org.libraryofarts.core.data.export.ShareRatio
import org.libraryofarts.core.designsystem.component.ArtButton
import org.libraryofarts.core.designsystem.component.GlassCard
import org.libraryofarts.core.designsystem.component.LibraryPill
import org.libraryofarts.core.designsystem.component.LoadingPane
import org.libraryofarts.core.designsystem.component.SectionHeader
import org.libraryofarts.core.designsystem.component.SoftBackdrop
import org.libraryofarts.core.designsystem.haptic.rememberLibraryHaptics
import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.model.EyesFirstComparison
import org.libraryofarts.core.model.LayerKind
import org.libraryofarts.core.model.Mood
import org.libraryofarts.core.ui.ArtworkRow
import org.libraryofarts.core.ui.BackButton
import org.libraryofarts.core.ui.ScreenTitle

@Composable
fun ArtworkRoute(
    onBack: () -> Unit,
    onOpenArtwork: (String) -> Unit,
    onOpenAr: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    viewModel: ArtworkViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val shareFile by viewModel.shareFile.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showJournalDialog by remember { mutableStateOf(false) }
    var showSlowLooking by remember { mutableStateOf(false) }
    var showFrame by remember { mutableStateOf(false) }
    var showScale by remember { mutableStateOf(false) }
    var showShare by remember { mutableStateOf(false) }
    var showAnnotations by remember { mutableStateOf(false) }
    var layerDialog by remember { mutableStateOf<org.libraryofarts.core.model.ArtworkLayer?>(null) }

    DisposableEffect(Unit) {
        onDispose { viewModel.recordSession() }
    }

    LaunchedEffect(shareFile) {
        val file = shareFile ?: return@LaunchedEffect
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share your card"))
        viewModel.clearShareFile()
    }

    LaunchedEffect(state.journalSaved) {
        if (state.journalSaved) {
            showJournalDialog = false
            snackbar.showSnackbar("Saved to your private journal.")
            viewModel.clearJournalSaved()
        }
    }

    SoftBackdrop {
        Box(Modifier.fillMaxSize()) {
            when {
                state.loading -> LoadingPane(label = "Fetching the artwork…")
                state.artwork == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.error ?: "Artwork unavailable", style = MaterialTheme.typography.bodyLarge)
                }
                else -> ArtworkScreen(
                    artwork = state.artwork!!,
                    state = state,
                    onBack = onBack,
                    onToggleSave = viewModel::toggleSave,
                    onShare = { showShare = true },
                    onJournal = { showJournalDialog = true },
                    onSlowLooking = { showSlowLooking = true },
                    onFrame = { showFrame = true },
                    onScale = { showScale = true },
                    onAr = { onOpenAr(state.artwork!!.id) },
                    onZoom = viewModel::markZoomed,
                    onNarrate = viewModel::narrate,
                    onToggleAudio = viewModel::toggleAudio,
                    onPlayAudio = viewModel::play,
                    onToggleBrushstroke = viewModel::toggleBrushstroke,
                    onToggleAnnotations = { showAnnotations = !showAnnotations },
                    onLayer = { layerDialog = it },
                    onWallpaper = {
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) {
                                runCatching {
                                    val url = state.artwork!!.image.fullUrl
                                    WallpaperManager.getInstance(context).setStream(java.net.URL(url).openStream())
                                }.isSuccess
                            }
                            snackbar.showSnackbar(if (ok) "Wallpaper set." else "Wallpaper could not be set on this device.")
                        }
                    },
                    onOpenArtist = onOpenArtist,
                    onSubmitInterpretation = viewModel::compareInterpretation,
                    onSaveInterpretation = { text, mood, tags ->
                        viewModel.saveInterpretation(text, mood, tags)
                    },
                    onShowRelated = onOpenArtwork,
                    showAnnotations = showAnnotations,
                )
            }
            SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }

    val artwork = state.artwork
    if (showSlowLooking && artwork != null) SlowLookingDialog(artwork, onDismiss = { showSlowLooking = false })
    if (showFrame && artwork != null) FramePreviewDialog(artwork, onDismiss = { showFrame = false })
    if (showScale && artwork != null) ScaleCheckDialog(artwork, onDismiss = { showScale = false })
    if (showShare && artwork != null) {
        ShareCardDialog(
            artwork = artwork,
            words = interpretationText(state),
            onRender = { layout, ratio, font ->
                showShare = false
                viewModel.renderShareCard(interpretationText(state), layout, ratio, font)
            },
            onDismiss = { showShare = false },
        )
    }
    if (showJournalDialog && artwork != null) {
        JournalEditorDialog(
            artwork = artwork,
            initialText = interpretationText(state),
            onSave = { text, mood, tags -> viewModel.saveInterpretation(text, mood, tags) },
            onDismiss = { showJournalDialog = false },
        )
    }
    layerDialog?.let { layer ->
        LayerDialog(layer, onDismiss = { layerDialog = null })
    }
}

private fun interpretationText(state: ArtworkUiState): String = state.interpretation

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ArtworkScreen(
    artwork: Artwork,
    state: ArtworkUiState,
    onBack: () -> Unit,
    onToggleSave: () -> Unit,
    onShare: () -> Unit,
    onJournal: () -> Unit,
    onSlowLooking: () -> Unit,
    onFrame: () -> Unit,
    onScale: () -> Unit,
    onAr: () -> Unit,
    onZoom: () -> Unit,
    onNarrate: () -> Unit,
    onToggleAudio: () -> Unit,
    onPlayAudio: (org.libraryofarts.core.model.AudioTrack) -> Unit,
    onToggleBrushstroke: () -> Unit,
    onToggleAnnotations: () -> Unit,
    onLayer: (org.libraryofarts.core.model.ArtworkLayer) -> Unit,
    onWallpaper: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onSubmitInterpretation: (String) -> Unit,
    onSaveInterpretation: (String, Mood, List<String>) -> Unit,
    onShowRelated: (String) -> Unit,
    showAnnotations: Boolean = false,
) {
    val haptics = rememberLibraryHaptics()
    var showDescription by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 130.dp),
    ) {
        item("viewer") {
            Box(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                ArtworkViewer(
                    artwork = artwork,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp),
                    brushstroke = state.brushstroke,
                    showAnnotations = showAnnotations,
                    onZoom = onZoom,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    BackButton(onBack)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircleAction(
                            icon = if (state.saved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            label = if (state.saved) "Saved" else "Save",
                            onClick = { haptics.confirm(); onToggleSave() },
                        )
                        CircleAction(Icons.Default.Share, "Share", onShare)
                    }
                }
            }
        }
        item("title") {
            ScreenTitle(artwork.title, "${artwork.displayArtist} · ${artwork.year}")
        }
        item("license") {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LibraryPill(artwork.license.shortLabel)
                artwork.genre?.let { LibraryPill(it) }
                artwork.period?.let { LibraryPill(it) }
            }
        }
        item("artist-line") {
            val artist = artwork.artist
            if (artist != null) {
                Text(
                    text = "→ Explore ${artist.name}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .clickable { onOpenArtist(artist.name) },
                )
            }
        }
        item("actions") {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ActionChip(if (state.brushstroke) "Brushstroke on" else "Brushstroke", Icons.Default.Brush, onToggleBrushstroke)
                ActionChip(if (state.comparison == null) "Your eyes first" else "Your reading", Icons.Default.Edit, onJournal)
                ActionChip("Slow look", Icons.Default.Headphones, onSlowLooking)
                if (artwork.annotations.isNotEmpty()) ActionChip("Details", Icons.Default.Brush, onToggleAnnotations)
            }
        }
        item("eyes-first") {
            EyesFirstCard(
                artwork = artwork,
                comparison = state.comparison,
                comparing = state.comparing,
                onSubmit = onSubmitInterpretation,
                onSaveJournal = onSaveInterpretation,
                onShowDescription = { showDescription = true },
            )
        }
        item("hearing") {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (state.audioTrack != null) state.audioTrack.title else "A curator's note",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            if (state.audioTrack != null) "Audio commentary" else "Narrated on this device",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (state.audioTrack != null) {
                        ArtButton(if (state.audioPlaying) "Pause" else "Play", onClick = onToggleAudio, icon = Icons.Default.PlayArrow, filled = false)
                    } else {
                        ArtButton("Listen", onClick = onNarrate, icon = Icons.Default.Headphones, filled = false)
                    }
                }
                if (artwork.audio.size > 1) {
                    artwork.audio.drop(1).forEach { track ->
                        Text(
                            "▸ ${track.title}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 5.dp)
                                .clickable { onPlayAudio(track) },
                        )
                    }
                }
            }
        }
        item("description") {
            if (showDescription || artwork.description.isNullOrBlank()) {
                if (!artwork.description.isNullOrBlank()) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                        SectionHeader("The official reading", subtitle = "Now that you've looked first")
                        Text(
                            artwork.description.orEmpty(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(1.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .clickable { showDescription = true }
                        .padding(18.dp),
                ) {
                    Text(
                        "Your eyes first. The museum's reading is waiting when you're ready — tap to reveal it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (artwork.annotations.isNotEmpty()) {
            item("annotations") {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    SectionHeader("Hidden details", subtitle = "Curated notes on symbols, surprises and restoration")
                    artwork.annotations.forEach { annotation ->
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    annotation.kind.label.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(annotation.title, style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    annotation.body,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
        if (artwork.layers.isNotEmpty()) {
            item("layers") {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    SectionHeader("Look beneath the surface", subtitle = "Imaging supplied by the museum")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        artwork.layers.forEach { layer ->
                            LibraryPill(layer.kind.label, onClick = { onLayer(layer) })
                        }
                    }
                }
            }
        }
        item("tools") {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                SectionHeader("In your space", subtitle = "Bring the scale of the work into the room")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ActionChip("Frame preview", Icons.Default.Brush, onFrame)
                    ActionChip("Scale check", Icons.Default.Brush, onScale)
                    ActionChip("See on my wall", Icons.Default.Brush, onAr)
                    ActionChip("Wallpaper", Icons.Default.Wallpaper, onWallpaper)
                }
            }
        }
        item("facts") {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                SectionHeader("Credit & provenance", subtitle = "Everything you need to cite this work")
                org.libraryofarts.core.ui.ArtworkFacts(artwork)
                Spacer(Modifier.height(8.dp))
                if (artwork.provenance.isNotEmpty()) {
                    Text("Provenance", style = MaterialTheme.typography.titleSmall)
                    artwork.provenance.forEach { entry ->
                        Text(
                            entry.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 3.dp),
                        )
                    }
                }
                if (artwork.tags.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        artwork.tags.joinToString("  ") { "#$it" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        item("journal") {
            if (state.journal.isNotEmpty()) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    SectionHeader("Your past looking", subtitle = "Private reflections on this work")
                    state.journal.take(3).forEach { entry ->
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            onClick = onJournal,
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    "${entry.mood.emoji} ${entry.mood.label} · ${DateFormats.formatDate(entry.createdAt)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    entry.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
        item("related") {
            if (state.related.isNotEmpty()) {
                ArtworkRow(
                    title = "Related works",
                    subtitle = "Same hand, same world, or the same mood",
                    artworks = state.related,
                    onArtworkClick = { onShowRelated(it.id) },
                )
            }
        }
        item("footer") {
            Text(
                "Image credit: ${artwork.creditLine.ifBlank { artwork.license.creditLine }}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp),
            )
        }
    }
}

@Composable
private fun CircleAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(Color.Black.copy(alpha = 0.42f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(1.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(1.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(7.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
private fun EyesFirstCard(
    artwork: Artwork,
    comparison: EyesFirstComparison?,
    comparing: Boolean,
    onSubmit: (String) -> Unit,
    onSaveJournal: (String, Mood, List<String>) -> Unit,
    onShowDescription: () -> Unit,
) {
    var text by remember(artwork.id) { mutableStateOf("") }
    var saved by remember(artwork.id) { mutableStateOf(false) }
    val context = LocalContext.current
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { spoken ->
            text = if (text.isBlank()) spoken else "$text $spoken"
        }
    }
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
        SectionHeader("Your eyes first", subtitle = "Before the official explanation, make the looking yours")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp)) {
                if (comparison == null) {
                    androidx.compose.material3.OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        label = { Text("What do you notice?") },
                        placeholder = { Text("Colours, gestures, a detail nobody else seems to mention…") },
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ArtButton(
                            label = "Compare my looking",
                            onClick = { if (text.isNotBlank()) onSubmit(text) },
                            filled = true,
                        )
                        ArtButton(
                            label = "Dictate",
                            onClick = {
                                runCatching {
                                    voiceLauncher.launch(
                                        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say what you notice")
                                        },
                                    )
                                }
                            },
                            icon = Icons.Default.Mic,
                            filled = false,
                        )
                    }
                    if (comparing) {
                        Spacer(Modifier.height(8.dp))
                        Text("Looking closely…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Text(
                        "A warm comparison",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(comparison.encouragement, style = MaterialTheme.typography.bodyMedium)
                    if (comparison.matches.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text("What you saw that the curators saw too", style = MaterialTheme.typography.labelMedium)
                        comparison.matches.forEach { match ->
                            Text("• $match", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (comparison.originalInsights.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text("Your original lens", style = MaterialTheme.typography.labelMedium)
                        comparison.originalInsights.forEach { insight ->
                            Text("• $insight", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    TextButton(onClick = onShowDescription) { Text("Show the art-historical reading") }
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ArtButton(
                            label = if (saved) "Saved" else "Save to journal",
                            onClick = {
                                if (!saved) {
                                    onSaveJournal(text.ifBlank { "A first impression of ${artwork.title}." }, Mood.REFLECTIVE, emptyList())
                                    saved = true
                                }
                            },
                            filled = true,
                        )
                    }
                }
            }
        }
    }
}
