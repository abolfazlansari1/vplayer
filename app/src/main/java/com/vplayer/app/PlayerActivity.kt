@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.vplayer.app

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer

class PlayerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_INDEX = "index"
    }

    private lateinit var exo: ExoPlayer
    private lateinit var prefs: Prefs
    private var items: List<PlayerItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)

        var list: List<PlayerItem> = Playlist.items
        var startIndex = intent.getIntExtra(EXTRA_INDEX, Playlist.startIndex)
        val data = intent.data
        if (intent.action == Intent.ACTION_VIEW && data != null) {
            val path = if (data.scheme == "file") data.path else null
            list = listOf(PlayerItem(data, displayName(this, data), path))
            startIndex = 0
        }
        if (list.isEmpty()) {
            finish()
            return
        }
        items = list
        startIndex = startIndex.coerceIn(0, list.size - 1)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= 28) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        hideSystemBars()

        exo = ExoPlayer.Builder(this)
            .setRenderersFactory(DefaultRenderersFactory(this).setEnableDecoderFallback(true))
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build()
        exo.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            true,
        )
        val mediaItems = list.map {
            MediaItem.Builder().setUri(it.uri).setMediaId(it.uri.toString()).build()
        }
        val saved = prefs.getPos(uriKey(list[startIndex].uri.toString()))
        exo.setMediaItems(mediaItems, startIndex, if (saved > 3000L) saved else 0L)
        exo.prepare()
        exo.playWhenReady = true

        val playerItems = list
        setContent {
            VPlayerTheme {
                PlayerScreen(this, exo, playerItems)
            }
        }
    }

    private fun hideSystemBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    fun savePosition() {
        if (!::exo.isInitialized) return
        val item = items.getOrNull(exo.currentMediaItemIndex) ?: return
        val dur = exo.duration
        val pos = exo.currentPosition
        val key = uriKey(item.uri.toString())
        if (dur > 0 && pos > dur - 5000) prefs.setPos(key, 0L) else prefs.setPos(key, pos)
    }

    fun clearPosition() {
        val item = items.getOrNull(exo.currentMediaItemIndex) ?: return
        prefs.setPos(uriKey(item.uri.toString()), 0L)
    }

    fun setPortrait(portrait: Boolean) {
        requestedOrientation = if (portrait) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    override fun onPause() {
        super.onPause()
        if (::exo.isInitialized) {
            exo.pause()
            savePosition()
        }
    }

    override fun onDestroy() {
        if (::exo.isInitialized) {
            savePosition()
            exo.release()
        }
        super.onDestroy()
    }
}
