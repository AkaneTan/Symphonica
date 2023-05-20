package org.akanework.symphonica.logic.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.IBinder
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import org.akanework.symphonica.MainActivity
import org.akanework.symphonica.MainActivity.Companion.actuallyPlaying
import org.akanework.symphonica.MainActivity.Companion.isLoopEnabled
import org.akanework.symphonica.MainActivity.Companion.isShuffleEnabled
import org.akanework.symphonica.MainActivity.Companion.musicPlayer
import org.akanework.symphonica.MainActivity.Companion.playlistViewModel
import org.akanework.symphonica.R
import org.akanework.symphonica.SymphonicaApplication.Companion.context
import org.akanework.symphonica.logic.util.changePlayer
import org.akanework.symphonica.logic.util.nextSong
import org.akanework.symphonica.logic.util.prevSong
import kotlin.random.Random

class SymphonicaPlayerService : Service(), MediaPlayer.OnPreparedListener {

    /*


     */

    companion object {
        fun updatePlaybackState() {
            musicPlayer?.let {
                val playbackStateBuilder = PlaybackState.Builder()
                    .setActions(PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS)

                if (actuallyPlaying) {
                    playbackStateBuilder.setState(
                        PlaybackState.STATE_PLAYING,
                        it.currentPosition.toLong(),
                        1.0f
                    )
                } else {
                    playbackStateBuilder.setState(
                        PlaybackState.STATE_PAUSED,
                        it.currentPosition.toLong(),
                        0.0f
                    )
                }

                mediaSession.setPlaybackState(playbackStateBuilder.build())
            }
        }

        val mediaSession = MediaSession(context, "PlayerService")
        val mediaStyle = Notification.MediaStyle().setMediaSession(mediaSession.sessionToken)
        val notification = Notification.Builder(context, "channel_symphonica")
            .setStyle(mediaStyle)
            .setSmallIcon(R.drawable.ic_note)
            .setActions()
            .build()


        fun updateMetadata() {
            if (playlistViewModel.playList[playlistViewModel.currentLocation].cover != null) {
                mediaSession.setMetadata(
                    MediaMetadata.Builder()
                        .putString(MediaMetadata.METADATA_KEY_TITLE, playlistViewModel.playList[playlistViewModel.currentLocation].title)
                        .putString(MediaMetadata.METADATA_KEY_ARTIST, playlistViewModel.playList[playlistViewModel.currentLocation].artist)
                        .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, playlistViewModel.playList[playlistViewModel.currentLocation].cover!!.toBitmap())
                        .putLong(MediaMetadata.METADATA_KEY_DURATION, playlistViewModel.playList[playlistViewModel.currentLocation].duration)
                        .build()
                )
            } else {
                mediaSession.setMetadata(
                    MediaMetadata.Builder()
                        .putString(MediaMetadata.METADATA_KEY_TITLE, playlistViewModel.playList[playlistViewModel.currentLocation].title)
                        .putString(MediaMetadata.METADATA_KEY_ARTIST, playlistViewModel.playList[playlistViewModel.currentLocation].artist)
                        .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, AppCompatResources.getDrawable(context, R.drawable.ic_album_default_cover)!!.toBitmap())
                        .putLong(MediaMetadata.METADATA_KEY_DURATION, playlistViewModel.playList[playlistViewModel.currentLocation].duration)
                        .build()
                )
            }
            MainActivity.managerSymphonica.notify(1, notification)
        }
    }

    private val mediaSessionCallback = object : MediaSession.Callback() {
        override fun onSeekTo(pos: Long) {
            musicPlayer?.seekTo(pos.toInt())
            updatePlaybackState()
        }

        override fun onSkipToNext() {
            nextSong()
        }

        override fun onSkipToPrevious() {
            prevSong()
        }

        override fun onPause() {
            changePlayer()
            updatePlaybackState()
        }

        override fun onPlay() {
            changePlayer()
            updatePlaybackState()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }


    override fun onPrepared(mp: MediaPlayer) {
        mp.start()
        mediaSession.setActive(true)
        mediaSession.setCallback(mediaSessionCallback)
        updateMetadata()
        updatePlaybackState()
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
                    nextSongDecisionMaker()
                    if (actuallyPlaying) {
                        startPlaying()
                    }
                }
            }
            "ACTION_PREV" -> {
                if (musicPlayer != null) {
                    musicPlayer!!.reset()
                    prevSongDecisionMaker()
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
            nextSongDecisionMaker()
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
        if (MainActivity.managerSymphonica.activeNotifications.isEmpty()) {
            mediaSession.setCallback(mediaSessionCallback)
        }
    }

    private fun prevSongDecisionMaker() {
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

    private fun nextSongDecisionMaker() {
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