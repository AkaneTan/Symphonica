package org.akanework.symphonica.logic.data

import android.graphics.drawable.Drawable

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val path: String,
    val cover: Drawable?
)

data class Album(
    val title: String,
    val artist: String,
    val cover: Drawable?,
    val songList: List<Song>
)

data class StorageSong(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val path: String
)

data class StorageAlbum(
    val title: String,
    val artist: String,
    val songList: List<Song>
)