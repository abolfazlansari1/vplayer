@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.vplayer.app

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class Panel { NONE, SUBTITLE, SUB_SETTINGS, EDITOR, AUDIO, SPEED }

private val aspectLabels = listOf("متناسب", "پر کردن", "زوم")
private val aspectModes = listOf(
    AspectRatioFrameLayout.RESIZE_MODE_FIT,
    AspectRatioFrameLayout.RESIZE_MODE_FILL,
    AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
)

private fun trackName(f: Format, n: Int): String {
    val parts = ArrayList<String>()
    val label = f.label
    if (!label.isNullOrBlank()) parts.add(label)
    val lang = f.language
    if (!lang.isNullOrBlank() && lang != "und") {
        val d = Locale.forLanguageTag(lang).displayLanguage
        parts.add(if (d.isNullOrBlank()) lang else d)
    }
    if (parts.isEmpty()) parts.add("#$n")
    return parts.distinct().joinToString(" • ")
}

private fun audioDetail(f: Format): String {
    val codec = f.sampleMimeType?.substringAfter('/')?.uppercase(Locale.US) ?: ""
    val ch = if (f.channelCount > 0) " ${f.channelCount}ch" else ""
    return (codec + ch).trim()
}

@Composable
fun PlayerScreen(activity: PlayerActivity, exo: ExoPlayer, items: List<PlayerItem>) {
    val prefs = remember { Prefs(activity) }
    val scope = rememberCoroutineScope()
    val audioManager = remember { activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    var index by remember { mutableIntStateOf(exo.currentMediaItemIndex) }
    var isPlaying by remember { mutableStateOf(exo.isPlaying) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var buffering by remember { mutableStateOf(false) }
    var controls by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }
    var showUnlock by remember { mutableStateOf(false) }
    var aspectIdx by remember { mutableIntStateOf(prefs.aspect.coerceIn(0, 2)) }
    var speed by remember { mutableFloatStateOf(1f) }
    var tracks by remember { mutableStateOf(Tracks.EMPTY) }
    var embedded by remember { mutableStateOf<List<Cue>>(emptyList()) }
    var panel by remember { mutableStateOf(Panel.NONE) }
    var hud by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableIntStateOf(0) }
    var seeking by remember { mutableStateOf<Float?>(null) }
    var portrait by remember { mutableStateOf(false) }

    var extName by remember { mutableStateOf<String?>(null) }
    val editCues = remember { mutableStateListOf<SubCue>() }
    var editVersion by remember { mutableIntStateOf(0) }
    var subDelay by remember { mutableLongStateOf(0L) }
    var subSize by remember { mutableFloatStateOf(prefs.subSize) }
    var subColor by remember { mutableIntStateOf(prefs.subColor) }
    var subBg by remember { mutableFloatStateOf(prefs.subBg) }
    var subBottom by remember { mutableFloatStateOf(prefs.subBottom) }
    var subBold by remember { mutableStateOf(prefs.subBold) }
    var siblings by remember { mutableStateOf<List<File>>(emptyList()) }

    fun toast(s: String) {
        Toast.makeText(activity, s, Toast.LENGTH_SHORT).show()
    }

    fun setTextDisabled(disabled: Boolean) {
        exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, disabled)
            .build()
    }

    fun resetTextSelection() {
        exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .build()
    }

    fun clearExternal() {
        extName = null
        editCues.clear()
        subDelay = 0L
    }

    fun loadExternal(name: String, cues: List<SubCue>) {
        editCues.clear()
        editCues.addAll(cues)
        editVersion += 1
        extName = name
        subDelay = 0L
        setTextDisabled(true)
    }

    fun loadBytes(name: String, bytes: ByteArray) {
        val cues = SubtitleIO.parse(SubtitleIO.decode(bytes))
        if (cues.isEmpty()) {
            toast("فایل زیرنویس معتبر نیست")
        } else {
            loadExternal(name, cues)
            toast("زیرنویس بارگذاری شد: " + cues.size)
        }
    }

    fun selectTrack(g: Tracks.Group, i: Int) {
        val b = exo.trackSelectionParameters.buildUpon()
        if (g.type == C.TRACK_TYPE_TEXT) {
            clearExternal()
            b.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
        }
        b.setOverrideForType(TrackSelectionOverride(g.mediaTrackGroup, i))
        exo.trackSelectionParameters = b.build()
    }

    fun seekBy(delta: Long) {
        val limit = if (duration > 0) duration else Long.MAX_VALUE
        val target = (exo.currentPosition + delta).coerceIn(0L, limit)
        exo.seekTo(target)
        hud = (if (delta > 0) "+" else "-") + (abs(delta) / 1000) + "s  " + fmtTime(target)
    }

    fun currentBrightness(): Float {
        val b = activity.window.attributes.screenBrightness
        if (b >= 0f) return b
        return try {
            Settings.System.getInt(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
        } catch (e: Exception) {
            0.5f
        }
    }

    fun setBrightness(v: Float) {
        val lp = activity.window.attributes
        lp.screenBrightness = v
        activity.window.attributes = lp
    }

    val subPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val name = displayName(activity, uri)
                val bytes = withContext(Dispatchers.IO) {
                    try {
                        activity.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    } catch (e: Exception) {
                        null
                    }
                }
                if (bytes == null) toast("خواندن فایل ممکن نشد") else loadBytes(name, bytes)
            }
        }
    }

    val exporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/x-subrip")
    ) { uri: Uri? ->
        if (uri != null) {
            val snapshot = editCues.toList()
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    try {
                        activity.contentResolver.openOutputStream(uri)?.use { it.write(SubtitleIO.toBytes(snapshot)) }
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
                toast(if (ok) "ذخیره شد" else "ذخیره نشد")
            }
        }
    }

    fun saveBeside() {
        val p = items.getOrNull(index)?.path
        if (p.isNullOrEmpty()) {
            toast("مسیر ویدیو مشخص نیست؛ از «ذخیره به عنوان» استفاده کنید")
            return
        }
        val f = File(p)
        val out = File(f.parentFile, f.nameWithoutExtension + ".edited.srt")
        val snapshot = editCues.toList()
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    out.writeBytes(SubtitleIO.toBytes(snapshot))
                    true
                } catch (e: Exception) {
                    false
                }
            }
            toast(if (ok) "ذخیره شد: " + out.name else "اجازه نوشتن در این پوشه نیست؛ از «ذخیره به عنوان» استفاده کنید")
        }
    }

    DisposableEffect(exo) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                isPlaying = isPlayingNow
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_ENDED && !exo.hasNextMediaItem()) {
                    activity.clearPosition()
                    activity.finish()
                }
            }

            override fun onTracksChanged(t: Tracks) {
                tracks = t
            }

            override fun onCues(cueGroup: CueGroup) {
                embedded = cueGroup.cues
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                index = exo.currentMediaItemIndex
                embedded = emptyList()
                error = null
                clearExternal()
                resetTextSelection()
            }

            override fun onPlayerError(e: PlaybackException) {
                error = "خطا در پخش (" + e.errorCodeName + "). ممکن است قالب یا کدک این فایل روی دستگاه پشتیبانی نشود."
            }
        }
        exo.addListener(listener)
        tracks = exo.currentTracks
        onDispose { exo.removeListener(listener) }
    }

    LaunchedEffect(exo) {
        var counter = 0
        while (true) {
            position = exo.currentPosition
            duration = exo.duration.coerceAtLeast(0L)
            counter += 1
            if (counter % 40 == 0) activity.savePosition()
            delay(150)
        }
    }

    LaunchedEffect(controls, isPlaying, tick, panel) {
        if (controls && isPlaying && panel == Panel.NONE) {
            delay(3500)
            controls = false
        }
    }
    LaunchedEffect(showUnlock) {
        if (showUnlock) {
            delay(2500)
            showUnlock = false
        }
    }
    LaunchedEffect(hud) {
        if (hud != null) {
            delay(900)
            hud = null
        }
    }
    LaunchedEffect(index) {
        val path = items.getOrNull(index)?.path
        val found = withContext(Dispatchers.IO) { SubtitleIO.findSiblings(path) }
        siblings = found
        if (found.isNotEmpty() && extName == null && prefs.autoSub) {
            val f = found.first()
            val bytes = withContext(Dispatchers.IO) {
                try {
                    f.readBytes()
                } catch (e: Exception) {
                    null
                }
            }
            if (bytes != null) loadBytes(f.name, bytes)
        }
    }

    BackHandler(enabled = panel != Panel.NONE) { panel = Panel.NONE }
    BackHandler(enabled = locked) { showUnlock = true }

    // ---- derived values ----
    val subText: String = if (extName != null) {
        val p = position - subDelay
        editCues.filter { p >= it.startMs && p <= it.endMs }.joinToString("\n") { it.text }
    } else {
        embedded.mapNotNull { it.text?.toString() }.joinToString("\n")
    }

    val textOptions = ArrayList<TrackOption>()
    val audioOptions = ArrayList<TrackOption>()
    var textCount = 0
    var audioCount = 0
    for (g in tracks.groups) {
        if (g.type == C.TRACK_TYPE_TEXT) {
            for (i in 0 until g.length) {
                textCount += 1
                val f = g.getTrackFormat(i)
                textOptions.add(
                    TrackOption(trackName(f, textCount), extName == null && g.isTrackSelected(i), g.isTrackSupported(i)) {
                        selectTrack(g, i)
                    }
                )
            }
        } else if (g.type == C.TRACK_TYPE_AUDIO) {
            for (i in 0 until g.length) {
                audioCount += 1
                val f = g.getTrackFormat(i)
                val detail = audioDetail(f)
                val name = trackName(f, audioCount) + if (detail.isNotEmpty()) "  ($detail)" else ""
                audioOptions.add(
                    TrackOption(name, g.isTrackSelected(i), g.isTrackSupported(i)) {
                        selectTrack(g, i)
                    }
                )
            }
        }
    }
    val textOff = extName == null && textOptions.none { it.selected }
    val currentTitle = items.getOrNull(index)?.title ?: ""
    val hasNext = index < items.size - 1
    val hasPrev = index > 0

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { c ->
                    PlayerView(c).apply {
                        useController = false
                        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                        subtitleView?.visibility = View.GONE
                        this.player = exo
                    }
                },
                update = { v -> v.resizeMode = aspectModes[aspectIdx] },
                modifier = Modifier.fillMaxSize(),
            )

            SubtitleOverlay(
                text = subText,
                size = subSize,
                color = subColor,
                bgAlpha = subBg,
                bold = subBold,
                bottom = (subBottom + if (controls && !locked) 76f else 0f).dp,
            )

            // gesture layer
            var gestureMode = 0
            var gestureSide = 0
            var gestureStartPos = 0L
            var gestureTotalX = 0f
            var gestureTotalY = 0f
            var gestureStartBrightness = 0.5f
            var gestureStartVolume = 0
            var gestureTarget = 0L
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(locked) {
                        detectTapGestures(
                            onTap = {
                                if (locked) showUnlock = !showUnlock else controls = !controls
                            },
                            onDoubleTap = { offset ->
                                if (!locked) {
                                    if (offset.x < size.width / 2f) seekBy(-10000L) else seekBy(10000L)
                                }
                            },
                        )
                    }
                    .pointerInput(locked) {
                        if (locked) return@pointerInput
                        detectDragGestures(
                            onDragStart = { offset ->
                                gestureMode = 0
                                gestureTotalX = 0f
                                gestureTotalY = 0f
                                gestureStartPos = exo.currentPosition
                                gestureSide = if (offset.x < size.width / 2f) 2 else 3
                                gestureStartBrightness = currentBrightness()
                                gestureStartVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                gestureTarget = gestureStartPos
                            },
                            onDrag = { change, drag ->
                                change.consume()
                                gestureTotalX += drag.x
                                gestureTotalY += drag.y
                                if (gestureMode == 0 && (abs(gestureTotalX) > 28f || abs(gestureTotalY) > 28f)) {
                                    gestureMode = if (abs(gestureTotalX) > abs(gestureTotalY)) 1 else gestureSide
                                }
                                when (gestureMode) {
                                    1 -> {
                                        val limit = if (duration > 0) duration else Long.MAX_VALUE
                                        val delta = (gestureTotalX / size.width * 120000f).toLong()
                                        gestureTarget = (gestureStartPos + delta).coerceIn(0L, limit)
                                        val sign = if (delta >= 0) "+" else "-"
                                        hud = sign + (abs(delta) / 1000) + "s  " + fmtTime(gestureTarget)
                                    }
                                    2 -> {
                                        val nb = (gestureStartBrightness - gestureTotalY / size.height * 1.3f).coerceIn(0.02f, 1f)
                                        setBrightness(nb)
                                        hud = "روشنایی " + (nb * 100).roundToInt() + "%"
                                    }
                                    3 -> {
                                        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                        val nv = (gestureStartVolume - gestureTotalY / size.height * max * 1.3f)
                                            .roundToInt().coerceIn(0, max)
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, nv, 0)
                                        hud = "صدا " + (nv * 100 / max.coerceAtLeast(1)) + "%"
                                    }
                                }
                            },
                            onDragEnd = {
                                if (gestureMode == 1) exo.seekTo(gestureTarget)
                                gestureMode = 0
                            },
                            onDragCancel = { gestureMode = 0 },
                        )
                    }
            )

            // controls
            if (controls && !locked) {
                TopBar(
                    title = currentTitle,
                    speedLabel = speedText(speed),
                    onBack = { activity.finish() },
                    onSubtitle = { panel = Panel.SUBTITLE },
                    onAudio = { panel = Panel.AUDIO },
                    onSpeed = { panel = Panel.SPEED },
                    modifier = Modifier.align(Alignment.TopCenter),
                )
                CenterControls(
                    isPlaying = isPlaying,
                    hasPrev = hasPrev,
                    hasNext = hasNext,
                    onPrev = { exo.seekToPreviousMediaItem() },
                    onBack10 = { seekBy(-10000L); tick += 1 },
                    onPlayPause = {
                        if (exo.isPlaying) exo.pause() else exo.play()
                        tick += 1
                    },
                    onFwd10 = { seekBy(10000L); tick += 1 },
                    onNext = { exo.seekToNextMediaItem() },
                    modifier = Modifier.align(Alignment.Center),
                )
                BottomBar(
                    position = position,
                    duration = duration,
                    seeking = seeking,
                    aspectLabel = aspectLabels[aspectIdx],
                    onSeekChange = {
                        seeking = it
                        tick += 1
                    },
                    onSeekDone = {
                        seeking?.let { exo.seekTo(it.toLong()) }
                        seeking = null
                    },
                    onLock = {
                        locked = true
                        controls = false
                        showUnlock = true
                    },
                    onAspect = {
                        aspectIdx = (aspectIdx + 1) % aspectModes.size
                        prefs.aspect = aspectIdx
                        hud = aspectLabels[aspectIdx]
                    },
                    onRotate = {
                        portrait = !portrait
                        activity.setPortrait(portrait)
                    },
                    onEdit = { panel = Panel.EDITOR },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            if (buffering) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(44.dp),
                    color = Accent,
                    strokeWidth = 3.dp,
                )
            }

            val h = hud
            if (h != null) {
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(h, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }

            if (locked && showUnlock) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable {
                            locked = false
                            showUnlock = false
                            controls = true
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = "باز کردن قفل", tint = Color.White)
                }
            }

            val err = error
            if (err != null) {
                Column(
                    Modifier
                        .align(Alignment.Center)
                        .padding(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(PanelBg)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(err, color = TextPrimary, fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PillButton("تلاش مجدد") {
                            error = null
                            exo.prepare()
                        }
                        if (hasNext) {
                            PillButton("بعدی") {
                                error = null
                                exo.seekToNextMediaItem()
                                exo.prepare()
                            }
                        }
                        PillButton("خروج") { activity.finish() }
                    }
                }
            }

            // panels
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                when (panel) {
                    Panel.NONE -> {}
                    Panel.SUBTITLE -> SubtitlePanel(
                        textOff = textOff,
                        embeddedOptions = textOptions,
                        siblings = siblings,
                        extName = extName,
                        onOff = {
                            clearExternal()
                            setTextDisabled(true)
                            panel = Panel.NONE
                        },
                        onPickSibling = { f ->
                            scope.launch {
                                val bytes = withContext(Dispatchers.IO) {
                                    try {
                                        f.readBytes()
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                if (bytes == null) toast("خواندن فایل ممکن نشد") else loadBytes(f.name, bytes)
                            }
                        },
                        onPickFile = { subPicker.launch(arrayOf("*/*")) },
                        onEdit = { panel = Panel.EDITOR },
                        onSettings = { panel = Panel.SUB_SETTINGS },
                        onClose = { panel = Panel.NONE },
                    )
                    Panel.SUB_SETTINGS -> SubSettingsPanel(
                        hasExternal = extName != null,
                        delay = subDelay,
                        onDelay = { subDelay = it },
                        textSize = subSize,
                        onSize = {
                            subSize = it
                            prefs.subSize = it
                        },
                        bold = subBold,
                        onBold = {
                            subBold = it
                            prefs.subBold = it
                        },
                        color = subColor,
                        onColor = {
                            subColor = it
                            prefs.subColor = it
                        },
                        bg = subBg,
                        onBg = {
                            subBg = it
                            prefs.subBg = it
                        },
                        bottom = subBottom,
                        onBottom = {
                            subBottom = it
                            prefs.subBottom = it
                        },
                        onClose = { panel = Panel.NONE },
                    )
                    Panel.EDITOR -> EditorPanel(
                        hasExternal = extName != null,
                        name = extName ?: "",
                        cues = editCues,
                        version = editVersion,
                        onShiftAll = { d ->
                            for (k in editCues.indices) {
                                val c = editCues[k]
                                editCues[k] = c.copy(
                                    startMs = (c.startMs + d).coerceAtLeast(0L),
                                    endMs = (c.endMs + d).coerceAtLeast(0L),
                                )
                            }
                            editVersion += 1
                        },
                        onSeek = { ms -> exo.seekTo((ms + subDelay).coerceAtLeast(0L)) },
                        onPickFile = { subPicker.launch(arrayOf("*/*")) },
                        onExport = {
                            val base = (items.getOrNull(index)?.title ?: "subtitle")
                            exporter.launch("$base.srt")
                        },
                        onSaveBeside = { saveBeside() },
                        onClose = { panel = Panel.NONE },
                    )
                    Panel.AUDIO -> OptionsPanel(
                        title = "مسیر صدا",
                        options = audioOptions,
                        emptyText = "مسیر صدایی پیدا نشد",
                        onClose = { panel = Panel.NONE },
                    )
                    Panel.SPEED -> SpeedPanel(
                        speed = speed,
                        onSpeed = {
                            speed = it
                            exo.setPlaybackSpeed(it)
                        },
                        onClose = { panel = Panel.NONE },
                    )
                }
            }
        }
    }
}

private fun speedText(s: Float): String {
    val t = String.format(Locale.US, "%.2f", s).trimEnd('0').trimEnd('.')
    return t + "x"
}

@Composable
fun PillButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TopBar(
    title: String,
    speedLabel: String,
    onBack: () -> Unit,
    onSubtitle: () -> Unit,
    onAudio: () -> Unit,
    onSpeed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت", tint = Color.White)
        }
        Text(
            title,
            color = Color.White,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            style = TextStyle(textDirection = TextDirection.Content),
        )
        PillButton("زیرنویس", onSubtitle)
        Spacer(Modifier.width(8.dp))
        PillButton("صدا", onAudio)
        Spacer(Modifier.width(8.dp))
        PillButton(speedLabel, onSpeed)
        Spacer(Modifier.width(8.dp))
    }
}

@Composable
private fun CenterControls(
    isPlaying: Boolean,
    hasPrev: Boolean,
    hasNext: Boolean,
    onPrev: () -> Unit,
    onBack10: () -> Unit,
    onPlayPause: () -> Unit,
    onFwd10: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        CircleButton(52.dp, enabled = hasPrev, onClick = onPrev) {
            GlyphIcon(Glyph.PREV, tint = if (hasPrev) Color.White else Color.White.copy(alpha = 0.3f), iconSize = 24.dp)
        }
        CircleButton(52.dp, onClick = onBack10) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GlyphIcon(Glyph.REWIND, iconSize = 20.dp)
                Text("10", color = Color.White, fontSize = 10.sp)
            }
        }
        Box(
            Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Accent)
                .clickable { onPlayPause() },
            contentAlignment = Alignment.Center,
        ) {
            GlyphIcon(if (isPlaying) Glyph.PAUSE else Glyph.PLAY, iconSize = 38.dp)
        }
        CircleButton(52.dp, onClick = onFwd10) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GlyphIcon(Glyph.FORWARD, iconSize = 20.dp)
                Text("10", color = Color.White, fontSize = 10.sp)
            }
        }
        CircleButton(52.dp, enabled = hasNext, onClick = onNext) {
            GlyphIcon(Glyph.NEXT, tint = if (hasNext) Color.White else Color.White.copy(alpha = 0.3f), iconSize = 24.dp)
        }
    }
}

@Composable
private fun CircleButton(
    size: Dp,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun BottomBar(
    position: Long,
    duration: Long,
    seeking: Float?,
    aspectLabel: String,
    onSeekChange: (Float) -> Unit,
    onSeekDone: () -> Unit,
    onLock: () -> Unit,
    onAspect: () -> Unit,
    onRotate: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val max = duration.coerceAtLeast(1L).toFloat()
    val value = (seeking ?: position.toFloat()).coerceIn(0f, max)
    Column(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(fmtTime(value.toLong()), color = Color.White, fontSize = 12.sp)
            Slider(
                value = value,
                onValueChange = onSeekChange,
                onValueChangeFinished = onSeekDone,
                valueRange = 0f..max,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Accent,
                    activeTrackColor = Accent,
                    inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                ),
            )
            Text(fmtTime(duration), color = Color.White, fontSize = 12.sp)
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PillButton("قفل", onLock)
            PillButton(aspectLabel, onAspect)
            PillButton("چرخش", onRotate)
            PillButton("ویرایش زیرنویس", onEdit)
        }
    }
}

@Composable
private fun SubtitleOverlay(
    text: String,
    size: Float,
    color: Int,
    bgAlpha: Float,
    bold: Boolean,
    bottom: Dp,
) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(start = 32.dp, end = 32.dp, bottom = bottom),
        contentAlignment = Alignment.BottomCenter,
    ) {
        if (text.isNotBlank()) {
            Text(
                text = text,
                color = Color(color),
                fontSize = size.sp,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    textDirection = TextDirection.Content,
                    shadow = Shadow(Color.Black, Offset(2f, 2f), 4f),
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = bgAlpha))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}
