package org.akanework.symphonica.logic.service

import android.app.IntentService
import android.app.Service
import android.content.Intent
import android.content.Context
import android.media.MediaPlayer
import android.os.IBinder
import android.provider.MediaStore.Audio.Media
import android.util.Log
import androidx.core.net.toUri
import org.akanework.symphonica.MainActivity.Companion.actuallyPlaying
import org.akanework.symphonica.MainActivity.Companion.isLoopEnabled
import org.akanework.symphonica.MainActivity.Companion.isShuffleEnabled
import org.akanework.symphonica.MainActivity.Companion.musicPlayer
import org.akanework.symphonica.MainActivity.Companion.playlistViewModel
import kotlin.random.Random

class SymphonicaPlayerService : Service(), MediaPlayer.OnPreparedListener {
    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onPrepared(mp: MediaPlayer) {
        mp.start()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when(intent.action) {
            "ACTION_REPLACE_AND_PLAY" -> {
                if (musicPlayer == null) {
                    musicPlayer = MediaPlayer()
                    setLoopListener()
                    startPlaying()
                } else {
                    stopAndPlay()
                }
            }
            "ACTION_PAUSE" -> {
                if (musicPlayer != null && musicPlayer!!.isPlaying) {
                    musicPlayer!!.pause()
                }
            }
            "ACTION_RESUME" -> {
                if (musicPlayer != null && !musicPlayer!!.isPlaying) {
                    musicPlayer!!.start()
                }
            }
            "ACTION_NEXT" -> {
                if (musicPlayer != null) {
                    musicPlayer!!.reset()
                    nextSong()
                    if (actuallyPlaying) {
                        startPlaying()
                    }
                }
            }
            "ACTION_PREV" -> {
                if (musicPlayer != null) {
                    musicPlayer!!.reset()
                    prevSong()
                    startPlaying()
                }
            }
            "ACTION_JUMP" -> {
                if (musicPlayer != null) {
                    stopAndPlay()
                } else {
                    musicPlayer = MediaPlayer()
                    setLoopListener()
                    startPlaying()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startPlaying() {
        musicPlayer!!.apply {
            setDataSource(applicationContext, playlistViewModel.playList[
                playlistViewModel.currentLocation
            ].path.toUri())
            setOnPreparedListener(this@SymphonicaPlayerService)
            prepareAsync()
            broadcastPlayStart()
            setLoopListener()
        }
    }

    private fun stopAndPlay() {
        musicPlayer!!.reset()
        startPlaying()
    }

    private fun setLoopListener() {
        musicPlayer!!.setOnCompletionListener {
            nextSong()
            if (musicPlayer != null) {
                musicPlayer!!.reset()
                musicPlayer!!.apply {
                    setDataSource(applicationContext, playlistViewModel.playList[
                        playlistViewModel.currentLocation
                    ].path.toUri())
                    setOnPreparedListener(this@SymphonicaPlayerService)
                    prepareAsync()
                    broadcastPlayStart()
                    setLoopListener()
                }
            }
        }
    }

    private fun stopPlaying() {
        musicPlayer!!.reset()
        musicPlayer!!.release()
        musicPlayer = null
    }

    private fun broadcastPlayStart() {
        val intent = Intent("org.akanework.symphonica.PLAY_START")
        sendBroadcast(intent)
    }

    private fun prevSong() {
        playlistViewModel.currentLocation =
        if (playlistViewModel.currentLocation == 0 && isLoopEnabled && !isShuffleEnabled) {
            playlistViewModel.playList.size - 1
        } else if (playlistViewModel.currentLocation == 0 && !isLoopEnabled && !isShuffleEnabled) {
            stopPlaying()
            actuallyPlaying = false
            0
        } else if (playlistViewModel.currentLocation != 0 && !isShuffleEnabled) {
            playlistViewModel.currentLocation - 1
        } else if (isShuffleEnabled && playlistViewModel.playList.size != 1) {
            Random.nextInt(0, playlistViewModel.playList.size - 1)
        } else {
            0
        }
    }

    private fun nextSong() {
        playlistViewModel.currentLocation =
            if (playlistViewModel.currentLocation == playlistViewModel.playList.size - 1 && isLoopEnabled && !isShuffleEnabled) {
                0
            } else if (playlistViewModel.currentLocation == playlistViewModel.playList.size - 1 && !isLoopEnabled && !isShuffleEnabled) {
                stopPlaying()
                actuallyPlaying = false
                0
            } else if (playlistViewModel.currentLocation != playlistViewModel.playList.size - 1 && !isShuffleEnabled) {
                playlistViewModel.currentLocation + 1
            } else if (isShuffleEnabled && playlistViewModel.playList.size != 1) {
                Random.nextInt(0, playlistViewModel.playList.size - 1)
            } else {
                0
            }
    }

}