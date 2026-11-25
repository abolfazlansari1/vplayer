package com.vplayer.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis

private data class FolderInfo(val path: String, val name: String, val items: List<VideoItem>)

@Composable
fun LibraryScreen(
    videos: List<VideoItem>,
    loading: Boolean,
    prefs: Prefs,
    resumeTick: Int,
    onPlay: (List<VideoItem>, Int) -> Unit,
    onOpenFile: () -> Unit,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var folderPath by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }

    val folders = remember(videos) {
        videos.groupBy { it.folder }
            .map { (path, list) ->
                FolderInfo(path, list.first().folderName, list.sortedWith(Comparator { a, b -> naturalCompare(a.title, b.title) }))
            }
            .sortedBy { it.name.lowercase() }
    }
    val selectedFolder = folders.firstOrNull { it.path == folderPath }

    BackHandler(enabled = searching || selectedFolder != null) {
        if (searching) {
            searching = false
            query = ""
        } else {
            folderPath = null
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
    ) {
        // header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (searching) {
                IconButton(onClick = {
                    searching = false
                    query = ""
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت", tint = TextPrimary)
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = TextPrimary, fontSize = 17.sp),
                    cursorBrush = SolidColor(Accent),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Card)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text("جستجو در ویدیوها", color = TextSecondary, fontSize = 17.sp)
                        }
                        inner()
                    },
                )
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "پاک کردن", tint = TextSecondary)
                    }
                }
            } else {
                if (selectedFolder != null) {
                    IconButton(onClick = { folderPath = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت", tint = TextPrimary)
                    }
                }
                Text(
                    text = selectedFolder?.name ?: "ویدیوها",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                )
                IconButton(onClick = { searching = true }) {
                    Icon(Icons.Filled.Search, contentDescription = "جستجو", tint = TextPrimary)
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = "بازخوانی", tint = TextPrimary)
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "تنظیمات", tint = TextPrimary)
                }
            }
        }

        // tabs
        if (!searching && selectedFolder == null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TabChip("پوشه‌ها", tab == 0) { tab = 0 }
                TabChip("همه ویدیوها", tab == 1) { tab = 1 }
                Spacer(Modifier.weight(1f))
                TabChip("باز کردن فایل", false) { onOpenFile() }
            }
        }
        Spacer(Modifier.height(6.dp))

        Box(Modifier.weight(1f)) {
            when {
                loading && videos.isEmpty() -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center), color = Accent)
                }
                videos.isEmpty() -> {
                    Column(
                        Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("ویدیویی پیدا نشد", color = TextSecondary, fontSize = 16.sp)
                        Button(onClick = onOpenFile) { Text("باز کردن فایل") }
                    }
                }
                searching && query.isNotBlank() -> {
                    val found = videos.filter { it.title.contains(query.trim(), ignoreCase = true) }
                    VideoList(found, prefs, resumeTick) { i -> onPlay(found, i) }
                }
                selectedFolder != null -> {
                    VideoList(selectedFolder.items, prefs, resumeTick) { i -> onPlay(selectedFolder.items, i) }
                }
                tab == 0 -> {
                    FolderList(folders) { folderPath = it.path }
                }
                else -> {
                    VideoList(videos, prefs, resumeTick) { i -> onPlay(videos, i) }
                }
            }
        }
    }
}

@Composable
private fun TabChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Accent else Card)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text, color = if (selected) Color.White else TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun FolderList(folders: List<FolderInfo>, onClick: (FolderInfo) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
        items(folders, key = { it.path }) { f ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onClick(f) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Card),
                    contentAlignment = Alignment.Center,
                ) {
                    GlyphIcon(Glyph.FOLDER, tint = Accent, iconSize = 28.dp)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(f.name, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${f.items.size} ویدیو", color = TextSecondary, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun VideoList(
    list: List<VideoItem>,
    prefs: Prefs,
    resumeTick: Int,
    onClick: (Int) -> Unit,
) {
    val ctx = LocalContext.current
    val loader = remember {
        ImageLoader.Builder(ctx)
            .components { add(VideoFrameDecoder.Factory()) }
            .crossfade(true)
            .build()
    }
    LazyColumn(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
    ) {
        items(list.size, key = { list[it].id }) { i ->
            val v = list[i]
            val pos = remember(resumeTick, v.uri) { prefs.getPos(uriKey(v.uri.toString())) }
            val frac = if (v.durationMs > 0) (pos.toFloat() / v.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onClick(i) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .width(124.dp)
                        .height(72.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Card)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(ctx).data(v.uri).videoFrameMillis(3000).build(),
                        imageLoader = loader,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(fmtTime(v.durationMs), color = Color.White, fontSize = 11.sp)
                    }
                    if (frac > 0.01f) {
                        LinearProgressIndicator(
                            progress = { frac },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(3.dp),
                            color = Accent,
                            trackColor = Color.Black.copy(alpha = 0.5f),
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(v.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Text(fmtSize(v.sizeBytes), color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}
