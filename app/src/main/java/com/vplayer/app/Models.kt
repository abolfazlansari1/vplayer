package com.vplayer.app

import android.net.Uri

data class VideoItem(
    val id: Long,
    val uri: Uri,
    val title: String,
    val path: String?,
    val folder: String,
    val folderName: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAdded: Long,
) {
    fun toPlayerItem(): PlayerItem = PlayerItem(uri, title, path)
}

data class PlayerItem(
    val uri: Uri,
    val title: String,
    val path: String?,
)

private var cueCounter = 0L

fun nextCueId(): Long {
    cueCounter += 1
    return cueCounter
}

data class SubCue(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val id: Long = nextCueId(),
)

/** Playlist handed from the library to the player activity. */
object Playlist {
    @Volatile
    var items: List<PlayerItem> = emptyList()

    @Volatile
    var startIndex: Int = 0
}

data class TrackOption(
    val label: String,
    val selected: Boolean,
    val supported: Boolean,
    val onClick: () -> Unit,
)
