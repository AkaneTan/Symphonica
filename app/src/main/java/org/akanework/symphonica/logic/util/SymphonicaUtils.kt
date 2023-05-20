package org.akanework.symphonica.logic.util

import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.akanework.symphonica.MainActivity.Companion.cacheDirDrawable
import org.akanework.symphonica.logic.data.Album
import org.akanework.symphonica.logic.data.Song
import org.akanework.symphonica.logic.data.StorageAlbum
import org.akanework.symphonica.logic.data.StorageSong
import java.io.File
import java.io.FileOutputStream

fun convertDurationToTimeStamp(duration: String): String {
    val minutes = duration.toInt() / 1000 / 60
    val seconds = duration.toInt() / 1000 - minutes * 60
    if (seconds < 10) {
        return "$minutes:0$seconds"
    }
    return "$minutes:$seconds"
}

fun saveDrawableToFile(key: String, drawable: Drawable?) {
    if (drawable != null) {
        val file = File(cacheDirDrawable, key)
        val bitmap = (drawable as BitmapDrawable).bitmap

        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
        }
    }
}

fun loadDrawableFromFile(key: String): Drawable? {
    val file = File(cacheDirDrawable, key)
    if (file.exists()) {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        return BitmapDrawable(Resources.getSystem(), bitmap)
    }
    return null
}

fun convertToStorageSong(song: Song): StorageSong {
    return StorageSong(
        id = song.id,
        title = song.title,
        artist = song.artist,
        album = song.album,
        duration = song.duration,
        path = song.path
    )
}

fun convertToSong(storageSong: StorageSong, cover: Drawable?): Song {
    return Song(
        id = storageSong.id,
        title = storageSong.title,
        artist = storageSong.artist,
        album = storageSong.album,
        imgUri = null,
        duration = storageSong.duration,
        path = storageSong.path
    )
}

fun convertToStorageAlbum(album: Album): StorageAlbum {
    return StorageAlbum(
        title = album.title,
        artist = album.artist,
        songList = album.songList,
    )
}

fun convertToAlbum(storageAlbum: StorageAlbum, cover: Drawable?): Album {
    return Album(
        title = storageAlbum.title,
        artist = storageAlbum.artist,
        cover = cover,
        songList = storageAlbum.songList
    )
}

fun saveLibrarySongList(songList: List<Song>, sharedPreferences: SharedPreferences) {
    val transformedSongList = mutableListOf<StorageSong>()
    for (i in songList) {
        transformedSongList.add(convertToStorageSong(i))
        // saveDrawableToFile(i.id.toString(), i.cover)
    }
    val json = Gson().toJson(transformedSongList)
    sharedPreferences.edit().putString("song_list", json).apply()
}

fun loadLibrarySongList(sharedPreferences: SharedPreferences): List<Song> {
    val json = sharedPreferences.getString("song_list", null)
    return if (json != null) {
        val type = object : TypeToken<List<StorageSong>>() {}.type
        val tmp: List<StorageSong> = Gson().fromJson(json, type)
        val transformedSongList = mutableListOf<Song>()
        for (i in tmp) {
            transformedSongList.add(convertToSong(i, loadDrawableFromFile(i.id.toString())))
        }
        transformedSongList
    } else {
        listOf()
    }
}