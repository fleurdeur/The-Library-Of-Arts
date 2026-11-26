package org.libraryofarts.feature.home

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer

/**
 * Small looping player for reel soundtracks. The artwork itself is a still
 * image, so there is no video to sync: the track simply loops under the reel.
 */
class ReelAudioPlayer(@Suppress("UNUSED_PARAMETER") context: Context) {
    private var player: MediaPlayer? = null
    private var currentUrl: String? = null
    private var prepared = false
    private var muted = false
    private var paused = false

    fun play(url: String, muted: Boolean) {
        this.muted = muted
        this.paused = false
        if (url == currentUrl && player != null) {
            applyVolume()
            if (prepared) runCatching { player?.start() }
            return
        }
        releasePlayer()
        currentUrl = url
        prepared = false
        val media = MediaPlayer()
        player = media
        runCatching {
            media.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            media.isLooping = true
            media.setDataSource(url)
            applyVolume(media)
            media.setOnPreparedListener { mp ->
                prepared = true
                if (!paused) runCatching { mp.start() }
            }
            media.setOnErrorListener { _, _, _ ->
                prepared = false
                true
            }
            media.prepareAsync()
        }.onFailure { releasePlayer() }
    }

    fun pause() {
        paused = true
        if (prepared) runCatching { if (player?.isPlaying == true) player?.pause() }
    }

    fun setMuted(value: Boolean) {
        muted = value
        applyVolume()
    }

    fun release() {
        releasePlayer()
        currentUrl = null
    }

    private fun applyVolume(media: MediaPlayer? = player) {
        val level = if (muted) 0f else 0.72f
        runCatching { media?.setVolume(level, level) }
    }

    private fun releasePlayer() {
        prepared = false
        runCatching { player?.release() }
        player = null
    }
}
