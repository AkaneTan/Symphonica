package org.akanework.symphonica.logic.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.akanework.symphonica.MainActivity
import org.akanework.symphonica.SymphonicaApplication
import org.akanework.symphonica.logic.util.getAllAlbums
import org.akanework.symphonica.logic.util.getAllSongs
import org.akanework.symphonica.logic.util.loadLibrarySongList
import org.akanework.symphonica.logic.util.saveLibrarySongList
import org.akanework.symphonica.ui.fragment.LibraryGridFragment
import org.akanework.symphonica.ui.fragment.LibraryListFragment

suspend fun loadDataFromCache() {
    if (MainActivity.libraryViewModel.librarySongList.isEmpty()) {
        withContext(Dispatchers.IO) {
            MainActivity.songList = loadLibrarySongList(MainActivity.sharedPreferences)
            MainActivity.libraryViewModel.librarySongList = MainActivity.songList
        }
        withContext(Dispatchers.Main) {
            LibraryListFragment.updateRecyclerView(MainActivity.songList)
            MainActivity.albumList = getAllAlbums(SymphonicaApplication.context, MainActivity.songList)
            MainActivity.libraryViewModel.libraryAlbumList = MainActivity.albumList
        }
    } else {
        MainActivity.songList = MainActivity.libraryViewModel.librarySongList
        MainActivity.albumList = MainActivity.libraryViewModel.libraryAlbumList
    }
    withContext(Dispatchers.Main) {
        if (!MainActivity.songList.isEmpty()) {
            LibraryListFragment.dismissPrompt()
            LibraryListFragment.updateRecyclerView(MainActivity.songList)
            LibraryGridFragment.dismissPrompt()
            LibraryGridFragment.updateRecyclerView(MainActivity.albumList)
        }
    }
}

suspend fun loadDataFromDisk() {
    if (MainActivity.libraryViewModel.librarySongList.isEmpty()) {
        withContext(Dispatchers.IO) {
            if (MainActivity.songList.isEmpty()) {
                MainActivity.songList = getAllSongs(SymphonicaApplication.context)
                MainActivity.libraryViewModel.librarySongList = MainActivity.songList
            }
        }
        withContext(Dispatchers.Main) {
            if (MainActivity.albumList.isEmpty()) {
                MainActivity.albumList = getAllAlbums(SymphonicaApplication.context, MainActivity.songList)
                MainActivity.libraryViewModel.libraryAlbumList = MainActivity.albumList
            }
        }
    } else {
        MainActivity.albumList = MainActivity.libraryViewModel.libraryAlbumList
        MainActivity.songList = MainActivity.libraryViewModel.librarySongList
    }

    withContext(Dispatchers.Main) {
        LibraryListFragment.dismissPrompt()
        LibraryListFragment.updateRecyclerView(MainActivity.songList)
        LibraryGridFragment.dismissPrompt()
        LibraryGridFragment.updateRecyclerView(MainActivity.albumList)
    }

    withContext(Dispatchers.IO) {
        saveLibrarySongList(MainActivity.songList, MainActivity.sharedPreferences)
    }
}