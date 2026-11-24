package org.libraryofarts.feature.artwork

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.model.ArtworkAnnotation

/**
 * A deep-zoom viewer. Pinch, pan and double-tap; the image is requested at its
 * full resolution so zooming reveals real detail. Hidden-detail markers are
 * drawn in image-relative coordinates, so they follow the artwork as it moves.
 */
@Composable
fun ArtworkViewer(
    artwork: Artwork,
    modifier: Modifier = Modifier,
    brushstroke: Boolean = false,
    showAnnotations: Boolean = false,
    lens: org.libraryofarts.core.model.StudyLens = org.libraryofarts.core.model.StudyLens.NONE,
    onZoom: () -> Unit = {},
) {
    val context = LocalContext.current
    var scale by remember(artwork.id) { mutableFloatStateOf(1f) }
    var offsetX by remember(artwork.id) { mutableFloatStateOf(0f) }
    var offsetY by remember(artwork.id) { mutableFloatStateOf(0f) }
    var doubleTapZoom by remember(artwork.id) { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(true) }
    val annotations = artwork.annotations
    val local = artwork.image.localPath?.let { java.io.File(it) }
    val thumbModel = local ?: artwork.image.thumbnailUrl
    val fullModel = local ?: run {
        val iiiF = artwork.image.iiifBase?.trimEnd('/')
        if (iiiF != null) "$iiiF/full/2000,/0/default.jpg" else artwork.image.fullUrl.ifBlank { artwork.image.thumbnailUrl }
    }
    var fullLoaded by remember(artwork.id) { mutableStateOf(false) }
    val fullAlpha by animateFloatAsState(if (fullLoaded) 1f else 0f, label = "plate-detail")
    val showDetail = fullModel.toString() != thumbModel.toString()
    val description = artwork.image.altText.ifBlank { "${artwork.title} by ${artwork.displayArtist}" }

    Box(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(1.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY,
                )
                .pointerInput(artwork.id) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        if (zoom != 1f) onZoom()
                        scale = (scale * zoom).coerceIn(1f, 8f)
                        val maxX = size.width * (scale - 1) / 2f
                        val maxY = size.height * (scale - 1) / 2f
                        offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                        offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                        if (scale <= 1.02f) {
                            offsetX = 0f
                            offsetY = 0f
                        }
                        showHint = false
                    }
                }
                .pointerInput(artwork.id) {
                    detectTapGestures(
                        onDoubleTap = { position ->
                            onZoom()
                            doubleTapZoom = !doubleTapZoom
                            if (doubleTapZoom) {
                                scale = 3.2f
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                offsetX = (cx - position.x) * 1.8f
                                offsetY = (cy - position.y) * 1.8f
                            } else {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            }
                            showHint = false
                        },
                    )
                },
        ) {
            // Usually already in Coil's memory cache from the card that opened
            // this screen, so the painting appears immediately.
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(if (brushstroke && !showDetail) fullModel else thumbModel)
                    .memoryCacheKey(thumbModel.toString())
                    .diskCacheKey(thumbModel.toString())
                    .build(),
                contentDescription = description,
                contentScale = ContentScale.Fit,
                colorFilter = lensColorFilter(lens),
                modifier = Modifier.fillMaxSize(),
            )
            if (showDetail || brushstroke) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(fullModel)
                        .memoryCacheKey(fullModel.toString())
                        .diskCacheKey(fullModel.toString())
                        .build(),
                    contentDescription = description,
                    contentScale = ContentScale.Fit,
                    colorFilter = lensColorFilter(lens),
                    onSuccess = { fullLoaded = true },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(alpha = if (brushstroke) 1f else fullAlpha),
                )
            }
        }

        if (showAnnotations && annotations.isNotEmpty()) {
            AnnotationMarkers(artwork = artwork, annotations = annotations)
        }

        if (scale > 1f) {
            Text(
                text = "%.1f×".format(scale),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }

        if (showHint) {
            Text(
                text = "Pinch, pan or double-tap to look closer",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(14.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

/** Interpretive colour matrix over the plate; NONE returns null. */
private fun lensColorFilter(lens: org.libraryofarts.core.model.StudyLens): ColorFilter? {
    val matrix = when (lens) {
        org.libraryofarts.core.model.StudyLens.NONE -> return null
        org.libraryofarts.core.model.StudyLens.INFRARED -> floatArrayOf(
            0.15f, 1.45f, 0.05f, 0f, 8f,
            0.05f, 0.45f, 0.70f, 0f, 0f,
            0.40f, 0.05f, 1.15f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        )
        org.libraryofarts.core.model.StudyLens.X_RAY -> floatArrayOf(
            -0.70f, -0.70f, -0.70f, 0f, 245f,
            -0.70f, -0.70f, -0.70f, 0f, 245f,
            -0.70f, -0.70f, -0.70f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f,
        )
        org.libraryofarts.core.model.StudyLens.UNDERDRAWING -> floatArrayOf(
            -0.45f, -0.45f, -0.45f, 0f, 228f,
            -0.35f, -0.35f, -0.35f, 0f, 226f,
            0.80f, 0.80f, 0.80f, 0f, 24f,
            0f, 0f, 0f, 1f, 0f,
        )
        org.libraryofarts.core.model.StudyLens.BEFORE -> floatArrayOf(
            0.82f, 0.46f, 0.22f, 0f, 12f,
            0.44f, 0.72f, 0.22f, 0f, 10f,
            0.32f, 0.40f, 0.48f, 0f, 18f,
            0f, 0f, 0f, 1f, 0f,
        )
        org.libraryofarts.core.model.StudyLens.AFTER -> floatArrayOf(
            1.22f, -0.08f, -0.05f, 0f, -8f,
            -0.06f, 1.24f, -0.05f, 0f, -8f,
            -0.05f, -0.08f, 1.28f, 0f, -8f,
            0f, 0f, 0f, 1f, 0f,
        )
    }
    return ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix(matrix))
}

@Composable
private fun AnnotationMarkers(artwork: Artwork, annotations: List<ArtworkAnnotation>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        annotations.fastForEach { annotation ->
            val x = annotation.x.coerceIn(0f, 1f) * size.width
            val y = annotation.y.coerceIn(0f, 1f) * size.height
            drawCircle(color = Color.White.copy(alpha = 0.9f), radius = 11f, center = Offset(x, y))
            drawCircle(color = Color(0xFFB5452B), radius = 7f, center = Offset(x, y))
        }
    }
}
