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
import org.akanework.symphonica.MainActivity.Companion.playlistDatabase
import org.akanework.symphonica.logic.data.PlaylistDataEntity
import org.akanework.symphonica.logic.data.Song

/**
 * [PlaylistViewModel] is a ViewModel that stores
 * playlist information.
 *
 * Arguments:
 * [playList] is a list of [Song].
 * [originalPlaylist] is [Song]'s original list (without shuffle)
 * [currentLocation] stores player's location.
 */
class PlaylistViewModel : ViewModel() {
    var playList = mutableListOf<Song>()
    var originalPlaylist = mutableListOf<Song>()
    var currentLocation: Int = 0
    val playlistList: MutableList<PlaylistDataEntity> = mutableListOf()

    suspend fun createPlaylist(name: String, desc: String): PlaylistDataEntity {
        val playlist = PlaylistDataEntity(name, desc, mutableListOf())
        val playlistId = playlistDatabase.playlistDao().createPlaylist(playlist)
        return playlist.copy(id = playlistId)
    }

    suspend fun deletePlaylist(playlist: PlaylistDataEntity) {
        playlistDatabase.playlistDao().deletePlaylist(playlist)
    }
}