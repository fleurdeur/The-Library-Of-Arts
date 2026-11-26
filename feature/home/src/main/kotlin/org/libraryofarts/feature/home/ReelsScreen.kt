package org.libraryofarts.feature.home

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.ui.ArtworkImage

@Composable
fun ReelsRoute(
    onBack: () -> Unit,
    onOpenArtwork: (String) -> Unit,
    viewModel: ReelsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val muted by viewModel.muted.collectAsStateWithLifecycle()
    ReelsScreen(
        state = state,
        muted = muted,
        onToggleMuted = viewModel::toggleMuted,
        onEnsureMusic = viewModel::ensureMusic,
        onRetryMusic = viewModel::retryMusic,
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
    muted: Boolean,
    onToggleMuted: () -> Unit,
    onEnsureMusic: (Artwork) -> Unit,
    onRetryMusic: (Artwork) -> Unit,
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
    val context = LocalContext.current
    var showGallery by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var commentFor by remember { mutableStateOf<Artwork?>(null) }
    var sharedText by remember { mutableStateOf<Artwork?>(null) }
    var moreFor by remember { mutableStateOf<Artwork?>(null) }
    var musicFor by remember { mutableStateOf<Artwork?>(null) }
    var paused by remember { mutableStateOf(false) }

    val audio = remember { ReelAudioPlayer(context) }
    DisposableEffect(Unit) { onDispose { audio.release() } }

    val view = LocalView.current
    val window = (context as? Activity)?.window
    DisposableEffect(window, view) {
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val statusWasLight = controller?.isAppearanceLightStatusBars
        val navWasLight = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            statusWasLight?.let { controller?.isAppearanceLightStatusBars = it }
            navWasLight?.let { controller?.isAppearanceLightNavigationBars = it }
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            onMessageShown()
        }
    }

    if (state.pool.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(42.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(Modifier.weight(1f))
                Text("Reels", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(42.dp))
            }
            Column(
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No plates yet", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Browse Home once and the reels will fill themselves.",
                    color = Color(0xFFBDBDBD),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    "Back to Home",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .border(1.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                        .clickable(onClick = onBack)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                )
            }
        }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = state.pool.size * 500,
        pageCount = { Int.MAX_VALUE },
    )
    val currentPage by remember { derivedStateOf { pagerState.currentPage } }
    val activeIndex = ((currentPage % state.pool.size) + state.pool.size) % state.pool.size
    val activeArtwork = state.pool[activeIndex]
    val activeMusic = state.music[activeArtwork.id]

    LaunchedEffect(activeArtwork.id) {
        paused = false
        onEnsureMusic(activeArtwork)
    }
    LaunchedEffect(currentPage) {
        val nextIndex = ((currentPage + 1) % state.pool.size + state.pool.size) % state.pool.size
        onEnsureMusic(state.pool[nextIndex])
    }
    LaunchedEffect(muted) { audio.setMuted(muted) }
    LaunchedEffect(activeMusic?.track?.streamUrl, paused) {
        val track = activeMusic?.track
        if (track != null && !paused) audio.play(track.streamUrl, muted) else audio.pause()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, activeMusic?.track?.streamUrl, paused) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> audio.pause()
                Lifecycle.Event.ON_START -> {
                    val track = activeMusic?.track
                    if (!paused && track != null) audio.play(track.streamUrl, muted)
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
        ) { page ->
            val artwork = state.pool[((page % state.pool.size) + state.pool.size) % state.pool.size]
            ReelPage(
                artwork = artwork,
                liked = artwork.id in state.liked,
                count = likeCount(artwork.id, artwork.id in state.liked),
                comments = commentsFor(artwork.id).size,
                saving = artwork.id in state.saving,
                music = state.music[artwork.id],
                active = page == currentPage,
                paused = page == currentPage && paused,
                onOpen = { onOpenArtwork(artwork.id) },
                onLike = { onLike(artwork.id) },
                onComment = { commentFor = artwork },
                onShare = { sharedText = artwork },
                onDownload = { onDownload(artwork, context) { } },
                onTogglePause = { paused = !paused },
                onMusicInfo = { musicFor = artwork },
                onMore = { moreFor = artwork },
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .align(Alignment.TopStart)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.62f), Color.Transparent),
                    ),
                ),
        )
        ReelTopBar(
            name = state.name,
            muted = muted,
            onBack = onBack,
            onToggleMuted = onToggleMuted,
            onProfile = { showNameDialog = true },
        )
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
    moreFor?.let { artwork ->
        ReelMoreSheet(
            name = state.name,
            onOpen = {
                moreFor = null
                onOpenArtwork(artwork.id)
            },
            onSave = {
                moreFor = null
                onDownload(artwork, context) { }
            },
            onGallery = {
                moreFor = null
                showGallery = true
            },
            onMusic = {
                moreFor = null
                musicFor = artwork
            },
            onProfile = {
                moreFor = null
                showNameDialog = true
            },
            onDismiss = { moreFor = null },
        )
    }
    musicFor?.let { artwork ->
        MusicDialog(
            artwork = artwork,
            music = state.music[artwork.id],
            onRetry = { onRetryMusic(artwork) },
            onDismiss = { musicFor = null },
        )
    }
}

@Composable
private fun ReelTopBar(
    name: String,
    muted: Boolean,
    onBack: () -> Unit,
    onToggleMuted: () -> Unit,
    onProfile: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(42.dp)) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        Text("Reels", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onToggleMuted, modifier = Modifier.size(42.dp)) {
            Icon(
                imageVector = if (muted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                contentDescription = if (muted) "Unmute" else "Mute",
                tint = Color.White,
                modifier = Modifier.size(23.dp),
            )
        }
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color(0xFF333333))
                .border(1.dp, Color.White.copy(alpha = 0.75f), CircleShape)
                .clickable(onClick = onProfile),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "Y",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(4.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReelPage(
    artwork: Artwork,
    liked: Boolean,
    count: Int,
    comments: Int,
    saving: Boolean,
    music: ReelMusicState?,
    active: Boolean,
    paused: Boolean,
    onOpen: () -> Unit,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit,
    onTogglePause: () -> Unit,
    onMusicInfo: () -> Unit,
    onMore: () -> Unit,
) {
    var expanded by remember(artwork.id) { mutableStateOf(false) }
    var following by remember(artwork.id) { mutableStateOf(false) }
    var saved by remember(artwork.id) { mutableStateOf(false) }
    var captionOverflows by remember(artwork.id) { mutableStateOf(false) }
    var heartAt by remember(artwork.id) { mutableStateOf<Offset?>(null) }
    var heartNonce by remember { mutableIntStateOf(0) }

    val track = music?.track
    val musicLabel = track?.let {
        listOf(it.displayArtist, it.displayTitle).filter { value -> value.isNotBlank() }.joinToString(" · ")
    } ?: if (music?.loading == true) {
        "Finding period music…"
    } else {
        "Original audio · The Great Library"
    }
    val avatarColor = remember(artwork.id) {
        Color(artwork.palette.getOrNull(0)?.argb ?: 0xFF2A2622)
    }
    val initial = artwork.museum.shortName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "M"
    val caption = buildString {
        append("“").append(artwork.title).append("”")
        if (artwork.displayArtist.isNotBlank()) append(" — ").append(artwork.displayArtist)
        if (artwork.year.isNotBlank()) append(", ").append(artwork.year)
        append(". ").append(artwork.museum.name).append(".")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(artwork.id, active) {
                detectTapGestures(
                    onTap = { if (active) onTogglePause() },
                    onDoubleTap = { position ->
                        if (active) {
                            heartAt = position
                            heartNonce += 1
                            if (!liked) onLike()
                        }
                    },
                )
            },
    ) {
        val transition = rememberInfiniteTransition(label = "reel-zoom")
        val zoom by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.07f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 13000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "reel-zoom-value",
        )
        ArtworkImage(
            artwork = artwork,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val value = if (active) zoom else 1f
                    scaleX = value
                    scaleY = value
                },
            contentScale = ContentScale.Crop,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .align(Alignment.TopStart)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .align(Alignment.BottomStart)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f)),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 6.dp, bottom = 92.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            ReelAction(
                icon = if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                label = if (liked) "Unlike" else "Like",
                count = count,
                tint = if (liked) Color(0xFFFF3040) else Color.White,
                onClick = onLike,
            )
            ReelAction(
                icon = Icons.Default.ChatBubbleOutline,
                label = "Comments",
                count = comments,
                onClick = onComment,
            )
            ReelAction(
                icon = Icons.Default.Send,
                label = "Share",
                onClick = onShare,
            )
            ReelAction(
                icon = if (saved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                label = "Save to your gallery",
                loading = saving,
                onClick = {
                    if (!saved) {
                        saved = true
                        onDownload()
                    }
                },
            )
            ReelAction(
                icon = Icons.Default.MoreVert,
                label = "More",
                iconSize = 26.dp,
                onClick = onMore,
            )
            SpinningDisc(
                active = active && !paused,
                label = musicLabel,
                onClick = onMusicInfo,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .fillMaxWidth(0.78f)
                .padding(start = 14.dp, bottom = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(avatarColor)
                        .border(1.5.dp, Color.White.copy(alpha = 0.9f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(initial, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        artwork.museum.shortName,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        artwork.displayArtist,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (following) "Following" else "Follow",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                        .clickable { following = !following }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                caption,
                color = Color.White,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { layout ->
                    if (!expanded) captionOverflows = layout.hasVisualOverflow
                },
            )
            if (expanded || captionOverflows) {
                Text(
                    if (expanded) "less" else "more",
                    color = Color(0xFFD6D6D6),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { expanded = !expanded },
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.clickable(onClick = onMusicInfo),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    musicLabel,
                    color = Color.White,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .basicMarquee(),
                )
                if (music?.loading == true) {
                    Spacer(Modifier.width(6.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(11.dp),
                        color = Color.White,
                        strokeWidth = 1.5.dp,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = active && paused,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Resume",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(76.dp),
            )
        }

        heartAt?.let { position ->
            key(heartNonce) {
                HeartBurst(position)
            }
        }
    }
}

@Composable
private fun ReelAction(
    icon: ImageVector,
    label: String,
    count: Int? = null,
    tint: Color = Color.White,
    iconSize: androidx.compose.ui.unit.Dp = 30.dp,
    loading: Boolean = false,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
        } else {
            Icon(
                icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(iconSize),
            )
        }
        if (count != null) {
            Spacer(Modifier.height(3.dp))
            Text(
                count.toString(),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SpinningDisc(
    active: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "reel-disc")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "reel-disc-angle",
    )
    Box(
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer { rotationZ = if (active) angle else 0f }
            .clip(CircleShape)
            .background(Color(0xFF232323))
            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.MusicNote,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun HeartBurst(position: Offset) {
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(620)
        visible = false
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.4f),
        exit = fadeOut() + scaleOut(targetScale = 0.75f),
        modifier = Modifier.offset {
            IntOffset(
                (position.x - 44.dp.toPx()).toInt(),
                (position.y - 44.dp.toPx()).toInt(),
            )
        },
    ) {
        Icon(
            Icons.Default.Favorite,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(88.dp),
        )
    }
}

@Composable
private fun ReelMoreSheet(
    name: String,
    onOpen: () -> Unit,
    onSave: () -> Unit,
    onGallery: () -> Unit,
    onMusic: () -> Unit,
    onProfile: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF1C1C1E)) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                SheetRow("Open this artwork", onOpen)
                SheetRow("Save to your gallery", onSave)
                SheetRow("Your saved plates", onGallery)
                SheetRow("About the music", onMusic)
                SheetRow("Posting as " + name, onProfile)
                SheetRow("Close", onDismiss)
            }
        }
    }
}

@Composable
private fun SheetRow(text: String, onClick: () -> Unit) {
    Text(
        text,
        color = Color.White,
        fontSize = 15.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
    )
}

@Composable
private fun MusicDialog(
    artwork: Artwork,
    music: ReelMusicState?,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val track = music?.track
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (track != null) "The sound of this reel" else "Period music") },
        text = {
            Column {
                if (track == null) {
                    Text(
                        if (music?.loading == true) {
                            "Looking through Wikimedia Commons, Openverse and the Internet Archive for a public-domain recording from this period…"
                        } else {
                            "No openly licensed recording came back for this period. Check your connection and try again."
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (music?.loading != true) {
                        TextButton(onClick = onRetry) { Text("Try again") }
                    }
                } else {
                    Text(track.displayTitle, style = MaterialTheme.typography.titleMedium)
                    if (track.displayArtist.isNotBlank()) {
                        Text(track.displayArtist, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "“" + artwork.title + "” · " + track.era.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Licence: " + track.license.ifBlank { "open licence" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Source: " + track.source,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Public-domain and openly licensed recordings, credited to their performers and libraries.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            if (track?.pageUrl?.isNotBlank() == true) {
                TextButton(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(track.pageUrl)))
                        }
                    },
                ) { Text("Open source") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
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
