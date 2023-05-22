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

package org.akanework.symphonica.logic.util

import android.content.Intent
import org.akanework.symphonica.MainActivity.Companion.musicPlayer
import org.akanework.symphonica.MainActivity.Companion.playlistViewModel
import org.akanework.symphonica.SymphonicaApplication
import org.akanework.symphonica.logic.data.Song
import org.akanework.symphonica.logic.service.SymphonicaPlayerService

fun replacePlaylist(targetPlaylist: MutableList<Song>, index: Int) {
    val intent = Intent(SymphonicaApplication.context, SymphonicaPlayerService::class.java)
    playlistViewModel.playList = targetPlaylist
    playlistViewModel.currentLocation = index
    intent.action = "ACTION_REPLACE_AND_PLAY"
    SymphonicaApplication.context.startService(intent)
}

fun addToNext(nextSong: Song) {
    if (playlistViewModel.currentLocation < playlistViewModel.playList.size) {
        playlistViewModel.playList.add(playlistViewModel.currentLocation + 1, nextSong)
    } else {
        playlistViewModel.playList.add(playlistViewModel.currentLocation, nextSong)
    }
    if (musicPlayer == null) {
        if (playlistViewModel.playList.size != 1) {
            playlistViewModel.currentLocation++
        }
        thisSong()
    }
}

fun jumpTo(index: Int) {
    playlistViewModel.currentLocation = index
    val intent = Intent(SymphonicaApplication.context, SymphonicaPlayerService::class.java)
    intent.action = "ACTION_JUMP"
    SymphonicaApplication.context.startService(intent)
}

fun nextSong() {
    if (musicPlayer != null && !musicPlayer!!.isPlaying) {
        thisSong()
    }
    val intent = Intent(SymphonicaApplication.context, SymphonicaPlayerService::class.java)
    intent.action = "ACTION_NEXT"
    SymphonicaApplication.context.startService(intent)
}

fun thisSong() {
    val intent = Intent(SymphonicaApplication.context, SymphonicaPlayerService::class.java)
    intent.action = "ACTION_REPLACE_AND_PLAY"
    intent.putExtra("Position", playlistViewModel.currentLocation)
    SymphonicaApplication.context.startService(intent)
}

fun prevSong() {
    val intent = Intent(SymphonicaApplication.context, SymphonicaPlayerService::class.java)
    intent.action = "ACTION_PREV"
    SymphonicaApplication.context.startService(intent)
}

fun pausePlayer() {
    val intent = Intent(SymphonicaApplication.context, SymphonicaPlayerService::class.java)
    intent.action = "ACTION_PAUSE"
    SymphonicaApplication.context.startService(intent)
}

fun resumePlayer() {
    val intent = Intent(SymphonicaApplication.context, SymphonicaPlayerService::class.java)
    intent.action = "ACTION_RESUME"
    SymphonicaApplication.context.startService(intent)
}

fun changePlayer() {
    if (musicPlayer != null && musicPlayer!!.isPlaying) {
        pausePlayer()
    } else {
        resumePlayer()
    }
}