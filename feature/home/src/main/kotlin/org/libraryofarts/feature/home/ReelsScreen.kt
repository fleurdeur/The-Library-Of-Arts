package org.libraryofarts.feature.home

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File
import kotlinx.coroutines.launch
import org.libraryofarts.core.designsystem.component.ArtButton
import org.libraryofarts.core.designsystem.component.EmptyState
import org.libraryofarts.core.designsystem.component.LibraryIconButton
import org.libraryofarts.core.designsystem.component.SoftBackdrop
import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.ui.ArtworkImage
import org.libraryofarts.core.ui.BackButton
import org.libraryofarts.core.ui.ScreenTitle

@Composable
fun ReelsRoute(
    onBack: () -> Unit,
    onOpenArtwork: (String) -> Unit,
    viewModel: ReelsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ReelsScreen(
        state = state,
        onBack = onBack,
        onOpenArtwork = onOpenArtwork,
        onLike = viewModel::toggleLike,
        onAddComment = viewModel::addComment,
        onRemoveComment = viewModel::removeComment,
        onRename = viewModel::requestNameChange,
        onDownload = { artwork, context, onSaved -> viewModel.download(artwork, context, onSaved) },
        galleryFiles = viewModel::galleryFiles,
        likeCount = viewModel::likeCountFor,
        commentsFor = viewModel::commentsFor,
        onMessageShown = viewModel::consumeMessage,
    )
}

@Composable
fun ReelsScreen(
    state: ReelsUiState,
    onBack: () -> Unit,
    onOpenArtwork: (String) -> Unit,
    onLike: (String) -> Unit,
    onAddComment: (String, String) -> Unit,
    onRemoveComment: (String, String) -> Unit,
    onRename: (String, (Boolean) -> Unit) -> Unit,
    onDownload: (Artwork, android.content.Context, (File?) -> Unit) -> Unit,
    galleryFiles: () -> List<File>,
    likeCount: (String, Boolean) -> Int,
    commentsFor: (String) -> List<org.libraryofarts.core.data.social.ReelComment>,
    onMessageShown: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showGallery by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var commentFor by remember { mutableStateOf<Artwork?>(null) }
    var sharedText by remember { mutableStateOf<Artwork?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            onMessageShown()
        }
    }

    SoftBackdrop {
        if (state.pool.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BackButton(onBack)
                    Spacer(Modifier.width(10.dp))
                    Text("Reels", style = MaterialTheme.typography.titleLarge)
                }
                EmptyState(
                    "No plates yet",
                    "Pull down on Home once and the reels will fill themselves.",
                )
            }
            return@SoftBackdrop
        }

        val half = state.pool.size / 2
        val pagerState = rememberPagerState(initialPage = half, pageCount = { Int.MAX_VALUE })
        val currentIndex by remember {
            derivedStateOf { pagerState.currentPage }
        }

        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
                contentPadding = PaddingValues(top = 64.dp, bottom = 88.dp),
                pageSpacing = 12.dp,
            ) { page ->
                val artwork = state.pool[((page % state.pool.size) + state.pool.size) % state.pool.size]
                ReelPage(
                    artwork = artwork,
                    liked = artwork.id in state.liked,
                    count = likeCount(artwork.id, artwork.id in state.liked),
                    comments = commentsFor(artwork.id).size,
                    saving = artwork.id in state.saving,
                    onOpen = { onOpenArtwork(artwork.id) },
                    onLike = { onLike(artwork.id) },
                    onComment = { commentFor = artwork },
                    onShare = { sharedText = artwork },
                    onDownload = { onDownload(artwork, context) { } },
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BackButton(onBack)
                Spacer(Modifier.width(10.dp))
                Text(
                    "REELS · ${state.pool.size} PLATES",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { showNameDialog = true }) {
                    Text(
                        "As ${state.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                LibraryIconButton(
                    icon = Icons.Default.Download,
                    description = "Your gallery",
                    onClick = { showGallery = true },
                )
            }
        }
    }

    if (showGallery) {
        GalleryDialog(files = galleryFiles(), onDismiss = { showGallery = false })
    }
    if (showNameDialog) {
        NameDialog(
            current = state.name,
            cooldownLeft = state.cooldownLeft,
            onRename = onRename,
            onDismiss = { showNameDialog = false },
        )
    }
    commentFor?.let { artwork ->
        CommentsDialog(
            artwork = artwork,
            comments = commentsFor(artwork.id),
            onAdd = { text -> onAddComment(artwork.id, text) },
            onRemove = { id -> onRemoveComment(id, artwork.id) },
            onDismiss = { commentFor = null },
        )
    }
    sharedText?.let { artwork ->
        SharePlateDialog(
            artwork = artwork,
            onDismiss = { sharedText = null },
        )
    }
}

@Composable
private fun ReelPage(
    artwork: Artwork,
    liked: Boolean,
    count: Int,
    comments: Int,
    saving: Boolean,
    onOpen: () -> Unit,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit,
) {
    var scale by remember(artwork.id) { mutableFloatStateOf(1f) }
    var offsetX by remember(artwork.id) { mutableFloatStateOf(0f) }
    var offsetY by remember(artwork.id) { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        val plateHeight = maxHeight
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(plateHeight)
                .clip(RoundedCornerShape(1.dp))
                .background(Color(0xFF171512)),
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
                            scale = (scale * zoom).coerceIn(1f, 6f)
                            val maxX = size.width * (scale - 1) / 2f
                            val maxY = size.height * (scale - 1) / 2f
                            offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                            offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                            if (scale <= 1.02f) {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    }
                    .pointerInput(artwork.id) {
                        detectTapGestures(
                            onDoubleTap = { position ->
                                if (scale > 1.6f) {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                } else {
                                    scale = 2.8f
                                    val cx = size.width / 2f
                                    val cy = size.height / 2f
                                    offsetX = (cx - position.x) * 1.7f
                                    offsetY = (cy - position.y) * 1.7f
                                }
                            },
                        )
                    },
            ) {
                ArtworkImage(
                    artwork = artwork,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(onClick = onOpen)
                    .padding(14.dp),
            ) {
                Text(
                    artwork.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${artwork.displayArtist} · ${artwork.year.ifBlank { "date unrecorded" }} · ${artwork.museum.shortName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE9E2D4),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LibraryIconButton(
                        icon = if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        description = if (liked) "Liked" else "Like",
                        onClick = onLike,
                        selected = liked,
                        size = 46.dp,
                    )
                    Text(
                        count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LibraryIconButton(
                        icon = Icons.Default.ChatBubbleOutline,
                        description = "Comments",
                        onClick = onComment,
                        size = 46.dp,
                    )
                    Text(
                        comments.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
                LibraryIconButton(
                    icon = Icons.Default.Share,
                    description = "Share",
                    onClick = onShare,
                    size = 46.dp,
                )
                LibraryIconButton(
                    icon = Icons.Default.Download,
                    description = if (saving) "Saving…" else "Save to your gallery",
                    onClick = onDownload,
                    size = 46.dp,
                )
            }
        }
    }
}

@Composable
private fun GalleryDialog(files: List<File>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your gallery") },
        text = {
            if (files.isEmpty()) {
                Text(
                    "Nothing saved yet. Use the download arrow on a reel.",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                LazyColumn(modifier = Modifier.height(420.dp)) {
                    items(files, key = { it.absolutePath }) { file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            coil3.compose.AsyncImage(
                                model = file,
                                contentDescription = file.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(1.dp)),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(file.nameWithoutExtension, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "%.0f KB".format(file.length() / 1024.0),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun NameDialog(
    current: String,
    cooldownLeft: Long,
    onRename: (String, (Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(current) }
    var error by remember { mutableStateOf<String?>(null) }
    val days = (cooldownLeft / 86_400_000L).toInt()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your name on the reels") },
        text = {
            Column {
                Text(
                    "No account: the name lives on this device. It can change once every 30 days.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Display name") },
                )
                if (days > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "You can change it again in $days day${if (days == 1) "" else "s"}.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = days == 0 && name.trim().length >= 3,
                onClick = {
                    onRename(name) { ok ->
                        if (ok) onDismiss() else error = "The change was not saved. Try again."
                    }
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun CommentsDialog(
    artwork: Artwork,
    comments: List<org.libraryofarts.core.data.social.ReelComment>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val locale = LocalLocale.current.platformLocale
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notes on “${artwork.title}”", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column {
                if (comments.isEmpty()) {
                    Text(
                        "No notes yet. What did you see?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(modifier = Modifier.height(260.dp)) {
                        items(comments, key = { it.id }) { comment ->
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        comment.author,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        java.text.SimpleDateFormat("d MMM", locale)
                                            .format(java.util.Date(comment.createdAt)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(comment.text, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Add a note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank(),
                onClick = {
                    onAdd(text)
                    text = ""
                },
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun SharePlateDialog(artwork: Artwork, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share this plate") },
        text = {
            Text(
                "“${artwork.title}” by ${artwork.displayArtist} — ${artwork.museum.name}. Found in The Great Library of Arts.",
                style = MaterialTheme.typography.bodySmall,
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "“${artwork.title}” by ${artwork.displayArtist} — ${artwork.museum.name}. From The Great Library of Arts.",
                        )
                    }
                    context.startActivity(Intent.createChooser(intent, "Share"))
                    onDismiss()
                },
            ) { Text("Share") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
