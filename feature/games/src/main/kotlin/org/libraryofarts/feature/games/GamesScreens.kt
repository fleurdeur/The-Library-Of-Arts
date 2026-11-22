package org.libraryofarts.feature.games

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.libraryofarts.core.data.seed.SeedArtworks
import org.libraryofarts.core.designsystem.component.ArtButton
import org.libraryofarts.core.designsystem.component.EmptyState
import org.libraryofarts.core.designsystem.component.GlassCard
import org.libraryofarts.core.designsystem.component.LibraryPill
import org.libraryofarts.core.designsystem.component.SectionHeader
import org.libraryofarts.core.designsystem.component.SoftBackdrop
import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.model.QuizKind
import org.libraryofarts.core.ui.ArtworkCard
import org.libraryofarts.core.ui.ArtworkImage
import org.libraryofarts.core.ui.ArtworkWideCard
import org.libraryofarts.core.ui.BackButton
import org.libraryofarts.core.ui.ScreenTitle
import java.io.File

private enum class Game(val title: String, val subtitle: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    QUIZ("Daily quiz", "Five questions, no timer", Icons.Default.Extension),
    ARTIST("Guess the artist", "Read the brushwork", Icons.Default.Face),
    FAKE("Spot the fake", "Which detail was invented?", Icons.Default.Search),
    DETAIL("Detail hunt", "A close look at a small thing", Icons.Default.Search),
    COMPARE("Compare", "Two works, one eye", Icons.Default.Compare),
    MOOD("Mood discovery", "Find a feeling", Icons.Default.Palette),
    SCAN("Scan a painting", "Identify a work offline", Icons.Default.CameraAlt),
    KIDS("Kids Mode", "Big pictures, simple questions", Icons.Default.Face),
}

@Composable
fun GamesRoute(
    onBack: () -> Unit,
    onOpenQuiz: (String) -> Unit,
    onOpenCompare: () -> Unit,
    onOpenMood: () -> Unit,
    onOpenScan: () -> Unit,
    onOpenKids: () -> Unit,
    onOpenArtwork: (String) -> Unit,
) {
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
                    Text("Play & notice", style = MaterialTheme.typography.titleLarge)
                }
            }
            item("intro") {
                ScreenTitle(
                    "Looking games",
                    "Small exercises that reward attention rather than speed. Everything works without a connection.",
                )
            }
            items(Game.entries, key = { it.name }) { game ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 5.dp),
                    onClick = {
                        when (game) {
                            Game.QUIZ -> onOpenQuiz(QuizKind.GENERAL.name)
                            Game.ARTIST -> onOpenQuiz(QuizKind.GUESS_THE_ARTIST.name)
                            Game.FAKE -> onOpenQuiz(QuizKind.SPOT_THE_FAKE.name)
                            Game.DETAIL -> onOpenQuiz(QuizKind.DETAIL_HUNT.name)
                            Game.COMPARE -> onOpenCompare()
                            Game.MOOD -> onOpenMood()
                            Game.SCAN -> onOpenScan()
                            Game.KIDS -> onOpenKids()
                        }
                    },
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(game.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(game.title, style = MaterialTheme.typography.titleMedium)
                            Text(game.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("→", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            item("note") {
                Text(
                    "Nothing here is scored against other people, and no result leaves the device.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp),
                )
            }
        }
    }
}

@Composable
fun QuizRoute(
    kind: String,
    onBack: () -> Unit,
    onOpenArtwork: (String) -> Unit,
    games: GamesViewModel = hiltViewModel(),
    quiz: QuizViewModel = hiltViewModel(),
) {
    val parsed = runCatching { QuizKind.valueOf(kind) }.getOrDefault(QuizKind.GENERAL)
    val questions = remember(parsed) { games.quiz(parsed) }
    val state by quiz.state.collectAsStateWithLifecycle()
    LaunchedEffect(parsed) { quiz.start(questions) }
    val labels = mapOf(
        QuizKind.GENERAL to "Daily quiz",
        QuizKind.GUESS_THE_ARTIST to "Guess the artist",
        QuizKind.SPOT_THE_FAKE to "Spot the fake",
        QuizKind.DETAIL_HUNT to "Detail hunt",
    )
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
                    Text(labels.getValue(parsed), style = MaterialTheme.typography.titleLarge)
                }
            }
            if (state.finished) {
                item("finished") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("${state.score} / ${state.questions.size}", style = MaterialTheme.typography.displaySmall)
                        Text(
                            if (state.score == state.questions.size) "Every one. Your eye is sharp today." else "A few new things noticed — that is the point.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ArtButton("Play again", onClick = quiz::restart)
                            ArtButton("Back", onClick = onBack, filled = false)
                        }
                    }
                }
            } else {
                val question = state.current
                if (question == null) {
                    item("empty") { EmptyState("No questions yet", "This set is still being prepared.") }
                } else {
                    item("progress") {
                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                            Text(
                                "Question ${state.index + 1} of ${state.questions.size} · Score ${state.score}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth((state.index + 1f) / state.questions.size)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.primary),
                                )
                            }
                        }
                    }
                    item("prompt") {
                        Column(modifier = Modifier.padding(20.dp)) {
                            if (parsed == QuizKind.DETAIL_HUNT) {
                                val artwork = question.artworkId?.let { SeedArtworks.byId[it] }
                                if (artwork != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1.6f)
                                            .clip(RoundedCornerShape(24.dp))
                                            .clipToBounds(),
                                    ) {
                                        ArtworkImage(
                                            artwork,
                                            Modifier
                                                .fillMaxSize()
                                                .graphicsLayer(scaleX = 2.4f, scaleY = 2.4f),
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Text(question.prompt, style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.height(14.dp))
                            question.options.forEachIndexed { index, option ->
                                val isCorrect = state.correct != null && index == question.correctIndex
                                val isWrongPick = state.selected == index && state.correct == false
                                GlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    onClick = { quiz.answer(index) },
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                when {
                                                    isCorrect -> Color(0xFF3E6B4F).copy(alpha = 0.25f)
                                                    isWrongPick -> Color(0xFFB5452B).copy(alpha = 0.2f)
                                                    else -> Color.Transparent
                                                },
                                            )
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            ('A' + index).toString(),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(option, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                        if (isCorrect) Text("✓", color = Color(0xFF6DBE8C))
                                            else if (isWrongPick) Text("✕", color = Color(0xFFE07A5F))
                                    }
                                }
                            }
                        }
                    }
                    if (state.selected != null) {
                        item("explanation") {
                            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                                Text(
                                    question.explanation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    question.artworkId?.let { id ->
                                        TextButton(onClick = { onOpenArtwork(id) }) { Text("Look at the work") }
                                    }
                                    ArtButton("Next", onClick = quiz::next)
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
fun MoodRoute(onBack: () -> Unit, onOpenArtwork: (String) -> Unit, games: GamesViewModel = hiltViewModel()) {
    var mood by remember { mutableStateOf("Calm") }
    val works = remember(mood) { games.mood(mood) }
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
                    Text("Mood discovery", style = MaterialTheme.typography.titleLarge)
                }
            }
            item("intro") {
                ScreenTitle("How do you want to feel?", "Choose a mood. The library answers with works tagged for it.")
            }
            item("chips") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(listOf("Calm", "Awe", "Melancholy", "Joy", "Tension", "Wonder", "Solitude", "Energy")) { option ->
                        LibraryPill(option, selected = mood == option, onClick = { mood = option })
                    }
                }
            }
            item("header") {
                SectionHeader(mood, subtitle = "${works.size} works in this feeling")
            }
            if (works.isEmpty()) {
                item("empty") {
                    Text(
                        "No works tagged for this mood yet. Try another, or install a themed pack.",
                        modifier = Modifier.padding(20.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(works, key = { it.id }) { artwork ->
                ArtworkWideCard(
                    artwork = artwork,
                    onClick = { onOpenArtwork(artwork.id) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
                )
            }
        }
    }
}

@Composable
fun CompareRoute(onBack: () -> Unit, onOpenArtwork: (String) -> Unit, games: GamesViewModel = hiltViewModel()) {
    val options = remember { games.artworksForCompare() }
    var firstIndex by remember { mutableStateOf(0) }
    var secondIndex by remember { mutableStateOf(if (options.size > 1) 1 else 0) }
    var split by remember { mutableFloatStateOf(0.5f) }
    val first = options.getOrNull(firstIndex)
    val second = options.getOrNull(secondIndex)
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
                    Text("Compare", style = MaterialTheme.typography.titleLarge)
                }
            }
            item("intro") {
                ScreenTitle("Two works, one eye", "Drag the divider. What changes when you look from one to the other?")
            }
            if (first == null || second == null) {
                item("empty") { EmptyState("Not enough works", "Install a pack to compare more of the collection.") }
            } else {
                item("compare") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(380.dp)
                            .clip(RoundedCornerShape(26.dp)),
                    ) {
                        ArtworkImage(first, Modifier.fillMaxSize())
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(split)
                                .fillMaxHeight()
                                .clipToBounds(),
                        ) {
                            ArtworkImage(second, Modifier.fillMaxSize())
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(2.dp)
                                .background(Color.White.copy(alpha = 0.8f))
                                .align(Alignment.CenterStart)
                                .graphicsLayer { translationX = size.width * split },
                        )
                    }
                    Slider(
                        value = split,
                        onValueChange = { split = it.coerceIn(0.05f, 0.95f) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
                item("names") {
                    Row(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(first.artist?.name ?: "Unknown", style = MaterialTheme.typography.titleSmall)
                            Text(first.title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text(second.artist?.name ?: "Unknown", style = MaterialTheme.typography.titleSmall)
                            Text(second.title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                item("pickers") {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Left work", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(options.size) { index ->
                                LibraryPill(
                                    options[index].artist?.name?.split(" ")?.last() ?: "? ",
                                    selected = firstIndex == index,
                                    onClick = { firstIndex = index },
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("Right work", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(options.size) { index ->
                                LibraryPill(
                                    options[index].artist?.name?.split(" ")?.last() ?: "? ",
                                    selected = secondIndex == index,
                                    onClick = { secondIndex = index },
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ArtButton("Open left", onClick = { onOpenArtwork(first.id) }, filled = false)
                            ArtButton("Open right", onClick = { onOpenArtwork(second.id) }, filled = false)
                        }
                    }
                }
                item("prompts") {
                    Text(
                        "Prompts: Where does your eye land first in each? Which has more contrast? What did the artist choose to leave out?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun KidsRoute(onBack: () -> Unit, onOpenArtwork: (String) -> Unit, games: GamesViewModel = hiltViewModel()) {
    val works = remember { games.kidsArtworks() }
    var index by remember { mutableStateOf(0) }
    val artwork = works.getOrNull(index)
    SoftBackdrop {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BackButton(onBack)
                Spacer(Modifier.width(10.dp))
                Text("Kids Mode", style = MaterialTheme.typography.titleLarge)
            }
            if (artwork == null) {
                EmptyState("No pictures yet", "Install a pack to add artwork for Kids Mode.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp),
                ) {
                    item("picture") {
                        ArtworkImage(
                            artwork,
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .height(340.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .clickable { onOpenArtwork(artwork.id) },
                        )
                    }
                    item("words") {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(artwork.title, style = MaterialTheme.typography.headlineMedium)
                            Text("by ${artwork.artist}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "What colours can you find?",
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Text(
                                "Can you spot an animal, a person, or a tiny detail? What do you think happens next?",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { if (index > 0) index-- }, enabled = index > 0) { Text("← Back") }
                                ArtButton(
                                    "Another picture",
                                    onClick = { index = if (index + 1 < works.size) index + 1 else 0 },
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Kids Mode hides text-heavy features and keeps navigation simple. Turn it off in Settings.",
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
fun ScanRoute(
    onBack: () -> Unit,
    onOpenArtwork: (String) -> Unit,
    games: GamesViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    val imageCapture = remember { mutableStateOf<ImageCapture?>(null) }
    val scanState by games.scan.collectAsStateWithLifecycle()

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
                    Text("Scan a painting", style = MaterialTheme.typography.titleLarge)
                }
            }
            item("intro") {
                ScreenTitle("Point, frame, identify", "Images are compared on this device. Nothing is uploaded.")
            }
            item("camera") {
                if (!hasPermission) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Camera access lets you identify a work in front of you. The camera is only active on this screen.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        ArtButton("Allow camera", onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) })
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(420.dp)
                            .clip(RoundedCornerShape(26.dp)),
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                PreviewView(ctx).also { previewView ->
                                    val providerFuture = ProcessCameraProvider.getInstance(ctx)
                                    providerFuture.addListener({
                                        runCatching {
                                            val provider = providerFuture.get()
                                            val preview = Preview.Builder().build().also {
                                                it.setSurfaceProvider(previewView.surfaceProvider)
                                            }
                                            val capture = ImageCapture.Builder()
                                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                                .build()
                                            imageCapture.value = capture
                                            provider.unbindAll()
                                            provider.bindToLifecycle(
                                                lifecycleOwner,
                                                CameraSelector.DEFAULT_BACK_CAMERA,
                                                preview,
                                                capture,
                                            )
                                        }
                                    }, ContextCompat.getMainExecutor(ctx))
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(18.dp)
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable {
                                    val capture = imageCapture.value ?: return@clickable
                                    val file = File(context.cacheDir, "scan-${System.currentTimeMillis()}.jpg")
                                    val options = ImageCapture.OutputFileOptions.Builder(file).build()
                                    capture.takePicture(
                                        options,
                                        ContextCompat.getMainExecutor(context),
                                        object : ImageCapture.OnImageSavedCallback {
                                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                                                if (bitmap != null) games.identify(bitmap)
                                                file.delete()
                                            }

                                            override fun onError(exception: ImageCaptureException) {
                                                games.resetScan()
                                            }
                                        },
                                    )
                                },
                        )
                    }
                }
            }
            item("state") {
                Column(modifier = Modifier.padding(20.dp)) {
                    when (val state = scanState) {
                        is ScanState.Idle -> Text(
                            "Fill the frame with the painting and hold steady.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        is ScanState.Scanning -> Text("Looking for a match…", style = MaterialTheme.typography.labelMedium)
                        is ScanState.Message -> Text(state.text, style = MaterialTheme.typography.bodySmall)
                        is ScanState.Found -> {
                            Text(
                                "Match · ${(state.match.confidence * 100).toInt()}% confidence",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(6.dp))
                            ArtworkWideCard(
                                artwork = state.match.artwork,
                                onClick = { onOpenArtwork(state.match.artwork.id) },
                            )
                            Spacer(Modifier.height(6.dp))
                            ArtButton("Scan again", onClick = games::resetScan, filled = false)
                        }
                    }
                }
            }
        }
    }
}
