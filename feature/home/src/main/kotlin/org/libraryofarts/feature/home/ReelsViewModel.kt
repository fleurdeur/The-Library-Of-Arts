package org.libraryofarts.feature.home

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import dagger.Lazy
import org.libraryofarts.core.common.AppDispatchers
import org.libraryofarts.core.data.repository.ArtworkRepository
import org.libraryofarts.core.data.social.ReelComment
import org.libraryofarts.core.data.social.SocialRepository
import org.libraryofarts.core.model.Artwork

data class ReelsUiState(
    val pool: List<Artwork> = emptyList(),
    val likes: Map<String, Int> = emptyMap(),
    val liked: Set<String> = emptySet(),
    val comments: Map<String, List<ReelComment>> = emptyMap(),
    val name: String = "",
    val cooldownLeft: Long = 0L,
    val saving: Set<String> = emptySet(),
    val message: String? = null,
)

@HiltViewModel
class ReelsViewModel @Inject constructor(
    private val artworks: Lazy<ArtworkRepository>,
    private val social: SocialRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ReelsUiState())
    val state: StateFlow<ReelsUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(AppDispatchers.io) {
            val pool = runCatching {
                artworks.get().allCandidates(400)
                    .filter { it.image.thumbnailUrl.isNotBlank() || it.image.localPath != null }
            }.getOrDefault(emptyList())
            val counts = runCatching { social.remoteLikeCounts() }.getOrDefault(emptyMap())
            _state.update {
                it.copy(
                    pool = pool,
                    likes = counts,
                    liked = pool.mapNotNull { artwork ->
                        if (social.isLiked(artwork.id)) artwork.id else null
                    }.toSet(),
                    comments = emptyMap(),
                    name = social.displayName(),
                    cooldownLeft = social.nameChangeCooldownLeft(),
                )
            }
        }
    }

    fun commentsFor(artworkId: String): List<ReelComment> = social.comments(artworkId)

    fun commentsVersion(artworkId: String): Int = _state.value.comments[artworkId]?.size ?: -1

    fun toggleLike(artworkId: String) {
        val added = social.toggleLike(artworkId)
        _state.update { state ->
            state.copy(
                liked = if (added) state.liked + artworkId else state.liked - artworkId,
            )
        }
    }

    fun addComment(artworkId: String, text: String) {
        social.addComment(artworkId, text) ?: return
        _state.update { it.copy(comments = it.comments + (artworkId to social.comments(artworkId))) }
    }

    fun removeComment(commentId: String, artworkId: String) {
        social.removeComment(commentId)
        _state.update { it.copy(comments = it.comments + (artworkId to social.comments(artworkId))) }
    }

    fun requestNameChange(newName: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = social.changeName(newName)
            if (ok) {
                _state.update { it.copy(name = social.displayName(), cooldownLeft = social.nameChangeCooldownLeft()) }
            }
            onResult(ok)
        }
    }

    fun likeCountFor(artworkId: String, liked: Boolean): Int =
        (_state.value.likes[artworkId] ?: 0) + if (liked) 1 else 0

    fun download(artwork: Artwork, context: android.content.Context, onSaved: (File?) -> Unit) {
        if (_state.value.saving.contains(artwork.id)) return
        _state.update { it.copy(saving = it.saving + artwork.id) }
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val loader = SingletonImageLoader.get(context)
                val model = artwork.image.localPath?.let { File(it) } ?: artwork.image.thumbnailUrl
                val request = ImageRequest.Builder(context)
                    .data(model)
                    .allowHardware(false)
                    .build()
                val image = (loader.execute(request) as? coil3.request.SuccessResult)?.image
                    ?: throw IllegalStateException("no image")
                val bitmap = (image as? coil3.BitmapImage)?.bitmap ?: run {
                    val bmp = Bitmap.createBitmap(image.width.coerceAtLeast(1), image.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                    image.draw(android.graphics.Canvas(bmp))
                    bmp
                }
                val dir = social.galleryDir()
                val file = File(dir, "${artwork.id.replace(':', '_')}-${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out) }
                file
            }.getOrNull()
            withContext(Dispatchers.Main) {
                _state.update { it.copy(saving = it.saving - artwork.id, message = if (result != null) "Saved to your gallery." else "Could not save this plate.") }
                onSaved(result)
            }
        }
    }

    fun galleryFiles(): List<File> = social.galleryImages()

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }
}
