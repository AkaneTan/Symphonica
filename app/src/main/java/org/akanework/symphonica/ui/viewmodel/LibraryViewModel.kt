package org.akanework.symphonica.ui.viewmodel

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.akanework.symphonica.logic.data.Album
import org.akanework.symphonica.logic.data.Song
import org.akanework.symphonica.logic.data.StorageSong
import org.akanework.symphonica.logic.util.convertToSong
import org.akanework.symphonica.logic.util.convertToStorageSong
import org.akanework.symphonica.logic.util.loadDrawableFromFile
import org.akanework.symphonica.logic.util.saveDrawableToFile

class LibraryViewModel: ViewModel() {
    var librarySongList: List<Song> = listOf()

    var libraryAlbumList: List<Album> = listOf()

}