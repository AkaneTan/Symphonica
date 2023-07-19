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

package org.akanework.symphonica

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import org.akanework.symphonica.logic.data.Song
import org.akanework.symphonica.logic.service.SymphonicaPlayerService
import org.akanework.symphonica.logic.util.convertDurationToTimeStamp
import org.akanework.symphonica.logic.util.fillSongCover
import org.akanework.symphonica.logic.util.getAllSongs

/**
 * [MiniPlayerActivity] is a mini player that can be
 * launched from file manager.
 *
 * It is separated from [SymphonicaPlayerService] and has
 * it's own media player for handling light tasks.
 *
 * This activity still needs future improvements on handler,
 * etc.
 */
class MiniPlayerActivity : AppCompatActivity() {

    val mediaPlayer: MediaPlayer = MediaPlayer()

    val handler = Handler(Looper.getMainLooper())

    private var isUserTracking = false
    private lateinit var receiveKill: KillReceiver
    private lateinit var slider: Slider
    private lateinit var timeStamp: TextView
    private lateinit var controlButton: FloatingActionButton

    private val sliderTask = object : Runnable {
        override fun run() {
            if (mediaPlayer.isPlaying) {
                slider.isEnabled = true

                slider.valueTo = mediaPlayer.duration.toFloat() / 1000

                if (!isUserTracking) {
                    slider.value = mediaPlayer.currentPosition.toFloat() / 1000
                    timeStamp.text =
                        convertDurationToTimeStamp(mediaPlayer.currentPosition.toLong())
                }

                controlButton.setImageResource(R.drawable.ic_pause)
                handler.postDelayed(this, 200)
            } else if (mediaPlayer.currentPosition >= mediaPlayer.duration - 1000) {
                slider.isEnabled = false
                controlButton.setImageResource(R.drawable.ic_sheet_play)
            } else {
                controlButton.setImageResource(R.drawable.ic_sheet_play)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_mini_player)

        MainActivity.isMiniPlayerRunning = true
        receiveKill = KillReceiver()
        registerReceiver(
            receiveKill,
            IntentFilter("internal.play_mini_player_stop"),
            RECEIVER_NOT_EXPORTED
        )

        // Kill the main player
        val intentKill = Intent(this, SymphonicaPlayerService::class.java)
        intentKill.action = "ACTION_PAUSE"
        this.startService(intentKill)

        val tempSongList: List<Song> = getAllSongs(applicationContext)

        val intent = intent
        val externalSongData: Uri? = intent.data


        val targetSong: Song?
        if (externalSongData != null) {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(this, externalSongData)

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val duration =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong()

            targetSong = tempSongList.find { it.title == title && it.duration == duration }
            if (targetSong != null) {
                findViewById<TextView>(R.id.miniplayer_song_name).text = targetSong.title
                findViewById<TextView>(R.id.miniplayer_sheet_author).text = targetSong.artist
                findViewById<TextView>(R.id.miniplayer_sheet_album).text = targetSong.album
                findViewById<TextView>(R.id.miniplayer_end_time).text =
                    convertDurationToTimeStamp(targetSong.duration)
                findViewById<TextView>(R.id.miniplayer_song_path).text = targetSong.path
                val prefs = getSharedPreferences("data", Context.MODE_PRIVATE)
                MainActivity.isGlideCacheEnabled = prefs.getBoolean("isGlideCacheEnabled", false)

                // Change glide cache strategy accordingly.
                MainActivity.diskCacheStrategyCustom = if (MainActivity.isGlideCacheEnabled) {
                    DiskCacheStrategy.AUTOMATIC
                } else {
                    DiskCacheStrategy.NONE
                }

                fillSongCover(targetSong.imgUri!!, findViewById(R.id.miniplayer_sheet_cover))
                initMediaPlayer(targetSong.path.toUri())

                slider = findViewById(R.id.miniplayer_slider)
                timeStamp = findViewById(R.id.miniplayer_now_time)
                controlButton = findViewById(R.id.miniplayer_control_button)
                val dialogButton = findViewById<MaterialButton>(R.id.miniplayer_song_info)
                val quitPlayer = findViewById<MaterialButton>(R.id.miniplayer_quit)

                mediaPlayer.start()

                // When the slider is dragged by user, mark it
                // to use this state later.
                val touchListener: Slider.OnSliderTouchListener =
                    object : Slider.OnSliderTouchListener {
                        override fun onStartTrackingTouch(slider: Slider) {
                            isUserTracking = true
                        }

                        override fun onStopTrackingTouch(slider: Slider) {
                            // This value is multiplied by 1000 is because
                            // when the number is too big (like when toValue
                            // used the duration directly) we might encounter
                            // some performance problem.
                            mediaPlayer.seekTo((slider.value * 1000).toInt())
                            isUserTracking = false
                        }
                    }

                quitPlayer.setOnClickListener {
                    finish()
                }

                slider.addOnSliderTouchListener(touchListener)

                slider.addOnChangeListener { _, value, fromUser ->
                    if (fromUser) timeStamp.text =
                        convertDurationToTimeStamp(value.toLong() * 1000)
                }

                dialogButton.setOnClickListener {
                    val rootView = MaterialAlertDialogBuilder(
                        this,
                        com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
                    )
                        .setTitle(getString(R.string.dialog_song_info))
                        .setView(R.layout.alert_dialog_song)
                        .setNeutralButton(getString(R.string.dialog_song_dismiss)) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()

                    val dialogID: TextInputEditText = rootView.findViewById(R.id.dialog_id)!!
                    val dialogName: TextInputEditText = rootView.findViewById(R.id.dialog_song)!!
                    val dialogArtist: TextInputEditText =
                        rootView.findViewById(R.id.dialog_artist)!!
                    val dialogAlbum: TextInputEditText = rootView.findViewById(R.id.dialog_album)!!
                    val dialogDuration: TextInputEditText =
                        rootView.findViewById(R.id.dialog_duration)!!
                    val dialogPath: TextInputEditText = rootView.findViewById(R.id.dialog_path)!!

                    dialogID.setText(targetSong.id.toString())
                    dialogPath.setText(targetSong.path)
                    dialogDuration.setText(targetSong.duration.toString())
                    if (targetSong.title.isNotEmpty()) {
                        dialogName.setText(targetSong.title)
                    }
                    if (targetSong.artist.isNotEmpty()) {
                        dialogArtist.setText(targetSong.artist)
                    }
                    if (targetSong.album.isNotEmpty()) {
                        dialogAlbum.setText(targetSong.album)
                    }
                }

                controlButton.setOnClickListener {
                    if (mediaPlayer.isPlaying) {
                        mediaPlayer.pause()
                        controlButton.setImageResource(R.drawable.ic_sheet_play)
                    } else {
                        mediaPlayer.start()
                        handler.postDelayed(sliderTask, 500)
                        controlButton.setImageResource(R.drawable.ic_pause)
                    }
                }

                handler.postDelayed(sliderTask, 500)
            }
        }
        onBackPressedDispatcher.addCallback(
            this /* lifecycle owner */,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finish()
                }
            })
    }

    private fun initMediaPlayer(songUri: Uri) {
        mediaPlayer.reset()
        mediaPlayer.setDataSource(this, songUri)
        mediaPlayer.prepare()
    }

    override fun onDestroy() {
        MainActivity.isMiniPlayerRunning = false
        mediaPlayer.stop()
        mediaPlayer.release()
        handler.removeCallbacks(sliderTask)
        super.onDestroy()
    }

    /**
     * [KillReceiver] receives the signals from [MainActivity] to kill
     * the miniplayer.
     */
    inner class KillReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            finish()
        }
    }
}