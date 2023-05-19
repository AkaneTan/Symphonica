package org.akanework.symphonica.ui.viewmodel

import androidx.lifecycle.ViewModel
import org.akanework.symphonica.logic.util.Album
import org.akanework.symphonica.logic.util.Song

class LibraryViewModel: ViewModel() {
    var librarySongList: List<Song> = listOf()
    var libraryAlbumList: List<Album> = listOf()
}
