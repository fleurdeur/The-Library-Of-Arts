package org.libraryofarts.feature.learn

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
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.libraryofarts.core.common.DateFormats
import org.libraryofarts.core.data.seed.SeedArtworks
import org.libraryofarts.core.designsystem.component.ArtButton
import org.libraryofarts.core.designsystem.component.EmptyState
import org.libraryofarts.core.designsystem.component.GlassCard
import org.libraryofarts.core.designsystem.component.LibraryPill
import org.libraryofarts.core.designsystem.component.SectionHeader
import org.libraryofarts.core.designsystem.component.SoftBackdrop
import org.libraryofarts.core.media.Narrator
import org.libraryofarts.core.model.Flashcard
import org.libraryofarts.core.model.Lesson
import org.libraryofarts.core.model.StoryTour
import org.libraryofarts.core.ui.ArtworkRow
import org.libraryofarts.core.ui.ArtworkWideCard
import org.libraryofarts.core.ui.BackButton
import org.libraryofarts.core.ui.ScreenTitle

private enum class LearnTab(val label: String) {
    TOURS("Story tours"),
    LESSONS("Art School"),
    GLOSSARY("Glossary"),
    ARTISTS("Artists"),
    CARDS("Flashcards"),
}

@Composable
fun LearnRoute(
    onBack: () -> Unit,
    onOpenTour: (String) -> Unit,
    onOpenLesson: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenArtwork: (String) -> Unit,
    onOpenFlashcards: () -> Unit,
    viewModel: LearnViewModel = hiltViewModel(),
) {
    val due by viewModel.dueCards.collectAsStateWithLifecycle()
    val glossaryQuery by viewModel.glossaryQuery.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(LearnTab.TOURS) }

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
                    Text("Art School", style = MaterialTheme.typography.titleLarge)
                }
            }
            item("intro") {
                ScreenTitle(
                    "Learn to see",
                    "Short tours, plain-language lessons, a glossary and a gentle memory practice. No scores, no streak guilt.",
                )
            }
            item("tabs") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(LearnTab.entries, key = { it.name }) { option ->
                        LibraryPill(option.label, selected = tab == option, onClick = { tab = option })
                    }
                }
            }
            when (tab) {
                LearnTab.TOURS -> {
                    items(viewModel.repository.tours, key = { it.id }) { tour ->
                        TourCard(tour, onClick = { onOpenTour(tour.id) })
                    }
                }
                LearnTab.LESSONS -> {
                    items(viewModel.repository.lessons, key = { it.id }) { lesson ->
                        LessonCard(lesson, onClick = { onOpenLesson(lesson.id) })
                    }
                }
                LearnTab.GLOSSARY -> {
                    item("glossary-search") {
                        OutlinedTextField(
                            value = glossaryQuery,
                            onValueChange = viewModel::setGlossaryQuery,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            placeholder = { Text("Search a term or idea") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (glossaryQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setGlossaryQuery("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(1.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        )
                    }
                    items(viewModel.glossary(glossaryQuery), key = { it.term }) { term ->
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row {
                                    Text(term.term, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                    Text(term.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    term.definition,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (term.related.isNotEmpty()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        term.related.joinToString("  ") { "→ $it" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
                LearnTab.ARTISTS -> {
                    items(viewModel.repository.artistProfiles, key = { it.artist.name }) { profile ->
                        ArtistCard(profile.artist.name, profile.summary, onClick = { onOpenArtist(profile.artist.name) })
                    }
                }
                LearnTab.CARDS -> {
                    item("cards") {
                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                            GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onOpenFlashcards) {
                                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Style, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Spaced-repetition flashcards", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "${due.size} cards ready · a minute or two is enough",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    ArtButton("Begin", onOpenFlashcards)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Cards are generated from works in your library. Ratings adjust the interval; difficult cards come back sooner without penalty.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TourCard(tour: StoryTour, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        onClick = onClick,
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Headphones, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tour.title, style = MaterialTheme.typography.titleMedium)
                Text(tour.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${tour.minutes} min · ${tour.stops.size} stops", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Text("→", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun LessonCard(lesson: Lesson, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        onClick = onClick,
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(lesson.category.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(lesson.title, style = MaterialTheme.typography.titleMedium)
                Text(lesson.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${lesson.minutes} min read", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun ArtistCard(name: String, summary: String, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun TourRoute(
    tourId: String,
    onBack: () -> Unit,
    onOpenArtwork: (String) -> Unit,
    viewModel: LearnViewModel = hiltViewModel(),
) {
    val tour = viewModel.tour(tourId)
    val context = LocalContext.current
    val narrator = remember(context) { Narrator(context) }
    var stopIndex by remember(tourId) { mutableStateOf(0) }
    SoftBackdrop {
        if (tour == null) {
            EmptyState("Tour unavailable", "This tour may have been removed from the library.", actionLabel = "Back", onAction = onBack)
        } else {
            val stop = tour.stops.getOrNull(stopIndex)
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
                        Text("Story tour", style = MaterialTheme.typography.titleLarge)
                    }
                }
                item("title") {
                    ScreenTitle(tour.title, "${tour.subtitle} · ${tour.minutes} min")
                }
                item("stop") {
                    stop?.let {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    "Stop ${stopIndex + 1} of ${tour.stops.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(it.title, style = MaterialTheme.typography.headlineSmall)
                                Spacer(Modifier.height(10.dp))
                                ArtworkWideCard(
                                    artwork = SeedArtworks.byId[it.artworkId] ?: SeedArtworks.all.first(),
                                    onClick = { onOpenArtwork(it.artworkId) },
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(it.narration, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ArtButton("Listen", onClick = { narrator.speak("${it.title}. ${it.narration}") }, icon = Icons.Default.Headphones)
                                    ArtButton("Stop", onClick = { narrator.stop() }, filled = false)
                                }
                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(
                                        onClick = { if (stopIndex > 0) stopIndex-- },
                                        enabled = stopIndex > 0,
                                    ) { Text("← Previous") }
                                    TextButton(
                                        onClick = {
                                            narrator.stop()
                                            if (stopIndex < tour.stops.lastIndex) stopIndex++ else onBack()
                                        },
                                    ) { Text(if (stopIndex < tour.stops.lastIndex) "Next stop →" else "Finish tour") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LessonRoute(
    lessonId: String,
    onBack: () -> Unit,
    viewModel: LearnViewModel = hiltViewModel(),
) {
    val lesson = viewModel.lesson(lessonId)
    SoftBackdrop {
        if (lesson == null) {
            EmptyState("Lesson unavailable", "This lesson may have moved.", actionLabel = "Back", onAction = onBack)
        } else {
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
                        Text(lesson.category.label, style = MaterialTheme.typography.titleLarge)
                    }
                }
                item("title") {
                    ScreenTitle(lesson.title, "${lesson.minutes} min · ${lesson.summary}")
                }
                items(lesson.sections) { section ->
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Text(section.heading, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            section.body,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item("movement") {
                    lesson.relatedMovement?.let { movement ->
                        Text(
                            "Related movement: $movement",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistRoute(
    artistName: String,
    onBack: () -> Unit,
    onOpenArtwork: (String) -> Unit,
    viewModel: LearnViewModel = hiltViewModel(),
) {
    val profile = viewModel.artist(artistName)
    SoftBackdrop {
        if (profile == null) {
            EmptyState("Artist not found", "Try searching the collection instead.", actionLabel = "Back", onAction = onBack)
        } else {
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
                        Text("Artist", style = MaterialTheme.typography.titleLarge)
                    }
                }
                item("title") {
                    ScreenTitle(
                        profile.artist.name,
                        listOfNotNull(profile.artist.lifespan, profile.artist.nationality).joinToString(" · "),
                    )
                }
                item("bio") {
                    Text(
                        profile.summary,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
                item("movements") {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        profile.movements.forEach { LibraryPill(it) }
                    }
                }
                item("works-header") {
                    SectionHeader("In the library", subtitle = "Tap a work to look closely")
                }
                items(profile.keyWorks.mapNotNull { SeedArtworks.byId[it] }) { artwork ->
                    ArtworkWideCard(
                        artwork = artwork,
                        onClick = { onOpenArtwork(artwork.id) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                }
                if (profile.connections.isNotEmpty()) {
                    item("connections") {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Connections", style = MaterialTheme.typography.titleMedium)
                            profile.connections.forEach { connection ->
                                Text(
                                    "${connection.fromArtist} → ${connection.toArtist} · ${connection.relation.label}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(vertical = 4.dp),
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
                    }
                }
            }
        }
    }
}

@Composable
fun FlashcardsRoute(
    onBack: () -> Unit,
    viewModel: LearnViewModel = hiltViewModel(),
) {
    val cards by viewModel.dueCards.collectAsStateWithLifecycle()
    var index by remember { mutableStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    val card = cards.getOrNull(index)

    SoftBackdrop {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BackButton(onBack)
                Spacer(Modifier.width(10.dp))
                Text("Memory practice", style = MaterialTheme.typography.titleLarge)
            }
            if (card == null) {
                EmptyState(
                    title = "Nothing due right now",
                    message = "Your next cards will appear when their interval is up. Looking at art is already practice.",
                )
            } else {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "${index + 1} of ${cards.size} due",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(10.dp))
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(card.front, style = MaterialTheme.typography.headlineSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            if (revealed) {
                                Spacer(Modifier.height(20.dp))
                                Text(card.back, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                                card.artworkId?.let { id ->
                                    val artwork = SeedArtworks.byId[id]
                                    if (artwork != null) {
                                        Spacer(Modifier.height(16.dp))
                                        org.libraryofarts.core.ui.ArtworkImage(
                                            artwork,
                                            Modifier
                                                .fillMaxWidth()
                                                .height(240.dp)
                                                .clip(RoundedCornerShape(1.dp)),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    if (!revealed) {
                        ArtButton("Reveal", onClick = { revealed = true })
                    } else {
                        Text("How did that feel?", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Again" to 1, "Hard" to 3, "Good" to 4, "Easy" to 5).forEach { (label, quality) ->
                                ArtButton(
                                    label = label,
                                    onClick = {
                                        viewModel.review(card, quality)
                                        revealed = false
                                        index = if (index + 1 < cards.size) index + 1 else 0
                                    },
                                    filled = quality >= 4,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Intervals are stored on this device. A difficult card simply returns sooner.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
