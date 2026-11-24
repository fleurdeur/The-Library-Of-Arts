package org.libraryofarts.feature.artwork

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import org.libraryofarts.core.data.export.ShareFont
import org.libraryofarts.core.data.export.ShareLayout
import org.libraryofarts.core.data.export.ShareRatio
import org.libraryofarts.core.designsystem.component.ArtButton
import org.libraryofarts.core.designsystem.component.GlassCard
import org.libraryofarts.core.designsystem.component.LibraryPill
import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.model.ArtworkLayer
import org.libraryofarts.core.model.Mood
import org.libraryofarts.core.ui.ArtworkImage
import kotlin.math.roundToInt

private val slowLookingPrompts = listOf(
    "Breathe once. Name the first thing you notice — without judging it.",
    "Follow one colour through the picture. Where does it repeat?",
    "Find the smallest detail you can see. Describe it plainly.",
    "Notice where your eyes rest. That's often the emotional centre.",
    "Write one sentence you would tell a friend. Keep it.",
)

@Composable
fun SlowLookingDialog(artwork: Artwork, onDismiss: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var seconds by remember { mutableIntStateOf(40) }
    var running by remember { mutableStateOf(true) }

    LaunchedEffect(step, running) {
        while (running && seconds > 0) {
            kotlinx.coroutines.delay(1_000)
            seconds -= 1
        }
        if (seconds <= 0 && running) {
            if (step < slowLookingPrompts.lastIndex) {
                step += 1
                seconds = 40
            } else {
                running = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(1.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Slow Looking", style = MaterialTheme.typography.titleLarge)
                Text(
                    artwork.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(18.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(1.dp)),
                ) {
                    ArtworkImage(artwork, Modifier.fillMaxWidth().height(220.dp))
                }
                Spacer(Modifier.height(18.dp))
                LinearProgressIndicator(
                    progress = { (step + (1f - seconds / 40f)) / slowLookingPrompts.size },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    slowLookingPrompts[step],
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (running) "$seconds seconds · step ${step + 1} of ${slowLookingPrompts.size}" else "Take the last sentence with you.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { running = !running }) {
                        Text(if (running) "Pause" else "Continue")
                    }
                    TextButton(onClick = {
                        if (step < slowLookingPrompts.lastIndex) {
                            step += 1
                            seconds = 40
                        } else {
                            onDismiss()
                        }
                    }) { Text(if (step < slowLookingPrompts.lastIndex) "Next prompt" else "Finish") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}

@Composable
fun FramePreviewDialog(artwork: Artwork, onDismiss: () -> Unit) {
    var frame by remember { mutableIntStateOf(0) }
    var mat by remember { mutableStateOf(true) }
    val frameNames = listOf("Gilded wood", "Black lacquer", "Natural oak")
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(1.dp), color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text("Frame Preview", style = MaterialTheme.typography.titleLarge)
                Text(
                    "A wall-scale mock-up. Colours are approximate; your screen is not a gallery light.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                val frameColor = when (frame) {
                    0 -> Color(0xFF8C6B32)
                    1 -> Color(0xFF171513)
                    else -> Color(0xFF8A6A4A)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEFE7D8))
                        .padding(18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(18.dp, frameColor, RoundedCornerShape(2.dp))
                            .padding(if (mat) 22.dp else 0.dp)
                            .background(if (mat) Color(0xFFF8F4EB) else Color.Transparent)
                            .padding(if (mat) 10.dp else 0.dp),
                    ) {
                        ArtworkImage(
                            artwork,
                            Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    frameNames.forEachIndexed { index, name ->
                        LibraryPill(name, selected = frame == index, onClick = { frame = index })
                    }
                }
                Spacer(Modifier.height(8.dp))
                LibraryPill("Museum mat", selected = mat, onClick = { mat = !mat })
                Spacer(Modifier.height(16.dp))
                ArtButton("Set as wallpaper", onClick = onDismiss)
            }
        }
    }
}

@Composable
fun ScaleCheckDialog(artwork: Artwork, onDismiss: () -> Unit) {
    val numbers = Regex("\\d+(?:\\.\\d+)?").findAll(artwork.dimensions.orEmpty())
        .mapNotNull { it.value.toFloatOrNull() }.toList()
    val heightCm = numbers.getOrNull(1) ?: numbers.firstOrNull() ?: 70f
    val widthCm = numbers.getOrNull(0) ?: (heightCm * artwork.image.aspectRatio)
    val humanCm = 170f
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(1.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Scale Check", style = MaterialTheme.typography.titleLarge)
                Text(
                    "${artwork.title} is about ${widthCm.roundToInt()} × ${heightCm.roundToInt()} cm. A 170 cm person is shown beside it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 27.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .height((300f * heightCm / humanCm).coerceIn(100f, 300f).dp)
                                .background(Color(0xFFD9C7A7), RoundedCornerShape(4.dp)),
                        ) { ArtworkImage(artwork, Modifier.fillMaxWidth().height((300f * heightCm / humanCm).coerceIn(100f, 300f).dp)) }
                        HumanSilhouette(
                            modifier = Modifier
                                .width(52.dp)
                                .height(300.dp),
                        )
                    }
                    Text(
                        "170 cm",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Dimensions are transcribed from the museum record. Frames and mounts add extra size.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    }
}

@Composable
private fun HumanSilhouette(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val paint = androidx.compose.ui.graphics.Paint()
        val w = size.width
        val h = size.height
        val color = Color(0xFF5B4636)
        drawCircle(color = color, radius = w * 0.18f, center = androidx.compose.ui.geometry.Offset(w / 2, h * 0.1f))
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.30f, h * 0.17f),
            size = androidx.compose.ui.geometry.Size(w * 0.40f, h * 0.42f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.15f),
        )
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.38f, h * 0.56f),
            size = androidx.compose.ui.geometry.Size(w * 0.24f, h * 0.42f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.10f),
        )
    }
}

@Composable
fun ShareCardDialog(
    artwork: Artwork,
    words: String,
    onRender: (ShareLayout, ShareRatio, ShareFont) -> Unit,
    onDismiss: () -> Unit,
) {
    var layout by remember { mutableStateOf(ShareLayout.MUSEUM_LABEL) }
    var ratio by remember { mutableStateOf(ShareRatio.STORY) }
    var font by remember { mutableStateOf(ShareFont.SANS) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(1.dp), color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text("Share a card", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Your words, the artwork and its credit — exported through the share sheet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Text("Layout", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShareLayout.entries.forEach { option ->
                        LibraryPill(option.label, selected = layout == option, onClick = { layout = option })
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Format", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShareRatio.entries.forEach { option ->
                        LibraryPill(option.label, selected = ratio == option, onClick = { ratio = option })
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Type", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShareFont.entries.forEach { option ->
                        LibraryPill(option.label, selected = font == option, onClick = { font = option })
                    }
                }
                if (words.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "“$words”",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(18.dp))
                ArtButton("Create card", onClick = { onRender(layout, ratio, font) })
            }
        }
    }
}

@Composable
fun JournalEditorDialog(
    artwork: Artwork,
    initialText: String,
    onSave: (String, Mood, List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initialText) }
    var mood by remember { mutableStateOf(Mood.REFLECTIVE) }
    var tags by remember { mutableStateOf(artwork.tags.take(2).joinToString(", ")) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(1.dp), color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text("Save to your Journal", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Private, searchable, encrypted on this device.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Your interpretation") },
                    minLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Text("Mood", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Mood.all.forEach { option ->
                        LibraryPill(
                            "${option.emoji} ${option.label}",
                            selected = mood == option,
                            onClick = { mood = option },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags, separated by commas") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                ArtButton(
                    "Save reflection",
                    onClick = {
                        onSave(
                            text.trim(),
                            mood,
                            tags.split(',').map { it.trim() }.filter { it.isNotBlank() },
                        )
                    },
                )
            }
        }
    }
}

private fun org.libraryofarts.core.model.StudyLens.matrix(): ColorMatrix? = when (this) {
    org.libraryofarts.core.model.StudyLens.NONE -> null
    org.libraryofarts.core.model.StudyLens.INFRARED -> ColorMatrix(
        floatArrayOf(
            0.15f, 1.45f, 0.05f, 0f, 8f,
            0.05f, 0.45f, 0.70f, 0f, 0f,
            0.40f, 0.05f, 1.15f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
    )
    org.libraryofarts.core.model.StudyLens.X_RAY -> ColorMatrix(
        floatArrayOf(
            -0.70f, -0.70f, -0.70f, 0f, 245f,
            -0.70f, -0.70f, -0.70f, 0f, 245f,
            -0.70f, -0.70f, -0.70f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f,
        ),
    )
    org.libraryofarts.core.model.StudyLens.UNDERDRAWING -> ColorMatrix(
        floatArrayOf(
            -0.45f, -0.45f, -0.45f, 0f, 228f,
            -0.35f, -0.35f, -0.35f, 0f, 226f,
            0.80f, 0.80f, 0.80f, 0f, 24f,
            0f, 0f, 0f, 1f, 0f,
        ),
    )
    org.libraryofarts.core.model.StudyLens.BEFORE -> ColorMatrix(
        floatArrayOf(
            0.82f, 0.46f, 0.22f, 0f, 12f,
            0.44f, 0.72f, 0.22f, 0f, 10f,
            0.32f, 0.40f, 0.48f, 0f, 18f,
            0f, 0f, 0f, 1f, 0f,
        ),
    )
    org.libraryofarts.core.model.StudyLens.AFTER -> ColorMatrix(
        floatArrayOf(
            1.22f, -0.08f, -0.05f, 0f, -8f,
            -0.06f, 1.24f, -0.05f, 0f, -8f,
            -0.05f, -0.08f, 1.28f, 0f, -8f,
            0f, 0f, 0f, 1f, 0f,
        ),
    )
}

@Composable
fun TechnicalStudyDialog(
    artwork: Artwork,
    selected: org.libraryofarts.core.model.StudyLens = org.libraryofarts.core.model.StudyLens.NONE,
    onSelect: (org.libraryofarts.core.model.StudyLens) -> Unit = {},
    onDismiss: () -> Unit,
) {
    var lens by remember { mutableStateOf(selected) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(1.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    "TECHNICAL STUDY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
                Text(artwork.title, style = MaterialTheme.typography.titleLarge, maxLines = 2)
                Text(
                    "${artwork.displayArtist} · ${artwork.year.ifBlank { "date unrecorded" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                AsyncImage(
                    model = artwork.image.localPath?.let { java.io.File(it) }
                        ?: artwork.image.thumbnailUrl,
                    contentDescription = "Technical study of ${artwork.title}",
                    contentScale = ContentScale.Fit,
                    colorFilter = lens.matrix()?.let { ColorFilter.colorMatrix(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.25f)
                        .clip(RoundedCornerShape(1.dp))
                        .background(Color(0xFF171512)),
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(org.libraryofarts.core.model.StudyLens.entries) { entry ->
                        LibraryPill(
                            text = entry.label,
                            selected = lens == entry,
                            onClick = {
                                lens = entry
                                onSelect(entry)
                            },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    lens.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "These are interpretive study lenses, not museum imaging. Real X-ray, infrared and restoration plates appear when a source supplies them.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    ArtButton("Close", onClick = onDismiss, filled = false)
                }
            }
        }
    }
}

@Composable
fun LayerDialog(layer: ArtworkLayer, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(1.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(layer.label, style = MaterialTheme.typography.titleLarge)
                Text(
                    layer.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                val context = LocalContext.current
                AsyncImage(
                    model = layer.imageUrl,
                    contentDescription = "${layer.kind.label} view of the artwork",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .clip(RoundedCornerShape(1.dp)),
                )
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    }
}
