package org.akanework.symphonica.logic.util

import android.content.Intent
import org.akanework.symphonica.MainActivity.Companion.actuallyPlaying
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
    actuallyPlaying = true
}

fun addToNext(nextSong: Song) {
    playlistViewModel.playList.add(playlistViewModel.currentLocation, nextSong)
}

fun jumpTo(index: Int) {
    playlistViewModel.currentLocation = index
    val intent = Intent(SymphonicaApplication.context, SymphonicaPlayerService::class.java)
    intent.action = "ACTION_JUMP"
    SymphonicaApplication.context.startService(intent)
    actuallyPlaying = true
}

fun nextSong() {
    val intent = Intent(SymphonicaApplication.context, SymphonicaPlayerService::class.java)
    intent.action = "ACTION_NEXT"
    SymphonicaApplication.context.startService(intent)
}

fun thisSong() {
    val intent = Intent(SymphonicaApplication.context, SymphonicaPlayerService::class.java)
    intent.action = "ACTION_REPLACE_AND_PLAY"
    intent.putExtra("Position", playlistViewModel.currentLocation)
    SymphonicaApplication.context.startService(intent)
    actuallyPlaying = true
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
    actuallyPlaying = false
}

fun resumePlayer() {
    val intent = Intent(SymphonicaApplication.context, SymphonicaPlayerService::class.java)
    intent.action = "ACTION_RESUME"
    SymphonicaApplication.context.startService(intent)
    actuallyPlaying = true
}

fun changePlayer() {
    if (musicPlayer != null && musicPlayer!!.isPlaying) {
        pausePlayer()
    } else {
        resumePlayer()
    }
}