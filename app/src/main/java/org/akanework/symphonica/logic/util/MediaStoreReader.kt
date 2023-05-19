package org.akanework.symphonica.logic.util

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import org.akanework.symphonica.R
import java.lang.Exception

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val path: String,
    val cover: Drawable
)

data class Album(
    val title: String,
    val artist: String,
    val cover: Drawable,
    val songList: List<Song>
)

fun getAllAlbums(context: Context, externalSongList: List<Song>): List<Album> {
    val albumsMap = mutableMapOf<String, MutableList<Song>>()

    for (song in externalSongList) {
        val albumKey = "${song.album}_${song.artist}"
        val albumSongs = albumsMap.getOrPut(albumKey) { mutableListOf() }
        albumSongs.add(song)
    }

    val albums = mutableListOf<Album>()

    for ((albumKey, songList) in albumsMap) {
        val (albumTitle, artist) = albumKey.split("_")
        val cover = songList[0].cover
        val album = Album(albumTitle, artist, cover, songList)
        albums.add(album)
    }

    return albums
}

fun getAllSongs(context: Context): List<Song> {
    val selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0"
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.DATA
    )
    val sortOrder = MediaStore.Audio.Media.TITLE + " ASC"

    val songs = mutableListOf<Song>()
    val cursor = context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        null,
        sortOrder
    )

    cursor?.use {
        val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val pathColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

        while (it.moveToNext()) {
            val id = it.getLong(idColumn)
            val title = it.getString(titleColumn)
            val artist = it.getString(artistColumn)
            val album = it.getString(albumColumn)
            val duration = it.getLong(durationColumn)
            val path = it.getString(pathColumn)

            val cover = getSongCover(context, path.toUri())
            val song = Song(id, title, artist, album, duration, path, cover)
            songs.add(song)
        }
    }

    return songs
}

fun getSongCover(context: Context, mUri: Uri): Drawable {
    val mmr = android.media.MediaMetadataRetriever()
    try {
        mmr.setDataSource(context, mUri)
    } catch (e: Exception) {
        if (e is IllegalArgumentException) {
            return AppCompatResources.getDrawable(context, R.drawable.ic_album_default_cover)!!
        }
    }
    val mAlbumThumbNailCoded = mmr.embeddedPicture
    if (mAlbumThumbNailCoded != null) {
        val mAlbumThumbNailBitmap = BitmapFactory.decodeByteArray(mAlbumThumbNailCoded, 0, mAlbumThumbNailCoded.size)
        mmr.release()
        return BitmapDrawable(mAlbumThumbNailBitmap)
    }
    return AppCompatResources.getDrawable(context, R.drawable.ic_album_default_cover)!!
}