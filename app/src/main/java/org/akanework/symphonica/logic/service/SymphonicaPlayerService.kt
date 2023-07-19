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
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.IBinder
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import org.akanework.symphonica.MainActivity
import org.akanework.symphonica.MainActivity.Companion.musicPlayer
import org.akanework.symphonica.R
import org.akanework.symphonica.SymphonicaApplication.Companion.context
import org.akanework.symphonica.logic.data.Song
import org.akanework.symphonica.logic.util.MediaStateCallback
import org.akanework.symphonica.logic.util.Playlist
import org.akanework.symphonica.logic.util.PlaylistCallbacks
import org.akanework.symphonica.logic.util.Timestamp
import org.akanework.symphonica.logic.util.changePlayerStatus
import org.akanework.symphonica.logic.util.nextSong
import org.akanework.symphonica.logic.util.prevSong

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
 * ----------------------------------------------------
 * 1. "ACTION_PLAY" will start playing.
 * 2. "ACTION_PAUSE" will pause current player instance.
 * 3. "ACTION_RESUME" will resume current player instance.
 * 4. "ACTION_NEXT" will make the player plays the next song
 * inside the playlist. If not, then stop the instance.
 * 5. "ACTION_PREV" similar to "ACTION_NEXT".
 * 6. "ACTION_JUMP" will jump to target song inside the playlist.
 */
class SymphonicaPlayerService : Service(), MediaStateCallback, PlaylistCallbacks<Song> {

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
     * It does not need any arguments, instead it uses the playlistViewModel
     * and [Glide] to update it's info. You can call it up anywhere.
     */
    @SuppressLint("NotificationPermission") // not needed for media notifications
    fun updateMetadata() {
        var initialized = false
        lateinit var bitmapResource: Bitmap
        try {
            Glide.with(context)
                .asBitmap()
                .load(musicPlayer.playlist!!.getItem(musicPlayer.playlist!!.currentPosition)!!.imgUri)
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
                                    musicPlayer.playlist!!.getItem(musicPlayer.playlist!!.currentPosition)!!.title
                                )
                                .putString(
                                    MediaMetadata.METADATA_KEY_ARTIST,
                                    musicPlayer.playlist!!.getItem(musicPlayer.playlist!!.currentPosition)!!.artist
                                )
                                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bitmapResource)
                                .putLong(
                                    MediaMetadata.METADATA_KEY_DURATION,
                                    musicPlayer.playlist!!.getItem(musicPlayer.playlist!!.currentPosition)!!.duration
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
                        musicPlayer.playlist!!.getItem(musicPlayer.playlist!!.currentPosition)!!.title
                    )
                    .putString(
                        MediaMetadata.METADATA_KEY_ARTIST,
                        musicPlayer.playlist!!.getItem(musicPlayer.playlist!!.currentPosition)!!.artist
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
                        musicPlayer.playlist!!.getItem(musicPlayer.playlist!!.currentPosition)!!.duration
                    )
                    .build()
            )
        }
        MainActivity.managerSymphonica.notify(1, notification)
        updatePlaybackState()
    }

    private val mediaSessionCallback = object : MediaSession.Callback() {
        override fun onSeekTo(pos: Long) {
            musicPlayer.seekTo(pos)
        }

        override fun onSkipToNext() {
            nextSong()
        }

        override fun onSkipToPrevious() {
            prevSong()
        }

        override fun onPause() {
            changePlayerStatus()
        }

        override fun onPlay() {
            changePlayerStatus()
        }
    }

    override fun onCreate() {
        super.onCreate()
        musicPlayer.addMediaStateCallback(this)
        musicPlayer.registerPlaylistCallback(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        musicPlayer.removeMediaStateCallback(this)
        musicPlayer.unregisterPlaylistCallback(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_PLAY" -> {
                musicPlayer.play()
            }

            "ACTION_PAUSE" -> {
                if (musicPlayer.isPlaying) {
                    musicPlayer.pause()
                }
            }

            "ACTION_PLAY_PAUSE" -> {
                musicPlayer.playOrPause()
            }

            "ACTION_RESUME" -> {
                if (!musicPlayer.isPlaying) {
                    musicPlayer.play()
                }
            }

            "ACTION_NEXT" -> {
                musicPlayer.next()
            }

            "ACTION_PREV" -> {
                musicPlayer.prev()
            }

            "ACTION_JUMP" -> {
                val nextSong = intent.getIntExtra("index", 0)
                musicPlayer.playlist!!.currentPosition = nextSong
            }
        }
        return START_STICKY
    }

    private fun killMiniPlayer() {
        if (MainActivity.isMiniPlayerRunning) {
            // Send a broadcast to finish MiniPlayerActivity.
            val intentKillBroadcast = Intent("internal.play_mini_player_stop")
            sendBroadcast(intentKillBroadcast)
        }
    }

    private fun updatePlaybackState(fakePlaying: Boolean = !musicPlayer.isPlaying) {
        // This method has a funny hack, because Android does NOT care about ANY position
        // update while we are paused, so we have to fake it to playing. Don't ask me why.
        mediaSession.setPlaybackState(PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT
                    or PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                        (if (musicPlayer.isSeekable) PlaybackState.ACTION_SEEK_TO else 0))
            .setState(
                if (musicPlayer.slowBuffer) PlaybackState.STATE_BUFFERING
                else if (musicPlayer.isPlaying || fakePlaying) PlaybackState.STATE_PLAYING
                else PlaybackState.STATE_PAUSED,
                if (musicPlayer.isSeekable) musicPlayer.currentTimestamp
                else PlaybackState.PLAYBACK_POSITION_UNKNOWN,
                if (musicPlayer.isPlaying) musicPlayer.speed else 0f,
                musicPlayer.timestampUpdateTime
            )
            .setBufferedPosition((musicPlayer.bufferProgress * musicPlayer.duration).toLong())
            .build())
        if (fakePlaying) {
            updatePlaybackState(false)
        }
    }

    override fun onPlayingStatusChanged(playing: Boolean) {
        if (playing) {
            mediaSession.isActive = true
            mediaSession.setCallback(mediaSessionCallback)
            updateMetadata()
            if (MainActivity.managerSymphonica.activeNotifications.isEmpty()) {
                mediaSession.setCallback(mediaSessionCallback)
            }
        }
    }

    override fun onUserPlayingStatusChanged(playing: Boolean) {
        if (playing) {
            killMiniPlayer()
        }
    }

    override fun onLiveInfoAvailable(text: String) {
        // TODO
    }

    override fun onMediaTimestampChanged(timestampMillis: Long) {
        // We care about onMediaTimestampBaseChanged() instead, we are in charge of notification
    }

    override fun onMediaTimestampBaseChanged(timestampBase: Timestamp) {
        updatePlaybackState()
    }

    override fun onSetSeekable(seekable: Boolean) {
        updatePlaybackState()
    }

    override fun onMediaBufferSlowStatus(slowBuffer: Boolean) {
        updatePlaybackState()
    }

    override fun onMediaBufferProgress(progress: Float) {
        updatePlaybackState()
    }

    override fun onMediaHasDecreasedPerformance() {
        // TODO
    }

    override fun onPlaybackError(what: Int) {
        // TODO
    }

    override fun onDurationAvailable(durationMillis: Long) {
        updatePlaybackState()
    }

    override fun onPlaybackSettingsChanged(volume: Float, speed: Float, pitch: Float) {
        updatePlaybackState()
    }

    override fun onPlaylistReplaced(oldPlaylist: Playlist<Song>?, newPlaylist: Playlist<Song>?) {
        // TODO
    }

    override fun onPlaylistPositionChanged(oldPosition: Int, newPosition: Int) {
        updateMetadata()
    }

    override fun onPlaylistItemAdded(position: Int) {
        // TODO
    }

    override fun onPlaylistItemRemoved(position: Int) {
        // TODO
    }

}