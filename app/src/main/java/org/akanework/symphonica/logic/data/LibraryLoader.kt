/*
 *     Copyright (C) 2023 AkaneWork Organization
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.akanework.symphonica.logic.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.akanework.symphonica.MainActivity
import org.akanework.symphonica.SymphonicaApplication
import org.akanework.symphonica.logic.util.getAllAlbums
import org.akanework.symphonica.logic.util.getAllSongs
import org.akanework.symphonica.ui.fragment.LibraryGridFragment
import org.akanework.symphonica.ui.fragment.LibraryListFragment

suspend fun loadDataFromDisk() {
    LibraryListFragment.switchPrompt(0)
    if (MainActivity.libraryViewModel.librarySongList.isEmpty()) {
        withContext(Dispatchers.IO) {
            if (MainActivity.songList.isEmpty()) {
                MainActivity.songList = getAllSongs(SymphonicaApplication.context)
                MainActivity.libraryViewModel.librarySongList = MainActivity.songList
            }
        }
        withContext(Dispatchers.IO) {
            if (MainActivity.albumList.isEmpty()) {
                MainActivity.albumList = getAllAlbums(MainActivity.songList)
                MainActivity.libraryViewModel.libraryAlbumList = MainActivity.albumList
            }
        }
    } else {
        MainActivity.albumList = MainActivity.libraryViewModel.libraryAlbumList
        MainActivity.songList = MainActivity.libraryViewModel.librarySongList
    }

    withContext(Dispatchers.Main) {
        reloadRecyclerView()
    }
}

fun reloadRecyclerView() {
    LibraryListFragment.switchPrompt(1)
    LibraryListFragment.updateRecyclerView(MainActivity.songList)
    LibraryGridFragment.updateRecyclerView(MainActivity.albumList)
}