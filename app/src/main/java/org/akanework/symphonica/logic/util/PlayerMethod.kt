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

package org.akanework.symphonica.logic.util

import android.content.Intent
import org.akanework.symphonica.MainActivity.Companion.musicPlayer
import org.akanework.symphonica.SymphonicaApplication
import org.akanework.symphonica.logic.data.Song
import org.akanework.symphonica.logic.service.SymphonicaPlayerService

/**
 * [replacePlaylist] replaces playlist with the given argument
 * and jump to the desired location inside the new playlist.
 */
fun replacePlaylist(targetPlaylist: MutableList<Song>, index: Int) {
    musicPlayer.playlist = Playlist(targetPlaylist)
    musicPlayer.playlist!!.currentPosition = index
    val intent = Intent(SymphonicaApplication.context, SymphonicaPlayerService::class.java)
    intent.action = "ACTION_PLAY"
    SymphonicaApplication.context.startService(intent)
}

/**
 * [addToNext] adds a song to the next position of the playlist.
 */
fun addToNext(nextSong: Song) {
    musicPlayer.playlist!!.add(nextSong, musicPlayer.playlist!!.size - 1)
}

/**
 * [jumpTo] will jump to the position in playlist.
 */
fun jumpTo(index: Int) {
    val intent = Intent(SymphonicaApplication.context, SymphonicaPlayerService::class.java)
    intent.action = "ACTION_JUMP"
    intent.putExtra("index", index)
    SymphonicaApplication.context.startService(intent)
}

/**
 * [nextSong] will jump to the next song in the playlist.
 */
fun nextSong() {
    val intent = Intent(SymphonicaApplication.context, SymphonicaPlayerService::class.java)
    intent.action = "ACTION_NEXT"
    SymphonicaApplication.context.startService(intent)
}

/**
 * [thisSong] will stop the music player and play current song.
 */
fun thisSong() {
    SymphonicaApplication.context.musicPlayer.playlist?.currentPosition?.let { jumpTo(it) }
}

/**
 * [prevSong] will play the previous song in the playlist.
 */
fun prevSong() {
    val intent = Intent(SymphonicaApplication.context, SymphonicaPlayerService::class.java)
    intent.action = "ACTION_PREV"
    SymphonicaApplication.context.startService(intent)
}

/**
 * [changePlayerStatus] changes [SymphonicaPlayerService]'s
 * player status. If paused then play. If is playing then pause.
 */
fun changePlayerStatus() {
    val intent = Intent(SymphonicaApplication.context, SymphonicaPlayerService::class.java)
    intent.action = "ACTION_PLAY_PAUSE"
    SymphonicaApplication.context.startService(intent)
}