package org.libraryofarts.feature.explore

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import android.content.pm.PackageManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import org.libraryofarts.core.designsystem.component.ArtButton
import org.libraryofarts.core.designsystem.component.GlassCard
import org.libraryofarts.core.designsystem.component.LibraryIconButton
import org.libraryofarts.core.designsystem.component.LibraryPill
import org.libraryofarts.core.designsystem.component.SectionHeader
import org.libraryofarts.core.designsystem.component.SoftBackdrop
import org.libraryofarts.core.designsystem.theme.EditorialFamily
import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.model.ArtworkFilter
import org.libraryofarts.core.model.FilterOptions
import org.libraryofarts.core.model.Museum
import org.libraryofarts.core.model.SortOrder
import org.libraryofarts.core.ui.ArtworkCard
import org.libraryofarts.core.ui.ArtworkRow
import org.libraryofarts.core.ui.ArtworkWideCard
import org.libraryofarts.core.ui.ScreenTitle
import org.libraryofarts.core.ui.WorldMapData
import kotlin.math.abs
import kotlin.math.min

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
                ScreenTitle("Explore", "Twelve sources, one library. Search the open collection, wander by century, or follow a colour.")
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
                        placeholder = { Text("Artist, work, mood, museum…") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (state.filter.query.isNotEmpty()) {
                                IconButton(onClick = { onQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(1.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    )
                    Spacer(Modifier.width(8.dp))
                    LibraryIconButton(
                        icon = Icons.Default.Tune,
                        description = "Filters",
                        onClick = onOpenFilters,
                        selected = state.filter.activeFilterCount > 0,
                        size = 56.dp,
                    )
                }
            }
            item("modes") {
                ExploreTabs(selected = state.mode, onSelect = onMode)
            }
            when (state.mode) {
                ExploreMode.BROWSE -> {
                    item("quickfilters") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "THE OPEN COLLECTION",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                "${lazyResults.itemCount} WORKS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                                    .padding(horizontal = 20.dp, vertical = 28.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(1.dp))
                                    .padding(22.dp),
                            ) {
                                Text(
                                    "NO WORKS IN THIS CORNER",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text("Nothing in that corner yet", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "Try a broader word, clear a filter, or browse a museum map.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(12.dp))
                                ArtButton("Clear filters", onClearFilters, filled = false)
                            }
                        }
                    }
                    item("results-header") {
                        if (lazyResults.itemCount > 0) {
                            SectionHeader("Selected works", subtitle = "${lazyResults.itemCount} works from your library and the museums")
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
private fun ExploreTabs(selected: ExploreMode, onSelect: (ExploreMode) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            items(ExploreMode.entries, key = { it.name }) { mode ->
                val active = selected == mode
                Column(
                    modifier = Modifier.clickable { onSelect(mode) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        mode.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 14.dp),
                    )
                    Box(
                        modifier = Modifier
                            .height(2.dp)
                            .width(if (active) 28.dp else 0.dp)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun MuseumWorldMap(museums: List<Museum>, onMuseum: (Museum) -> Unit) {
    var selected by remember { mutableStateOf<Museum?>(null) }
    val markerPositions = remember(museums) {
        museums.associate { museum ->
            museum.id to Offset(
                (((museum.longitude + 180.0) / 360.0).toFloat()).coerceIn(0.02f, 0.98f),
                (((90.0 - museum.latitude) / 180.0).toFloat()).coerceIn(0.04f, 0.92f),
            )
        }
    }
    val land = remember {
        WorldMapData.land.map { points ->
            Path().apply {
                var index = 0
                while (index + 1 < points.size) {
                    val x = points[index]
                    val y = points[index + 1]
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                    index += 2
                }
                close()
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        val mapHeight = when {
            maxWidth < 380.dp -> 250.dp
            maxWidth < 600.dp -> 300.dp
            else -> 340.dp
        }
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(mapHeight)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Color(0xFFE5DDCD))
                    .border(1.dp, Color(0xFFC8BDA8), RoundedCornerShape(1.dp)),
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(museums) {
                            detectTapGestures { tap ->
                                val threshold = 34.dp.toPx()
                                val nearest = markerPositions.minByOrNull { (_, position) ->
                                    val dx = position.x * size.width - tap.x
                                    val dy = position.y * size.height - tap.y
                                    dx * dx + dy * dy
                                }
                                if (nearest != null) {
                                    val position = nearest.value
                                    val dx = position.x * size.width - tap.x
                                    val dy = position.y * size.height - tap.y
                                    if (dx * dx + dy * dy < threshold * threshold) {
                                        selected = museums.first { it.id == nearest.key }
                                    }
                                }
                            }
                        },
                ) {
                    val paper = Color(0xFFE5DDCD)
                    val landFill = Color(0xFFF8F4EB)
                    val coast = Color(0xFFB7AA93)
                    val inkSoft = Color(0xFF857B6B)
                    val vermilion = Color(0xFFA63C26)
                    drawRect(paper)

                    // Quiet graticule, drawn like ruled catalogue paper.
                    val grid = Color(0xFFB9AD99).copy(alpha = 0.38f)
                    for (i in 1..5) {
                        val y = size.height * i / 6f
                        drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.7f)
                    }
                    for (i in 1..11) {
                        val x = size.width * i / 12f
                        drawLine(grid, Offset(x, 0f), Offset(x, size.height), strokeWidth = 0.7f)
                    }
                    // Equator and prime meridian are a little darker.
                    drawLine(grid.copy(alpha = 0.9f), Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), 1.1f)
                    drawLine(grid.copy(alpha = 0.9f), Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), 1.1f)

                    // Land plates with a soft offset shadow and a hairline coast.
                    val shadow = Color(0xFF7C7162).copy(alpha = 0.18f)
                    for (path in land) {
                        withTransform({ scale(size.width, size.height, pivot = Offset.Zero) }) {
                            val shadowPath = Path().apply {
                                addPath(path, Offset(0.0022f, 0.0034f))
                            }
                            drawPath(shadowPath, shadow)
                        }
                    }
                    for (path in land) {
                        withTransform({ scale(size.width, size.height, pivot = Offset.Zero) }) {
                            drawPath(path, landFill)
                            drawPath(path, coast, style = Stroke(width = 0.0017f))
                        }
                    }

                    // Antarctica is deliberately quieter, so the atlas reads as a map.
                    val antarctic = Path().apply {
                        addRect(androidx.compose.ui.geometry.Rect(0f, 0.93f, 1f, 1f))
                    }
                    drawPath(antarctic, paper)

                    val labelPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = inkSoft.copy(alpha = 0.75f).toArgb()
                        textSize = 8.sp.toPx()
                        typeface = android.graphics.Typeface.create("serif", android.graphics.Typeface.ITALIC)
                        letterSpacing = 0.18f
                    }
                    fun oceanLabel(text: String, x: Float, y: Float) {
                        drawContext.canvas.nativeCanvas.drawText(text, x * size.width, y * size.height, labelPaint)
                    }
                    oceanLabel("ATLANTIC", 0.30f, 0.47f)
                    oceanLabel("PACIFIC", 0.06f, 0.52f)
                    oceanLabel("INDIAN OCEAN", 0.68f, 0.58f)
                    oceanLabel("SOUTHERN OCEAN", 0.40f, 0.88f)

                    // Compass rose in the top-right corner.
                    val compassX = size.width - 32.dp.toPx()
                    val compassY = 32.dp.toPx()
                    val rose = Color(0xFF6B604F).copy(alpha = 0.8f)
                    drawCircle(rose.copy(alpha = 0.28f), radius = 15.dp.toPx(), center = Offset(compassX, compassY), style = Stroke(1f))
                    drawLine(rose, Offset(compassX, compassY - 19.dp.toPx()), Offset(compassX, compassY + 14.dp.toPx()), 1.4f)
                    drawLine(rose, Offset(compassX - 14.dp.toPx(), compassY), Offset(compassX + 14.dp.toPx(), compassY), 1.1f)
                    drawLine(rose, Offset(compassX, compassY - 19.dp.toPx()), Offset(compassX - 4.dp.toPx(), compassY - 11.dp.toPx()), 1.4f)
                    drawLine(rose, Offset(compassX, compassY - 19.dp.toPx()), Offset(compassX + 4.dp.toPx(), compassY - 11.dp.toPx()), 1.4f)
                    drawContext.canvas.nativeCanvas.drawText(
                        "N",
                        compassX - 3.2.sp.toPx(),
                        compassY - 23.dp.toPx(),
                        labelPaint.apply { color = Color(0xFF9E3B27).toArgb(); textSize = 9.sp.toPx() },
                    )

                    // Scale bar.
                    val scaleY = size.height - 14.dp.toPx()
                    drawLine(inkSoft, Offset(14.dp.toPx(), scaleY), Offset(14.dp.toPx() + 54.dp.toPx(), scaleY), 1.6f)
                    drawLine(inkSoft, Offset(14.dp.toPx(), scaleY - 3.dp.toPx()), Offset(14.dp.toPx(), scaleY + 3.dp.toPx()), 1.2f)
                    drawLine(inkSoft, Offset(14.dp.toPx() + 54.dp.toPx(), scaleY - 3.dp.toPx()), Offset(14.dp.toPx() + 54.dp.toPx(), scaleY + 3.dp.toPx()), 1.2f)
                    drawContext.canvas.nativeCanvas.drawText(
                        "2 000 KM",
                        14.dp.toPx(),
                        scaleY - 7.dp.toPx(),
                        labelPaint.apply { textSize = 7.sp.toPx(); color = inkSoft.toArgb() },
                    )

                    markerPositions.forEach { (id, position) ->
                        val center = Offset(position.x * size.width, position.y * size.height)
                        val isSelected = selected?.id == id
                        if (isSelected) {
                            drawCircle(vermilion.copy(alpha = 0.15f), radius = 18f, center = center)
                            drawCircle(vermilion.copy(alpha = 0.32f), radius = 13f, center = center, style = Stroke(1.4f))
                        }
                        drawCircle(Color(0xFFF8F4EB), radius = if (isSelected) 8.5f else 7f, center = center)
                        drawCircle(vermilion, radius = if (isSelected) 5.6f else 4.4f, center = center)
                        drawCircle(Color(0xFFF8F4EB).copy(alpha = 0.9f), radius = 1.4f, center = center)
                    }
                }

                Text(
                    "THE MUSEUM ATLAS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF534A3E),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 14.dp, top = 12.dp),
                )
                Text(
                    "PUBLIC COLLECTIONS · DRAWN OFFLINE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF8B7E69),
                    fontSize = 8.sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 14.dp, top = 27.dp),
                )
                Text(
                    "${museums.size} MUSEUMS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF8B7E69),
                    fontSize = 8.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 14.dp, bottom = 10.dp),
                )
            }

            if (selected == null) {
                Text(
                    "Tap a vermilion pin to read the museum's note. No signal, no tiles, no account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 9.dp),
                )
            } else {
                val museum = selected!!
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(1.dp))
                        .padding(14.dp),
                ) {
                    Text(
                        "SELECTED MUSEUM",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(museum.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${museum.city}, ${museum.country} · ${museum.shortName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ArtButton("Open guide", onClick = { onMuseum(museum) }, filled = false)
                        TextButton(onClick = { selected = null }) { Text("Close") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
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
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 6.dp),
                ) {
                    items(SortOrder.entries) { sort ->
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
    DisposableEffect(narrator) {
        onDispose { narrator.release() }
    }
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
            .clip(RoundedCornerShape(1.dp))
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
