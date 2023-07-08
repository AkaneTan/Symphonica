/*
 *     Copyright (C) 2023  Akane Foundation
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.akanework.symphonica.logic.data

import org.akanework.symphonica.MainActivity.Companion.isForceLoadingEnabled
import org.akanework.symphonica.MainActivity.Companion.libraryViewModel
import org.akanework.symphonica.SymphonicaApplication
import org.akanework.symphonica.logic.util.findTopTenSongsByAddDate
import org.akanework.symphonica.logic.util.getAllAlbums
import org.akanework.symphonica.logic.util.getAllSongs
import org.akanework.symphonica.logic.util.sortAlbumListByTrackNumber
import org.akanework.symphonica.ui.fragment.HomeFragment

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [loadDataFromDisk] is used when booting up.
 * It uses different threads to load data from disk.
 */
suspend fun loadDataFromDisk() {
    HomeFragment.switchPrompt(0)
    withContext(Dispatchers.IO) {
        if (libraryViewModel.librarySongList.isEmpty()) {
            libraryViewModel.librarySongList = getAllSongs(SymphonicaApplication.context)
        }
        if (libraryViewModel.libraryAlbumList.isEmpty()) {
            libraryViewModel.libraryAlbumList = getAllAlbums(libraryViewModel.librarySongList)
        }
    }

    if (isForceLoadingEnabled) {
        withContext(Dispatchers.IO) {
            libraryViewModel.librarySortedAlbumList =
                    sortAlbumListByTrackNumber(libraryViewModel.libraryAlbumList)
        }
    }

    withContext(Dispatchers.Main) {
        if (libraryViewModel.libraryNewestAddedList.isEmpty() && libraryViewModel.librarySongList.isNotEmpty()) {
            libraryViewModel.libraryNewestAddedList =
                    findTopTenSongsByAddDate(libraryViewModel.librarySongList)
        }
        HomeFragment.switchPrompt(1)
    }
}
