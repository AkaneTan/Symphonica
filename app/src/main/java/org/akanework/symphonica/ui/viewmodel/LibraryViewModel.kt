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

package org.akanework.symphonica.ui.viewmodel

import androidx.lifecycle.ViewModel
import org.akanework.symphonica.MainActivity.Companion.historyDao
import org.akanework.symphonica.MainActivity.Companion.isDBSafe
import org.akanework.symphonica.logic.data.Album
import org.akanework.symphonica.logic.data.HistoryDataEntity
import org.akanework.symphonica.logic.data.Song

/**
 * [LibraryViewModel] is a [ViewModel] that contains
 * Library's song list which are read from the disk
 * when booting up. They won't be saved locally.
 */
class LibraryViewModel : ViewModel() {
    var librarySongList: List<Song> = listOf()
    var librarySortedAlbumList: List<Album> = listOf()
    var libraryAlbumList: List<Album> = listOf()
    var libraryNewestAddedList: MutableList<Song> = mutableListOf()
    var libraryHistoryList: MutableList<Long> = mutableListOf()

    /**
     * @param song
     */
    fun addSongToHistory(song: Song) {
        if (isDBSafe) {
            libraryHistoryList.add(song.id)
        }
    }

    suspend fun saveSongToLocal() {
        historyDao.clearHistoryItems()
        val copyList = ArrayList(libraryHistoryList)
        for (item in copyList) {
            historyDao.insertItem(HistoryDataEntity(value = item))
        }
    }
}
