package org.libraryofarts.feature.explore

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import android.content.pm.PackageManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import org.libraryofarts.core.designsystem.component.ArtButton
import org.libraryofarts.core.designsystem.component.GlassCard
import org.libraryofarts.core.designsystem.component.LibraryPill
import org.libraryofarts.core.designsystem.component.SectionHeader
import org.libraryofarts.core.designsystem.component.SoftBackdrop
import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.model.ArtworkFilter
import org.libraryofarts.core.model.FilterOptions
import org.libraryofarts.core.model.Museum
import org.libraryofarts.core.model.SortOrder
import org.libraryofarts.core.ui.ArtworkCard
import org.libraryofarts.core.ui.ArtworkRow
import org.libraryofarts.core.ui.ArtworkWideCard
import org.libraryofarts.core.ui.ScreenTitle
import kotlin.math.abs

@Composable
fun ExploreRoute(
    onArtworkClick: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    viewModel: ExploreViewModel = hiltViewModel(),
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val lazyResults = viewModel.results.collectAsLazyPagingItems()
    val context = LocalContext.current
    var showFilters by remember { mutableStateOf(false) }
    var selectedMuseumGuide by remember { mutableStateOf(false) }
    var showArtistGraph by remember { mutableStateOf(false) }

    ExploreScreen(
        state = state,
        lazyResults = lazyResults,
        onMode = viewModel::setMode,
        onQuery = viewModel::setQuery,
        onToggleGenre = viewModel::toggleGenre,
        onTogglePeriod = viewModel::togglePeriod,
        onToggleMuseum = viewModel::toggleMuseum,
        onToggleColor = viewModel::toggleColor,
        onSetColor = viewModel::selectColor,
        onSort = viewModel::setSort,
        onCentury = viewModel::setCentury,
        onClearFilters = viewModel::clearFilters,
        onToggleSave = viewModel::toggleSave,
        onArtworkClick = { onArtworkClick(it.id) },
        onOpenFilters = { showFilters = true },
        onOpenMuseum = { museum ->
            viewModel.openMuseum(museum)
            selectedMuseumGuide = true
        },
        onLocation = viewModel::onLocation,
        onLocationFailure = viewModel::locationUnavailable,
        onArtistGraph = { showArtistGraph = true },
        onOpenArtist = onOpenArtist,
        context = context,
    )

    if (showFilters) {
        FilterDialog(
            filter = state.filter,
            onToggleGenre = viewModel::toggleGenre,
            onTogglePeriod = viewModel::togglePeriod,
            onToggleMuseum = viewModel::toggleMuseum,
            onToggleMedium = viewModel::toggleMedium,
            onToggleMood = viewModel::toggleMood,
            onToggleSubject = viewModel::toggleSubject,
            onSort = viewModel::setSort,
            onClear = { viewModel.clearFilters(); showFilters = false },
            onDismiss = { showFilters = false },
        )
    }

    if (selectedMuseumGuide && state.selectedMuseum != null) {
        MuseumGuideDialog(
            museum = state.selectedMuseum!!,
            guide = state.floorGuide,
            tourStops = state.tourStops,
            onDismiss = { selectedMuseumGuide = false; viewModel.closeMuseum() },
        )
    }

    if (showArtistGraph) {
        ArtistConnectionsDialog(
            connections = state.artistConnections,
            onOpenArtist = onOpenArtist,
            onDismiss = { showArtistGraph = false },
        )
    }
}

@Composable
private fun ExploreScreen(
    state: ExploreUiState,
    lazyResults: androidx.paging.compose.LazyPagingItems<Artwork>,
    onMode: (ExploreMode) -> Unit,
    onQuery: (String) -> Unit,
    onToggleGenre: (String) -> Unit,
    onTogglePeriod: (String) -> Unit,
    onToggleMuseum: (String) -> Unit,
    onToggleColor: (String) -> Unit,
    onSetColor: (String?) -> Unit,
    onSort: (SortOrder) -> Unit,
    onCentury: (Int?) -> Unit,
    onClearFilters: () -> Unit,
    onToggleSave: (Artwork) -> Unit,
    onArtworkClick: (Artwork) -> Unit,
    onOpenFilters: () -> Unit,
    onOpenMuseum: (Museum) -> Unit,
    onLocation: (Double, Double) -> Unit,
    onLocationFailure: (String) -> Unit,
    onArtistGraph: () -> Unit,
    onOpenArtist: (String) -> Unit,
    context: android.content.Context,
) {
    val locationLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (!granted) {
            onLocationFailure("Location permission was declined. You can still use the museum map and floor guides.")
        } else {
            locate(context, onLocation, onLocationFailure)
        }
    }

    SoftBackdrop {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp),
        ) {
            item("title") {
                ScreenTitle("Explore", "Eleven museums, one library. Search, wander, and follow your eye.")
            }
            item("search") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = state.filter.query,
                        onValueChange = onQuery,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search artists, works, moods…") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (state.filter.query.isNotEmpty()) {
                                IconButton(onClick = { onQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(22.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                if (state.filter.activeFilterCount > 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                            )
                            .clickable(onClick = onOpenFilters),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Filters",
                            tint = if (state.filter.activeFilterCount > 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item("modes") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(ExploreMode.entries, key = { it.name }) { mode ->
                        LibraryPill(mode.label, selected = state.mode == mode, onClick = { onMode(mode) })
                    }
                }
            }
            when (state.mode) {
                ExploreMode.BROWSE -> {
                    item("quickfilters") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(FilterOptions.genres) { genre ->
                                LibraryPill(genre, selected = genre in state.filter.genres, onClick = { onToggleGenre(genre) })
                            }
                            items(FilterOptions.periods) { period ->
                                LibraryPill(period, selected = period in state.filter.periods, onClick = { onTogglePeriod(period) })
                            }
                        }
                    }
                    if (state.filter.query.isBlank() && state.filter.activeFilterCount == 0) {
                        item("hidden") {
                            ArtworkRow(
                                title = "Hidden gems",
                                subtitle = "Rarely viewed masterpieces, chosen for a slow look",
                                artworks = state.hiddenGems,
                                savedIds = state.savedIds,
                                onArtworkClick = onArtworkClick,
                                onSave = onToggleSave,
                            )
                        }
                        item("artists") {
                            ArtButton(
                                label = "Artist connections",
                                onClick = onArtistGraph,
                                filled = false,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                            )
                        }
                    }
                    if (lazyResults.itemCount == 0 && lazyResults.loadState.refresh is androidx.paging.LoadState.NotLoading) {
                        item("empty") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text("Nothing in that corner yet", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Try a broader word, clear a filter, or browse a museum map.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(10.dp))
                                ArtButton("Clear filters", onClearFilters, filled = false)
                            }
                        }
                    }
                    item("results-header") {
                        if (lazyResults.itemCount > 0) {
                            SectionHeader("Results", subtitle = "${lazyResults.itemCount} works loaded from your library and the museums")
                        }
                    }
                    items(lazyResults.itemCount, key = { index -> lazyResults.peek(index)?.id ?: index }) { index ->
                        lazyResults[index]?.let { artwork ->
                            ArtworkWideCard(
                                artwork = artwork,
                                onClick = { onArtworkClick(artwork) },
                                isSaved = artwork.id in state.savedIds,
                                onSave = { onToggleSave(artwork) },
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
                            )
                        }
                    }
                }
                ExploreMode.MAP -> {
                    item("map") {
                        MuseumWorldMap(
                            museums = state.museums,
                            onMuseum = onOpenMuseum,
                        )
                    }
                    item("map-list") {
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            SectionHeader("Museums in the library", subtitle = "Tap a marker or a name")
                            state.museums.forEach { museum ->
                                GlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    onClick = { onOpenMuseum(museum) },
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(museum.name, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                "${museum.city}, ${museum.country}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Text(museum.shortName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
                ExploreMode.TIME -> {
                    item("centuries") {
                        TimeMachine(
                            selected = state.selectedCentury,
                            works = state.centuryResults,
                            savedIds = state.savedIds,
                            onCentury = onCentury,
                            onArtworkClick = onArtworkClick,
                            onSave = onToggleSave,
                        )
                    }
                }
                ExploreMode.COLORS -> {
                    item("colors") {
                        ColorSearch(
                            filter = state.filter,
                            results = state.colorResults,
                            savedIds = state.savedIds,
                            onColor = onSetColor,
                            onArtworkClick = onArtworkClick,
                            onSave = onToggleSave,
                        )
                    }
                }
                ExploreMode.VISIT -> {
                    item("visit") {
                        MuseumVisit(
                            state = state,
                            onLocate = {
                                locationLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                                    ),
                                )
                            },
                            onMuseum = onOpenMuseum,
                        )
                    }
                }
            }
        }
    }
}

private fun locate(
    context: android.content.Context,
    onLocation: (Double, Double) -> Unit,
    onFailure: (String) -> Unit,
) {
    val fine = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
    if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
        onFailure("Location permission is not available. The map and floor guides still work.")
        return
    }
    runCatching {
        val client = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
        client.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) onLocation(location.latitude, location.longitude)
                else onFailure("We couldn't get a fix just now. Try again near a window, or use the museum map.")
            }
            .addOnFailureListener { onFailure("Location is unavailable on this device. The map and floor guides still work.") }
    }.onFailure { onFailure("Location is unavailable on this device. The map and floor guides still work.") }
}

@Composable
private fun MuseumWorldMap(museums: List<Museum>, onMuseum: (Museum) -> Unit) {
    var selected by remember { mutableStateOf<Museum?>(null) }
    val markerPositions = remember(museums) {
        museums.associate { museum ->
            museum.id to Offset(
                ((museum.longitude + 180f) / 360f).toFloat(),
                ((90f - museum.latitude) / 180f).toFloat(),
            )
        }
    }
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF12232E)),
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(museums) {
                        detectTapGestures { tap ->
                            val nearest = markerPositions.minByOrNull { (_, position) ->
                                val dx = position.x * size.width - tap.x
                                val dy = position.y * size.height - tap.y
                                dx * dx + dy * dy
                            }
                            if (nearest != null) {
                                val position = nearest.value
                                val dx = position.x * size.width - tap.x
                                val dy = position.y * size.height - tap.y
                                if (dx * dx + dy * dy < 40 * 40) {
                                    selected = museums.first { it.id == nearest.key }
                                    onMuseum(selected!!)
                                }
                            }
                        }
                    },
            ) {
                val ocean = Color(0xFF12232E)
                drawRect(ocean)
                val land = Color(0xFF3D4F48)
                fun p(x: Float, y: Float) = Offset(x * size.width, y * size.height)
                // Rough, stylised continents — legible at phone size, no tile server needed.
                val northAmerica = Path().apply {
                    moveTo(p(0.08f, 0.22f).x, p(0.08f, 0.22f).y)
                    lineTo(p(0.24f, 0.14f).x, p(0.24f, 0.14f).y)
                    lineTo(p(0.30f, 0.28f).x, p(0.30f, 0.28f).y)
                    lineTo(p(0.24f, 0.42f).x, p(0.24f, 0.42f).y)
                    lineTo(p(0.16f, 0.40f).x, p(0.16f, 0.40f).y)
                    close()
                }
                val southAmerica = Path().apply {
                    moveTo(p(0.27f, 0.46f).x, p(0.27f, 0.46f).y)
                    lineTo(p(0.34f, 0.52f).x, p(0.34f, 0.52f).y)
                    lineTo(p(0.31f, 0.72f).x, p(0.31f, 0.72f).y)
                    lineTo(p(0.26f, 0.60f).x, p(0.26f, 0.60f).y)
                    close()
                }
                val europeAfrica = Path().apply {
                    moveTo(p(0.45f, 0.20f).x, p(0.45f, 0.20f).y)
                    lineTo(p(0.56f, 0.18f).x, p(0.56f, 0.18f).y)
                    lineTo(p(0.58f, 0.34f).x, p(0.58f, 0.34f).y)
                    lineTo(p(0.54f, 0.58f).x, p(0.54f, 0.58f).y)
                    lineTo(p(0.47f, 0.50f).x, p(0.47f, 0.50f).y)
                    close()
                }
                val asia = Path().apply {
                    moveTo(p(0.56f, 0.16f).x, p(0.56f, 0.16f).y)
                    lineTo(p(0.82f, 0.20f).x, p(0.82f, 0.20f).y)
                    lineTo(p(0.88f, 0.34f).x, p(0.88f, 0.34f).y)
                    lineTo(p(0.74f, 0.44f).x, p(0.74f, 0.44f).y)
                    lineTo(p(0.60f, 0.34f).x, p(0.60f, 0.34f).y)
                    close()
                }
                val australia = Path().apply {
                    moveTo(p(0.78f, 0.62f).x, p(0.78f, 0.62f).y)
                    lineTo(p(0.90f, 0.64f).x, p(0.90f, 0.64f).y)
                    lineTo(p(0.87f, 0.74f).x, p(0.87f, 0.74f).y)
                    lineTo(p(0.76f, 0.72f).x, p(0.76f, 0.72f).y)
                    close()
                }
                listOf(northAmerica, southAmerica, europeAfrica, asia, australia).forEach {
                    drawPath(it, land)
                    drawPath(it, Color(0xFF6E7F6A).copy(alpha = 0.35f), style = Stroke(width = 1.5f))
                }
                // graticule
                for (i in 1..5) {
                    drawLine(Color.White.copy(alpha = 0.05f), Offset(0f, size.height * i / 6f), Offset(size.width, size.height * i / 6f))
                }
                markerPositions.values.forEach { position ->
                    drawCircle(Color(0xFFE07A5F), radius = 7f, center = Offset(position.x * size.width, position.y * size.height))
                    drawCircle(Color.White, radius = 2.5f, center = Offset(position.x * size.width, position.y * size.height))
                }
            }
            Text(
                "Museum world map · offline",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp),
            )
        }
        selected?.let { museum ->
            Text(
                "${museum.name} · ${museum.city}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun TimeMachine(
    selected: Int?,
    works: List<Artwork>,
    savedIds: Set<String>,
    onCentury: (Int?) -> Unit,
    onArtworkClick: (Artwork) -> Unit,
    onSave: (Artwork) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(FilterOptions.centuries) { century ->
                LibraryPill("${century}th", selected = selected == century, onClick = { onCentury(century) })
            }
        }
        if (selected == null) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Travel by century", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Choose a century to walk through the collection in chronological order. Every work carries its date, even when the exact year is uncertain.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ArtButton("Renaissance", onClick = { onCentury(15) }, filled = false)
                    ArtButton("Baroque", onClick = { onCentury(17) }, filled = false)
                    ArtButton("19th c.", onClick = { onCentury(19) }, filled = false)
                }
            }
        } else {
            SectionHeader("The ${selected}th century", subtitle = "${works.size} works within reach")
            if (works.isEmpty()) {
                Text(
                    "No works from this century are installed yet. Try another century or install a period pack.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(works, key = { it.id }) { artwork ->
                        ArtworkCard(
                            artwork = artwork,
                            onClick = { onArtworkClick(artwork) },
                            isSaved = artwork.id in savedIds,
                            onSave = { onSave(artwork) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSearch(
    filter: ArtworkFilter,
    results: List<Artwork>,
    savedIds: Set<String>,
    onColor: (String?) -> Unit,
    onArtworkClick: (Artwork) -> Unit,
    onSave: (Artwork) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Find a feeling by colour",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Text(
            "Palettes are extracted on-device from each artwork, then matched to these broad families.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FilterOptions.colorSwatches.forEach { swatch ->
                val selected = swatch.name in filter.colors
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(swatch.argb))
                        .border(
                            width = if (selected) 3.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.4f),
                            shape = CircleShape,
                        )
                        .clickable { onColor(if (selected) null else swatch.name) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) Text("✓", color = Color.White)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (results.isEmpty()) {
            Text(
                "Choose a colour to see works with that pigment family.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(results, key = { it.id }) { artwork ->
                    ArtworkCard(
                        artwork = artwork,
                        onClick = { onArtworkClick(artwork) },
                        isSaved = artwork.id in savedIds,
                        onSave = { onSave(artwork) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MuseumVisit(
    state: ExploreUiState,
    onLocate: () -> Unit,
    onMuseum: (Museum) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text("Museum Visit", style = MaterialTheme.typography.titleLarge)
        Text(
            "Find partner museums near you, then download a floor guide before you lose signal. Location is used once, on this screen, and never stored.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        ArtButton("Find museums near me", onClick = onLocate, icon = Icons.Default.MyLocation)
        state.locationState?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(16.dp))
        if (state.nearby.isNotEmpty()) {
            state.nearby.forEach { nearby ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    onClick = { onMuseum(nearby.museum) },
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(nearby.museum.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${nearby.museum.city} · ${nearby.distanceLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        } else {
            Text(
                "No location yet. The offline map and every floor guide are still available below.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            state.museums.take(4).forEach { museum ->
                Text(
                    "▸ ${museum.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMuseum(museum) }
                        .padding(vertical = 7.dp),
                )
            }
        }
    }
}

@Composable
private fun FilterDialog(
    filter: ArtworkFilter,
    onToggleGenre: (String) -> Unit,
    onTogglePeriod: (String) -> Unit,
    onToggleMuseum: (String) -> Unit,
    onToggleMedium: (String) -> Unit,
    onToggleMood: (String) -> Unit,
    onToggleSubject: (String) -> Unit,
    onSort: (SortOrder) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filters") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .height(460.dp),
            ) {
                FilterGroup("Genre", FilterOptions.genres, filter.genres, onToggleGenre)
                FilterGroup("Period", FilterOptions.periods, filter.periods, onTogglePeriod)
                val museumOptions = org.libraryofarts.core.model.MuseumCatalog.all.associateBy { it.shortName }
                FilterGroup(
                    "Museum",
                    museumOptions.keys.toList(),
                    museumOptions.values.filter { it.id in filter.museumIds }.map { it.shortName }.toSet(),
                    { onToggleMuseum(museumOptions.getValue(it).id) },
                )
                FilterGroup("Medium", FilterOptions.mediums, filter.mediums, onToggleMedium)
                FilterGroup("Mood", FilterOptions.moods, filter.moods, onToggleMood)
                FilterGroup("Subject", FilterOptions.subjects, filter.subjects, onToggleSubject)
                Text("Sort", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                    SortOrder.entries.forEach { sort ->
                        LibraryPill(sort.label, selected = filter.sort == sort, onClick = { onSort(sort) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Show results") } },
        dismissButton = { TextButton(onClick = onClear) { Text("Clear all") } },
    )
}

@Composable
private fun FilterGroup(title: String, options: List<String>, selected: Set<String>, onToggle: (String) -> Unit) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
            items(options) { option ->
                LibraryPill(option, selected = option in selected, onClick = { onToggle(option) })
            }
        }
    }
}

@Composable
private fun MuseumGuideDialog(
    museum: Museum,
    guide: String?,
    tourStops: List<org.libraryofarts.core.model.MuseumAudioStop>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val narrator = remember(context) { org.libraryofarts.core.media.Narrator(context) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(museum.name) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).height(430.dp)) {
                Text(
                    "${museum.city}, ${museum.country} · offline floor guide",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                Text(guide ?: "A floor guide will be available in the next pack update.", style = MaterialTheme.typography.bodySmall)
                if (tourStops.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Text("Audio tour", style = MaterialTheme.typography.titleSmall)
                    tourStops.forEach { stop ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { narrator.speak(stop.transcript) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("▶", color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(stop.title, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${stop.gallery} · ${stop.durationSeconds}s · tap to listen",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { narrator.stop(); onDismiss() }) { Text("Close") } },
        dismissButton = {
            TextButton(onClick = { narrator.stop() }) { Text("Stop audio") }
        },
    )
}

@Composable
private fun ArtistConnectionsDialog(
    connections: List<org.libraryofarts.core.model.ArtistConnection>,
    onOpenArtist: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Artist connections") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).height(440.dp)) {
                ArtistGraph(connections)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Teachers, students, rivals and influences. Tap a name to explore their works.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                connections.forEach { connection ->
                    Text(
                        "${connection.fromArtist} → ${connection.toArtist}  ·  ${connection.relation.label}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenArtist(connection.toArtist) }
                            .padding(vertical = 6.dp),
                    )
                    if (connection.note.isNotBlank()) {
                        Text(
                            connection.note,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun ArtistGraph(connections: List<org.libraryofarts.core.model.ArtistConnection>) {
    val names = remember(connections) {
        (connections.map { it.fromArtist } + connections.map { it.toArtist }).distinct().take(6)
    }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = minOf(size.width, size.height) * 0.34f
        val positions = names.mapIndexed { index, _ ->
            val angle = (2 * Math.PI * index / names.size) - Math.PI / 2
            Offset(center.x + (radius * kotlin.math.cos(angle)).toFloat(), center.y + (radius * kotlin.math.sin(angle)).toFloat())
        }
        connections.forEach { connection ->
            val from = names.indexOf(connection.fromArtist)
            val to = names.indexOf(connection.toArtist)
            if (from >= 0 && to >= 0) {
                drawLine(
                    color = Color(0xFFC59A48).copy(alpha = 0.6f),
                    start = positions[from],
                    end = positions[to],
                    strokeWidth = 2f,
                )
            }
        }
        positions.forEach { position ->
            drawCircle(Color(0xFFB5452B), radius = 13f, center = position)
            drawCircle(Color.White.copy(alpha = 0.9f), radius = 5f, center = position)
        }
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 24f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        positions.forEachIndexed { index, position ->
            drawContext.canvas.nativeCanvas.drawText(
                names[index].split(" ").last(),
                position.x,
                position.y + 38f,
                paint,
            )
        }
    }
}
