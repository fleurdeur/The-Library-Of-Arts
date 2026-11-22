package org.libraryofarts.core.media

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.libraryofarts.core.model.AudioTrack

@Singleton
class AudioPlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val player: ExoPlayer by lazy { ExoPlayer.Builder(context).build() }
    private val _current = MutableStateFlow<AudioTrack?>(null)
    val current: StateFlow<AudioTrack?> = _current.asStateFlow()
    private val _playing = MutableStateFlow(false)
    val playing: StateFlow<Boolean> = _playing.asStateFlow()

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playing.value = isPlaying
            }
        })
    }

    fun play(track: AudioTrack) {
        if (track.url.isBlank()) return
        _current.value = track
        player.setMediaItem(MediaItem.fromUri(track.url))
        player.prepare()
        player.play()
    }

    fun toggle() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun stop() {
        player.stop()
        _current.value = null
    }

    fun release() {
        player.release()
    }
}
