/*
 *     Copyright (C) 2023 Akane Foundation
 *
 *     This file is part of Symphonica.
 *
 *     Symphonica is free software: you can redistribute it and/or modify it under the terms
 *     of the GNU General Public License as published by the Free Software Foundation,
 *     either version 3 of the License, or (at your option) any later version.
 *
 *     Symphonica is distributed in the hope that it will be useful, but WITHOUT ANY
 *     WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 *     FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along with
 *     Symphonica. If not, see <https://www.gnu.org/licenses/>.
 */

package org.akanework.symphonica.ui.viewmodel

import androidx.lifecycle.ViewModel
import org.akanework.symphonica.logic.data.Album
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
}