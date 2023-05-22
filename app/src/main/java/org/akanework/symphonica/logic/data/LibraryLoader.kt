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
import org.akanework.symphonica.MainActivity.Companion.isForceLoadingEnabled
import org.akanework.symphonica.MainActivity.Companion.libraryViewModel
import org.akanework.symphonica.SymphonicaApplication
import org.akanework.symphonica.logic.util.getAllAlbums
import org.akanework.symphonica.logic.util.getAllSongs
import org.akanework.symphonica.logic.util.sortAlbumListByTrackNumber
import org.akanework.symphonica.ui.fragment.HomeFragment

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
        HomeFragment.switchPrompt(1)
    }
}