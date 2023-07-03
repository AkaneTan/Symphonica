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

package org.akanework.symphonica.logic.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.IBinder
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import org.akanework.symphonica.MainActivity
import org.akanework.symphonica.MainActivity.Companion.booleanViewModel
import org.akanework.symphonica.MainActivity.Companion.fullSheetShuffleButton
import org.akanework.symphonica.MainActivity.Companion.isListShuffleEnabled
import org.akanework.symphonica.MainActivity.Companion.musicPlayer
import org.akanework.symphonica.MainActivity.Companion.playlistViewModel
import org.akanework.symphonica.R
import org.akanework.symphonica.SymphonicaApplication.Companion.context
import org.akanework.symphonica.logic.service.SymphonicaPlayerService.Companion.notification
import org.akanework.symphonica.logic.service.SymphonicaPlayerService.Companion.updateMetadata
import org.akanework.symphonica.logic.util.broadcastMetaDataUpdate
import org.akanework.symphonica.logic.util.broadcastPlayPaused
import org.akanework.symphonica.logic.util.broadcastPlayStart
import org.akanework.symphonica.logic.util.broadcastPlayStopped
import org.akanework.symphonica.logic.util.broadcastSliderSeek
import org.akanework.symphonica.logic.util.changePlayerStatus
import org.akanework.symphonica.logic.util.nextSong
import org.akanework.symphonica.logic.util.pausePlayer
import org.akanework.symphonica.logic.util.prevSong
import org.akanework.symphonica.logic.util.resumePlayer
import org.akanework.symphonica.logic.util.thisSong
import org.akanework.symphonica.ui.component.PlaylistBottomSheet.Companion.updatePlaylistSheetLocation
import kotlin.random.Random


/**
 * [SymphonicaPlayerService] is the core of Symphonica.
 * It used [musicPlayer]'s async method to play songs.
 * It also manages [notification]'s playback metadata
 * updates.
 *
 * SubFunctions:
 * [updateMetadata]
 *
 * Arguments:
 * It receives [Intent] when being called.
 * [playlistViewModel] is used to store playlist and
 * current position across this instance.
 * ----------------------------------------------------
 * 1. "ACTION_REPLACE_AND_PLAY" will replace current playlist
 * and start playing.
 * 2. "ACTION_PAUSE" will pause current player instance.
 * 3. "ACTION_RESUME" will resume current player instance.
 * 4. "ACTION_NEXT" will make the player plays the next song
 * inside the playlist. If not, then stop the instance.
 * 5. "ACTION_PREV" similar to "ACTION_NEXT".
 * 6. "ACTION_JUMP" will jump to target song inside the playlist.
 */
class SymphonicaPlayerService : Service(), MediaPlayer.OnPreparedListener {

    private lateinit var audioManager: AudioManager
    private var isAudioManagerInitialized = false

    private var isMusicPlayerError = false

    /**
     * [focusChangeListener] is a listener build for [audioManager].
     *
     * It has four main changes :
     * - AUDIOFOCUS_LOSS: Pause player when somebody else need to play media.
     * - AUDIOFOCUS_LOSS_TRANSIENT: Same as above.
     * - AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK: Same as above
     * - AUDIOFOCUS_GAIN: Resume player when acquired media session.
     */
    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                pausePlayer()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pausePlayer()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                pausePlayer()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                resumePlayer()
            }
        }
    }

    init {
        playbackStateBuilder = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT
                        or PlaybackState.ACTION_SKIP_TO_PREVIOUS or PlaybackState.ACTION_SEEK_TO
            )
    }

    companion object {

        lateinit var playbackStateBuilder: PlaybackState.Builder

        /**
         * [setPlaybackState] sets the playback state of the
         * media control notification.
         */
        fun setPlaybackState(operation: Int) {
            when (operation) {

                0 -> playbackStateBuilder.setState(
                    PlaybackState.STATE_PLAYING,
                    musicPlayer!!.currentPosition.toLong(),
                    1.0f
                )

                1 -> playbackStateBuilder.setState(
                    PlaybackState.STATE_PAUSED,
                    if (musicPlayer != null) musicPlayer!!.currentPosition.toLong() else 0,
                    0.0f
                )

                else -> throw IllegalArgumentException()
            }
            mediaSession.setPlaybackState(playbackStateBuilder.build())
        }

        val mediaSession = MediaSession(context, "PlayerService")
        private val mediaStyle: Notification.MediaStyle =
            Notification.MediaStyle().setMediaSession(mediaSession.sessionToken)
        private val notification = Notification.Builder(context, "channel_symphonica")
            .setStyle(mediaStyle)
            .setSmallIcon(R.drawable.ic_note)
            .setActions()
            .build()


        /**
         * [updateMetadata] is used for [notification] to update its
         * metadata information. You can find this functions all across
         * Symphonica.
         *
         * It does not need any arguments, instead it uses the [playlistViewModel]
         * and [Glide] to update it's info. You can call it up anywhere.
         *
         * About suppress, we don't need notification permission for only
         * sending the music notification.
         */
        @SuppressLint("NotificationPermission")
        fun updateMetadata() {
            var initialized = false
            lateinit var bitmapResource: Bitmap
            try {
                Glide.with(context)
                    .asBitmap()
                    .load(playlistViewModel.playList[playlistViewModel.currentLocation].imgUri)
                    .placeholder(R.drawable.ic_song_default_cover)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            bitmapResource = resource
                            initialized = true
                            mediaSession.setMetadata(
                                MediaMetadata.Builder()
                                    .putString(
                                        MediaMetadata.METADATA_KEY_TITLE,
                                        playlistViewModel.playList[playlistViewModel.currentLocation].title
                                    )
                                    .putString(
                                        MediaMetadata.METADATA_KEY_ARTIST,
                                        playlistViewModel.playList[playlistViewModel.currentLocation].artist
                                    )
                                    .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bitmapResource)
                                    .putLong(
                                        MediaMetadata.METADATA_KEY_DURATION,
                                        playlistViewModel.playList[playlistViewModel.currentLocation].duration
                                    )
                                    .build()
                            )
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            // this is called when imageView is cleared on lifecycle call or for
                            // some other reason.
                            // if you are referencing the bitmap somewhere else too other than this imageView
                            // clear it here as you can no longer have the bitmap
                        }
                    })
            } catch (_: Exception) {
                // Placeholder here.
            }
            if (!initialized) {
                mediaSession.setMetadata(
                    MediaMetadata.Builder()
                        .putString(
                            MediaMetadata.METADATA_KEY_TITLE,
                            playlistViewModel.playList[playlistViewModel.currentLocation].title
                        )
                        .putString(
                            MediaMetadata.METADATA_KEY_ARTIST,
                            playlistViewModel.playList[playlistViewModel.currentLocation].artist
                        )
                        .putBitmap(
                            MediaMetadata.METADATA_KEY_ALBUM_ART,
                            AppCompatResources.getDrawable(
                                context,
                                R.drawable.ic_album_notification_cover
                            )!!.toBitmap()
                        )
                        .putLong(
                            MediaMetadata.METADATA_KEY_DURATION,
                            playlistViewModel.playList[playlistViewModel.currentLocation].duration
                        )
                        .build()
                )
            }
            MainActivity.managerSymphonica.notify(1, notification)
        }
    }

    private val mediaSessionCallback = object : MediaSession.Callback() {
        override fun onSeekTo(pos: Long) {
            musicPlayer?.seekTo(pos.toInt())

            broadcastSliderSeek()
        }

        override fun onSkipToNext() {
            nextSong()
        }

        override fun onSkipToPrevious() {
            prevSong()
        }

        override fun onPause() {
            if (musicPlayer != null) {
                changePlayerStatus()
            } else if (playlistViewModel.playList.size != 0
                && playlistViewModel.currentLocation != playlistViewModel.playList.size
            ) {
                thisSong()
            }
        }

        override fun onPlay() {
            if (musicPlayer != null) {
                changePlayerStatus()
            } else if (playlistViewModel.playList.size != 0
                && playlistViewModel.currentLocation != playlistViewModel.playList.size
            ) {
                thisSong()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }


    override fun onPrepared(mp: MediaPlayer) {
        mp.start()
        mediaSession.isActive = true
        mediaSession.setCallback(mediaSessionCallback)
        updateMetadata()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (!isAudioManagerInitialized) {

            audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

            val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()

            val result = audioManager.requestAudioFocus(audioFocusRequest)
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                // Audio focus is granted, resume playback here
                resumePlayer()
            }

            isAudioManagerInitialized = true
        }
        when (intent.action) {
            "ACTION_REPLACE_AND_PLAY" -> {
                if (musicPlayer == null) {
                    musicPlayer = MediaPlayer()
                    musicPlayer!!.setOnErrorListener { _, _, _ ->
                        isMusicPlayerError = true
                        false
                    }
                    startPlaying()
                    setLoopListener()
                } else {
                    stopAndPlay()
                }
            }

            "ACTION_PAUSE" -> {
                if (musicPlayer != null && musicPlayer!!.isPlaying) {
                    musicPlayer!!.pause()
                    broadcastPlayPaused()
                }
            }

            "ACTION_RESUME" -> {
                if (musicPlayer != null && !musicPlayer!!.isPlaying) {
                    musicPlayer!!.start()
                    broadcastPlayStart()
                    if (MainActivity.managerSymphonica.activeNotifications.isEmpty()) {
                        mediaSession.setCallback(mediaSessionCallback)
                    }
                    killMiniPlayer()
                }
            }

            "ACTION_NEXT" -> {
                if (musicPlayer != null) {
                    musicPlayer!!.reset()
                    nextSongDecisionMaker()
                    startPlaying()
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
                    musicPlayer!!.setOnErrorListener { _, _, _ ->
                        isMusicPlayerError = true
                        false
                    }
                    setLoopListener()
                    startPlaying()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startPlaying() {
        if (musicPlayer != null) {
            killMiniPlayer()
            musicPlayer!!.apply {
                setDataSource(
                    applicationContext, playlistViewModel.playList[
                        playlistViewModel.currentLocation
                    ].path.toUri()
                )
                setOnPreparedListener(this@SymphonicaPlayerService)
                prepareAsync()
                broadcastPlayStart()
                if (MainActivity.managerSymphonica.activeNotifications.isEmpty()) {
                    mediaSession.setCallback(mediaSessionCallback)
                }
                setLoopListener()
            }
        }
    }

    private fun stopAndPlay() {
        musicPlayer!!.reset()
        startPlaying()
    }

    private fun setLoopListener() {
        musicPlayer!!.setOnCompletionListener {
            if (!isMusicPlayerError) {
                nextSongDecisionMaker()
            }
            isMusicPlayerError = false
            if (musicPlayer != null) {
                musicPlayer!!.reset()
                musicPlayer!!.apply {
                    setDataSource(
                        applicationContext, playlistViewModel.playList[
                            playlistViewModel.currentLocation
                        ].path.toUri()
                    )
                    setOnPreparedListener(this@SymphonicaPlayerService)
                    prepareAsync()
                    broadcastPlayStart()
                    if (MainActivity.managerSymphonica.activeNotifications.isEmpty()) {
                        mediaSession.setCallback(mediaSessionCallback)
                    }
                    setLoopListener()
                    killMiniPlayer()
                }
            }
        }
    }

    private fun killMiniPlayer() {
        if (MainActivity.isMiniPlayerRunning) {
            // Send a broadcast to finish MiniPlayerActivity.
            val intentKillBroadcast = Intent("internal.play_mini_player_stop")
            sendBroadcast(intentKillBroadcast)
        }
    }

    private fun stopPlaying() {
        musicPlayer!!.reset()
        musicPlayer!!.release()
        musicPlayer = null
        broadcastPlayStopped()
        broadcastMetaDataUpdate()
    }

    private fun prevSongDecisionMaker() {
        val previousLocation = playlistViewModel.currentLocation
        if (!isListShuffleEnabled && booleanViewModel.loopButtonStatus != 2) {
            playlistViewModel.currentLocation =
                if (playlistViewModel.currentLocation == 0 && booleanViewModel.loopButtonStatus == 1 && !fullSheetShuffleButton!!.isChecked) {
                    playlistViewModel.playList.size - 1
                } else if (playlistViewModel.currentLocation == 0 && booleanViewModel.loopButtonStatus == 0 && !fullSheetShuffleButton!!.isChecked) {
                    stopPlaying()
                    0
                } else if (playlistViewModel.currentLocation != 0 && !fullSheetShuffleButton!!.isChecked) {
                    playlistViewModel.currentLocation - 1
                } else if (fullSheetShuffleButton!!.isChecked && playlistViewModel.playList.size != 1) {
                    Random.nextInt(0, playlistViewModel.playList.size)
                } else {
                    0
                }
        } else if (booleanViewModel.loopButtonStatus != 2) {
            playlistViewModel.currentLocation =
                if (playlistViewModel.currentLocation == 0 && booleanViewModel.loopButtonStatus == 0) {
                    playlistViewModel.playList.size - 1
                } else if (playlistViewModel.currentLocation == 0 && booleanViewModel.loopButtonStatus == 1) {
                    stopPlaying()
                    0
                } else {
                    playlistViewModel.currentLocation - 1
                }
        }

        // Who the fuck opens the playlist and use media control to select the
        // previous song? Not me.
        updatePlaylistSheetLocation(previousLocation)
    }

    private fun nextSongDecisionMaker() {
        val previousLocation = playlistViewModel.currentLocation
        if (!isListShuffleEnabled && booleanViewModel.loopButtonStatus != 2) {
            playlistViewModel.currentLocation =
                if (playlistViewModel.currentLocation == playlistViewModel.playList.size - 1 && booleanViewModel.loopButtonStatus == 1 && !fullSheetShuffleButton!!.isChecked) {
                    0
                } else if (playlistViewModel.currentLocation == playlistViewModel.playList.size - 1 && booleanViewModel.loopButtonStatus == 0 && !fullSheetShuffleButton!!.isChecked) {
                    stopPlaying()
                    0
                } else if (playlistViewModel.currentLocation != playlistViewModel.playList.size - 1 && !fullSheetShuffleButton!!.isChecked) {
                    playlistViewModel.currentLocation + 1
                } else if (fullSheetShuffleButton!!.isChecked && playlistViewModel.playList.size != 1) {
                    Random.nextInt(0, playlistViewModel.playList.size)
                } else {
                    0
                }
        } else if (booleanViewModel.loopButtonStatus != 2) {
            playlistViewModel.currentLocation =
                if (playlistViewModel.currentLocation == playlistViewModel.playList.size - 1 &&
                    booleanViewModel.loopButtonStatus == 1
                ) {
                    0
                } else if (playlistViewModel.currentLocation == playlistViewModel.playList.size - 1 &&
                    booleanViewModel.loopButtonStatus == 0
                ) {
                    stopPlaying()
                    0
                } else {
                    playlistViewModel.currentLocation + 1
                }
        }
        updatePlaylistSheetLocation(previousLocation)
    }

}