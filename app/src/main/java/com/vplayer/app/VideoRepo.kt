package com.vplayer.app

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import java.io.File

object VideoRepo {

    @Suppress("DEPRECATION")
    fun load(context: Context): List<VideoItem> {
        val list = ArrayList<VideoItem>()
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        )
        try {
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                MediaStore.Video.Media.DATE_ADDED + " DESC",
            )?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dataCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val durCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val bucketCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val name = c.getString(nameCol) ?: "video"
                    val path = c.getString(dataCol)
                    val parent = if (path.isNullOrEmpty()) null else File(path).parent
                    val bucket = c.getString(bucketCol) ?: "Videos"
                    list.add(
                        VideoItem(
                            id = id,
                            uri = ContentUris.withAppendedId(collection, id),
                            title = name.substringBeforeLast('.'),
                            path = if (path.isNullOrEmpty()) null else path,
                            folder = parent ?: bucket,
                            folderName = bucket,
                            durationMs = c.getLong(durCol),
                            sizeBytes = c.getLong(sizeCol),
                            dateAdded = c.getLong(dateCol),
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // permission revoked or provider error: return what we have
        }
        return list
    }
}
