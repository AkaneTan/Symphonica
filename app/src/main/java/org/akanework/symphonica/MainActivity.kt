/*
 *     Copyright (C) 2023  Akane Foundation
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

@file:Suppress("KotlinConstantConditions")

package org.akanework.symphonica

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.symphonica.SymphonicaApplication.Companion.context
import org.akanework.symphonica.logic.data.loadDataFromDisk
import org.akanework.symphonica.logic.database.HistoryDatabase
import org.akanework.symphonica.logic.database.PlaylistDatabase
import org.akanework.symphonica.logic.service.SymphonicaPlayerService
import org.akanework.symphonica.logic.service.SymphonicaPlayerService.Companion.OPERATION_PAUSE
import org.akanework.symphonica.logic.service.SymphonicaPlayerService.Companion.OPERATION_PLAY
import org.akanework.symphonica.logic.service.SymphonicaPlayerService.Companion.setPlaybackState
import org.akanework.symphonica.logic.service.SymphonicaPlayerService.Companion.timer
import org.akanework.symphonica.logic.service.SymphonicaPlayerService.Companion.timerValue
import org.akanework.symphonica.logic.service.SymphonicaPlayerService.Companion.updateMetadata
import org.akanework.symphonica.logic.util.broadcastMetaDataUpdate
import org.akanework.symphonica.logic.util.broadcastPlayPaused
import org.akanework.symphonica.logic.util.broadcastSliderSeek
import org.akanework.symphonica.logic.util.convertDurationToTimeStamp
import org.akanework.symphonica.logic.util.nextSong
import org.akanework.symphonica.logic.util.prevSong
import org.akanework.symphonica.logic.util.px
import org.akanework.symphonica.logic.util.sortAlbumListByTrackNumber
import org.akanework.symphonica.logic.util.thisSong
import org.akanework.symphonica.logic.util.userChangedPlayerStatus
import org.akanework.symphonica.ui.component.PlaylistBottomSheet
import org.akanework.symphonica.ui.component.SquigglyView
import org.akanework.symphonica.ui.fragment.HomeFragment
import org.akanework.symphonica.ui.fragment.LibraryFragment
import org.akanework.symphonica.ui.fragment.SettingsFragment
import org.akanework.symphonica.ui.viewmodel.ControllerViewModel
import org.akanework.symphonica.ui.viewmodel.ControllerViewModel.Companion.LOOP
import org.akanework.symphonica.ui.viewmodel.ControllerViewModel.Companion.LOOP_SINGLE
import org.akanework.symphonica.ui.viewmodel.ControllerViewModel.Companion.NOT_IN_LOOP
import org.akanework.symphonica.ui.viewmodel.LibraryViewModel
import org.akanework.symphonica.ui.viewmodel.PlaylistViewModel

/**
 * [MainActivity] is the heart of Symphonica.
 * Google said, let there be fragments, so akane answered:
 * "There would be fragments."
 */
class MainActivity : AppCompatActivity() {

    // These are the variables needed throughout MainActivity.
    private var isUserTracking = false

    // This is the coroutineScope used across MainActivity.
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val sliderTask = object : Runnable {
        override fun run() {
            if (musicPlayer != null && musicPlayer!!.isPlaying && playlistViewModel.currentLocation !=
                    playlistViewModel.playList.size
            ) {
                fullSheetSlider.isEnabled = true

                val valueTo = musicPlayer!!.duration.toFloat() / PLAYER_SLIDER_VALUE_MULTIPLE
                if (valueTo < fullSheetSlider.value) {
                    fullSheetSlider.value = 0f
                }
                fullSheetSlider.valueTo = valueTo

                if (!isUserTracking && musicPlayer!!.currentPosition.toFloat() / PLAYER_SLIDER_VALUE_MULTIPLE <= fullSheetSlider.valueTo) {
                    val addVar = musicPlayer!!.currentPosition.toFloat() / PLAYER_SLIDER_VALUE_MULTIPLE
                    if (addVar <= fullSheetSlider.valueTo) {
                        fullSheetSlider.value = musicPlayer!!.currentPosition.toFloat() / PLAYER_SLIDER_VALUE_MULTIPLE
                    }

                    fullSheetTimeStamp.text =
                            convertDurationToTimeStamp(musicPlayer!!.currentPosition.toString())
                }

                // Update it per 200ms.
                handler.postDelayed(this, SLIDER_UPDATE_INTERVAL)
            }
        }
    }

    // Below are the variables used across MainActivity
    // to update themselves.

    private lateinit var bottomSheetSongName: TextView
    private lateinit var bottomSheetArtistAndAlbum: TextView
    private lateinit var fullSheetSongName: TextView
    private lateinit var fullSheetArtist: TextView
    private lateinit var fullSheetDuration: TextView
    private lateinit var fullSheetTimeStamp: TextView
    private lateinit var bottomSheetControlButton: MaterialButton
    private lateinit var fullSheetControlButton: MaterialButton
    private lateinit var fullSheetSlider: Slider
    private lateinit var fullSheetSquigglyView: SquigglyView
    private lateinit var fullSheetSquigglyViewFrame: FrameLayout
    private lateinit var fullSheetTimerButton: MaterialButton
    private lateinit var receiverPlay: SheetPlayReceiver
    private lateinit var receiverStop: SheetStopReceiver
    private lateinit var receiverPause: SheetPauseReceiver
    private lateinit var receiverSeek: SheetSeekReceiver
    private lateinit var receiverUpdate: SheetUpdateReceiver
    private lateinit var receiverSquigglyUpdate: SheetSquigglyReceiver
    private lateinit var playlistButton: MaterialButton
    private lateinit var fragmentContainerView: FragmentContainerView
    private lateinit var playerBottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var bottomFullSizePlayerPreview: LinearLayout
    private lateinit var bottomPlayerPreview: FrameLayout

    private fun updateAlbumView(view: View) {
        val sheetAlbumCover: ImageView? = view.findViewById(R.id.sheet_album_cover)
        val fullSheetCover: ImageView? = view.findViewById(R.id.sheet_cover)
        sheetAlbumCover?.setImageResource(R.drawable.ic_song_default_cover)
        sheetAlbumCover?.let {
            Glide.with(context)
                .load(playlistViewModel.playList[playlistViewModel.currentLocation].imgUri)
                .diskCacheStrategy(diskCacheStrategyCustom)
                .into(sheetAlbumCover)
        }
        fullSheetCover?.setImageResource(R.drawable.ic_song_default_cover)
        fullSheetCover?.let {
            Glide.with(context)
                .load(playlistViewModel.playList[playlistViewModel.currentLocation].imgUri)
                .diskCacheStrategy(diskCacheStrategyCustom)
                .into(fullSheetCover)
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isMainActivityActive = true

        // Get customized options.
        val prefs = getSharedPreferences("data", Context.MODE_PRIVATE)
        isColorfulButtonEnabled = prefs.getBoolean("isColorfulButtonEnabled", true)
        isGlideCacheEnabled = prefs.getBoolean("isGlideCacheEnabled", false)
        isForceLoadingEnabled = prefs.getBoolean("isForceLoadingEnabled", false)
        isForceDarkModeEnabled = prefs.getBoolean("isForceDarkModeEnabled", false)
        isLibraryShuffleButtonEnabled = prefs.getBoolean("isLibraryShuffleButtonEnabled", false)
        isListShuffleEnabled = prefs.getBoolean("isListShuffleEnabled", true)
        isEasterEggDiscovered = prefs.getBoolean("isEasterEggDiscovered", false)
        isAkaneVisible = prefs.getBoolean("isAkaneVisible", false)
        isSquigglyProgressBarEnabled = prefs.getBoolean("isSquigglyProgressBarEnabled", false)

        // Go to dark mode if force dark mode is on.
        if (isForceDarkModeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        // Change glide cache strategy accordingly.
        diskCacheStrategyCustom = if (isGlideCacheEnabled) {
            DiskCacheStrategy.AUTOMATIC
        } else {
            DiskCacheStrategy.NONE
        }

        // Register event receiver for bottom sheet.
        // This receiver should be receiving Broadcast from
        // SymphonicaPlayerService.kt .

        receiverPause = SheetPauseReceiver()
        receiverPlay = SheetPlayReceiver()
        receiverStop = SheetStopReceiver()
        receiverSeek = SheetSeekReceiver()
        receiverUpdate = SheetUpdateReceiver()
        receiverSquigglyUpdate = SheetSquigglyReceiver()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiverPause, IntentFilter("internal.play_pause"), RECEIVER_NOT_EXPORTED)
            registerReceiver(receiverPlay, IntentFilter("internal.play_start"), RECEIVER_NOT_EXPORTED)
            registerReceiver(receiverStop, IntentFilter("internal.play_stop"), RECEIVER_NOT_EXPORTED)
            registerReceiver(receiverSeek, IntentFilter("internal.play_seek"), RECEIVER_NOT_EXPORTED)
            registerReceiver(
                receiverUpdate,
                IntentFilter("internal.play_update"),
                RECEIVER_NOT_EXPORTED
            )
            registerReceiver(
                receiverSquigglyUpdate,
                IntentFilter("internal.play_squiggly_update"),
                RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(receiverPause, IntentFilter("internal.play_pause"))
            registerReceiver(receiverPlay, IntentFilter("internal.play_start"))
            registerReceiver(receiverStop, IntentFilter("internal.play_stop"))
            registerReceiver(receiverSeek, IntentFilter("internal.play_seek"))
            registerReceiver(
                receiverUpdate,
                IntentFilter("internal.play_update")
            )
            registerReceiver(
                receiverSquigglyUpdate,
                IntentFilter("internal.play_squiggly_update")
            )
        }

        // Flatten the decors to fit the system windows.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Initialize the instance of supportFragmentManager.
        customFragmentManager = supportFragmentManager

        // Initialize view models.
        libraryViewModel = ViewModelProvider(this)[LibraryViewModel::class.java]
        playlistViewModel = ViewModelProvider(this)[PlaylistViewModel::class.java]
        controllerViewModel = ViewModelProvider(this)[ControllerViewModel::class.java]

        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                if (libraryViewModel.libraryHistoryList.isEmpty()) {
                    libraryViewModel.libraryHistoryList =
                            historyDao.getAllItems().map { it.value }.toMutableList()
                }
                if (playlistViewModel.playlistList.isEmpty()) {
                    playlistViewModel.playlistList.addAll(playlistDao.getAllPlaylists())
                }

                // TODO: Improve this guard implant
                isDBSafe = true
            }
        }

        // Open external audio files.
        val intent = intent
        val externalAudioUri: Uri? = intent.data
        externalAudioUri?.let {
            thisSong()
        }

        // Initialize loading sequence.
        coroutineScope.launch {
            if (libraryViewModel.librarySongList.isEmpty()) {
                loadDataFromDisk()
            }

            if (!isForceLoadingEnabled) {
                withContext(Dispatchers.IO) {
                    libraryViewModel.librarySortedAlbumList = sortAlbumListByTrackNumber(
                        libraryViewModel.libraryAlbumList
                    )
                }
            }
        }

        // Inflate the view.
        setContentView(R.layout.activity_main)

        // Find the views.
        val bottomSheetNextButton = findViewById<MaterialButton>(R.id.bottom_sheet_next)
        val fullSheetBackButton = findViewById<MaterialButton>(R.id.sheet_extract_player)
        val fullSheetNextButton = findViewById<MaterialButton>(R.id.sheet_next_song)
        val fullSheetPrevButton = findViewById<MaterialButton>(R.id.sheet_previous_song)

        fragmentContainerView = findViewById(R.id.fragmentContainer)
        navigationView = findViewById(R.id.navigation_view)
        bottomPlayerPreview = findViewById(R.id.bottom_player)
        bottomSheetArtistAndAlbum = findViewById(R.id.bottom_sheet_artist_album)
        bottomSheetControlButton = findViewById(R.id.bottom_sheet_play)
        bottomSheetSongName = findViewById(R.id.bottom_sheet_song_name)
        fullSheetSongName = findViewById(R.id.sheet_song_name)
        fullSheetArtist = findViewById(R.id.sheet_author)
        fullSheetLoopButton = findViewById(R.id.sheet_loop)
        fullSheetShuffleButton = findViewById(R.id.sheet_random)
        fullSheetControlButton = findViewById(R.id.sheet_mid_button)
        fullSheetSlider = findViewById(R.id.sheet_slider)
        fullSheetSquigglyView = findViewById(R.id.squiggly)
        fullSheetSquigglyViewFrame = findViewById(R.id.squiggly_frame)
        fullSheetDuration = findViewById(R.id.sheet_end_time)
        fullSheetTimeStamp = findViewById(R.id.sheet_now_time)
        fullSheetTimerButton = findViewById(R.id.full_timer)
        playlistButton = findViewById(R.id.sheet_playlist)
        bottomFullSizePlayerPreview = findViewById(R.id.full_size_sheet_player)
        playerBottomSheetBehavior =
                BottomSheetBehavior.from(findViewById(R.id.standard_bottom_sheet))

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val params = window.attributes
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = params
        }

        checkIfSquigglyProgressBarEnabled()
        if (isSquigglyProgressBarEnabled) {
            trackSquigglyProgressBar()
        }

        // Initialize the animator. (Since we can't acquire fragmentContainer inside switchDrawer.)
        animator = ValueAnimator.ofFloat(0f, NAVIGATION_VIEW_WIDTH.px.toFloat())
        animator.addUpdateListener { animation ->
            fragmentContainerView.translationX = animation.animatedValue as Float
        }
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.duration = DRAWER_ANIMATION_DURATION

        animatorReverse = ValueAnimator.ofFloat(NAVIGATION_VIEW_WIDTH.px.toFloat(), 0f)
        animatorReverse.addUpdateListener { animation ->
            fragmentContainerView.translationX = animation.animatedValue as Float
        }
        animatorReverse.interpolator = AccelerateDecelerateInterpolator()
        animatorReverse.duration = DRAWER_ANIMATION_DURATION
        animatorReverse.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                Handler(Looper.getMainLooper()).post {
                    isDrawerOpen = false
                    navigationView?.visibility = GONE
                }
            }
        })

        // The behavior of the global sheet starts here.
        fullSheetTimerButton.isChecked = timer != null

        playerBottomSheetBehavior.isHideable = false

        val playlistBottomSheet = PlaylistBottomSheet()

        when (controllerViewModel.loopButtonStatus) {
            NOT_IN_LOOP -> {
                fullSheetLoopButton?.isChecked = false
                fullSheetLoopButton?.icon =
                        AppCompatResources.getDrawable(this, R.drawable.ic_repeat)
            }

            LOOP -> {
                fullSheetLoopButton?.isChecked = true
                fullSheetLoopButton?.icon =
                        AppCompatResources.getDrawable(this, R.drawable.ic_repeat)
            }

            LOOP_SINGLE -> {
                fullSheetLoopButton?.isChecked = true
                fullSheetLoopButton?.icon =
                        AppCompatResources.getDrawable(this, R.drawable.ic_repeat_one)
            }

            else -> throw IllegalStateException()
        }
        fullSheetLoopButton?.addOnCheckedChangeListener { _, _ ->

            /*
             * Status 0: Don't loop
             * Status 1: Loop
             * Status 2: Repeat single
             */
            when (controllerViewModel.loopButtonStatus) {
                NOT_IN_LOOP -> {
                    controllerViewModel.loopButtonStatus = LOOP
                    fullSheetLoopButton?.isChecked = true
                }

                LOOP -> {
                    controllerViewModel.loopButtonStatus = LOOP_SINGLE
                    fullSheetLoopButton?.isChecked = true
                    fullSheetLoopButton?.icon =
                            AppCompatResources.getDrawable(this, R.drawable.ic_repeat_one)
                }

                LOOP_SINGLE -> {
                    if (!isListShuffleEnabled) {
                        controllerViewModel.loopButtonStatus = LOOP_SINGLE
                        fullSheetLoopButton?.isChecked = true
                    } else {
                        controllerViewModel.loopButtonStatus = NOT_IN_LOOP
                        fullSheetLoopButton?.isChecked = false
                    }
                    fullSheetLoopButton?.icon =
                            AppCompatResources.getDrawable(this, R.drawable.ic_repeat)
                }

                else -> throw IllegalStateException()
            }
        }

        fullSheetShuffleButton?.isChecked = controllerViewModel.shuffleState

        fullSheetShuffleButton?.addOnCheckedChangeListener { _, isChecked ->
            if (!isListShuffleEnabled &&
                    playlistViewModel.playList.isNotEmpty()
            ) {
                fullSheetLoopButton?.isChecked = isChecked
                controllerViewModel.shuffleState = isChecked
            } else {
                val playlist = playlistViewModel.playList
                val originalPlaylist = playlistViewModel.originalPlaylist
                if (playlistViewModel.playList.isNotEmpty()) {
                    val currentSong = playlist[playlistViewModel.currentLocation]
                    if (originalPlaylist.isEmpty()) {
                        originalPlaylist.addAll(playlist)
                        playlist.shuffle()
                        playlist.remove(currentSong)
                        playlist.add(0, currentSong)
                        playlistViewModel.currentLocation = 0
                        broadcastMetaDataUpdate()
                    } else {
                        playlist.clear()
                        playlist.addAll(originalPlaylist)
                        playlistViewModel.currentLocation = playlist.indexOf(currentSong)
                        originalPlaylist.clear()

                        broadcastMetaDataUpdate()
                    }
                }
                controllerViewModel.shuffleState = isChecked
            }
        }

        bottomSheetControlButton.setOnClickListener {
            if (musicPlayer != null) {
                userChangedPlayerStatus()
            } else if (musicPlayer == null && playlistViewModel.playList.size != 0 && playlistViewModel.currentLocation != playlistViewModel.playList.size
            ) {
                thisSong()
            }
        }

        bottomPlayerPreview.setOnClickListener {
            if (playerBottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                playerBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                bottomFullSizePlayerPreview.visibility = VISIBLE
                bottomPlayerPreview.visibility = GONE
            }
        }

        bottomSheetNextButton.setOnClickListener {
            nextSong()
        }

        fullSheetPrevButton.setOnClickListener {
            prevSong()
        }

        fullSheetNextButton.setOnClickListener {
            nextSong()
        }

        fullSheetControlButton.setOnClickListener {
            if (musicPlayer != null) {
                userChangedPlayerStatus()
            } else if (musicPlayer == null && playlistViewModel.playList.size != 0 && playlistViewModel.currentLocation != playlistViewModel.playList.size
            ) {
                thisSong()
            }
        }

        fullSheetBackButton.setOnClickListener {
            playerBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

            ObjectAnimator.ofFloat(bottomFullSizePlayerPreview, "alpha", 1f, 0f)
                .setDuration(FULL_PLAYER_FADE_ANIMATION_DURATION)
                .start()

            bottomPlayerPreview.visibility = VISIBLE
        }

        findViewById<ImageView>(R.id.sheet_cover).setOnLongClickListener {
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
            val dialogId: TextInputEditText = rootView.findViewById(R.id.dialog_id)!!
            val dialogName: TextInputEditText = rootView.findViewById(R.id.dialog_song)!!
            val dialogArtist: TextInputEditText = rootView.findViewById(R.id.dialog_artist)!!
            val dialogAlbum: TextInputEditText = rootView.findViewById(R.id.dialog_album)!!
            val dialogDuration: TextInputEditText = rootView.findViewById(R.id.dialog_duration)!!
            val dialogPath: TextInputEditText = rootView.findViewById(R.id.dialog_path)!!

            if (playlistViewModel.playList.size > playlistViewModel.currentLocation) {
                val song = playlistViewModel.playList[playlistViewModel.currentLocation]
                dialogId.setText(song.id.toString())
                dialogPath.setText(song.path)
                dialogDuration.setText(song.duration.toString())
                if (song.title.isNotEmpty()) {
                    dialogName.setText(song.title)
                }
                if (song.artist.isNotEmpty()) {
                    dialogArtist.setText(song.artist)
                }
                if (song.album.isNotEmpty()) {
                    dialogAlbum.setText(song.album)
                }
            }

            true
        }

        playlistButton.setOnClickListener {
            if (!playlistBottomSheet.isAdded) {
                playlistBottomSheet.show(supportFragmentManager, PlaylistBottomSheet.TAG)
            }
        }

        if (!controllerViewModel.isBottomSheetOpen) {
            bottomFullSizePlayerPreview.alpha = 0f
        } else {
            bottomPlayerPreview.alpha = 0f
        }

        val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED && bottomPlayerPreview.isVisible) {
                    bottomFullSizePlayerPreview.visibility = GONE
                    controllerViewModel.isBottomSheetOpen = false
                } else if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                    bottomFullSizePlayerPreview.visibility = VISIBLE
                    bottomPlayerPreview.visibility = VISIBLE
                    if (isSquigglyProgressBarEnabled) {
                        trackSquigglyProgressBar()
                    }
                } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomPlayerPreview.visibility = GONE
                    controllerViewModel.isBottomSheetOpen = true
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                bottomPlayerPreview.alpha = 1 - (slideOffset * SHEET_OFFSET_MULTIPLE)
                bottomFullSizePlayerPreview.alpha = slideOffset
            }
        }

        playerBottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback)

        fullSheetTimerButton.setOnClickListener {
            if (timer != null) {
                fullSheetTimerButton.isChecked = true
            }
            if (musicPlayer != null) {
                val rootView = MaterialAlertDialogBuilder(
                    this,
                    com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
                )
                    .setTitle(getString(R.string.dialog_timer_title))
                    .setView(R.layout.alert_dialog_timer)
                    .setNeutralButton(getString(R.string.dialog_song_dismiss)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setOnDismissListener {
                        if (timer == null) {
                            fullSheetTimerButton.isChecked = false
                        }
                    }
                    .show()
                val rangeSlider = rootView.findViewById<Slider>(R.id.timer_slider)!!
                if (timer != null) {
                    rangeSlider.value = timerValue
                }
                rangeSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                    override fun onStartTrackingTouch(slider: Slider) {
                        // Keep this empty
                    }

                    override fun onStopTrackingTouch(slider: Slider) {
                        if (rangeSlider.value != 0f) {
                            if (timerValue != 0f) {
                                timer?.cancel()
                                timer = null
                                timerValue = 0f
                            }
                            timerValue = rangeSlider.value
                            timer = object : CountDownTimer((rangeSlider.value * 3600 * 1000).toLong(), 1000) {
                                override fun onTick(millisUntilFinished: Long) {
                                }

                                override fun onFinish() {
                                    musicPlayer?.pause()
                                    broadcastPlayPaused()
                                    broadcastMetaDataUpdate()
                                    timerValue = 0f
                                }
                            }
                            (timer as CountDownTimer).start()
                        } else {
                            timer?.cancel()
                            timer = null
                            timerValue = 0f
                        }
                    }
                })
            }
        }
        // The behavior of the global sheet ends here.

        // Slider behavior starts here.
        // Register handler for updating slider.
        handler = Handler(Looper.getMainLooper())

        // When the slider is dragged by user, mark it
        // to use this state later.
        val touchListener: Slider.OnSliderTouchListener = object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                isUserTracking = true
            }

            override fun onStopTrackingTouch(slider: Slider) {
                // This value is multiplied by 1000 is because
                // when the number is too big (like when toValue
                // used the duration directly) we might encounter
                // some performance problem.
                musicPlayer?.seekTo((slider.value * PLAYER_SLIDER_VALUE_MULTIPLE).toInt())

                broadcastSliderSeek()

                isUserTracking = false
            }
        }

        fullSheetSlider.addOnSliderTouchListener(touchListener)

        fullSheetSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                fullSheetTimeStamp.text =
                        convertDurationToTimeStamp((value * PLAYER_SLIDER_VALUE_MULTIPLE).toInt().toString())
            }
            if (isSquigglyProgressBarEnabled) {
                trackSquigglyProgressBar()
            }
        }
        // Slider behavior ends here.

        // Set the drawer's behavior.
        navigationView?.setNavigationItemSelectedListener { menuItem ->
            if (menuItem.title == getString(R.string.navigation_home)) {
                val homeFragment = HomeFragment()
                customFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, homeFragment)
                    .commit()
                switchDrawer()
            }
            if (menuItem.title == getString(R.string.navigation_view_settings)) {
                val settingsFragment = SettingsFragment()
                customFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, settingsFragment)
                    .commit()
                switchDrawer()
            }
            if (menuItem.title == getString(R.string.navigation_view_all_song)) {
                val libraryFragment = LibraryFragment()
                customFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, libraryFragment)
                    .commit()
                switchDrawer()
            }
            true
        }

        // Check the permission status on Tiramisu.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_MEDIA_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Ask if was denied.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_MEDIA_AUDIO),
                    PERMISSION_REQUEST_CODE
                )
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Ask if was denied.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_CODE
                )
            }
        }

        // TODO: Make another pop up when denied to state why you need the permission.
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Check the requestCode.
        coroutineScope.launch {
            loadDataFromDisk()
        }
    }

    override fun onResume() {
        super.onResume()

        // Update bottom sheet's status.
        if (controllerViewModel.isBottomSheetOpen) {
            playerBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            bottomFullSizePlayerPreview.visibility = VISIBLE
            bottomPlayerPreview.visibility = GONE
        } else {
            playerBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            bottomFullSizePlayerPreview.visibility = GONE
            bottomPlayerPreview.visibility = VISIBLE
        }

        // Update the data if resumed.
        // They might be lost if don't do so.
        if (playlistViewModel.playList.size != 0) {
            updateMetadata()
            updateAlbumView(this.findViewById(R.id.global_bottom_sheet))
        }

        broadcastMetaDataUpdate()

        if (musicPlayer != null && !musicPlayer!!.isPlaying) {
            fullSheetSlider.isEnabled = true

            val valueTo = musicPlayer!!.duration.toFloat() / PLAYER_SLIDER_VALUE_MULTIPLE
            if (valueTo < fullSheetSlider.value) {
                fullSheetSlider.value = 0f
            }
            fullSheetSlider.valueTo = valueTo
            val addVar = musicPlayer!!.currentPosition.toFloat() / PLAYER_SLIDER_VALUE_MULTIPLE
            if (addVar <= fullSheetSlider.valueTo) {
                fullSheetSlider.value = addVar
            }
            fullSheetTimeStamp.text =
                    convertDurationToTimeStamp(musicPlayer!!.currentPosition.toString())
        }

        checkIfSquigglyProgressBarEnabled()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister everything.
        handler.removeCallbacks(sliderTask)
        unregisterReceiver(receiverPause)
        unregisterReceiver(receiverStop)
        unregisterReceiver(receiverPlay)
        unregisterReceiver(receiverSeek)
        unregisterReceiver(receiverUpdate)
        unregisterReceiver(receiverSquigglyUpdate)
        navigationView = null
        fullSheetLoopButton = null
        fullSheetShuffleButton = null
        isMainActivityActive = null
        val intent = Intent(this, SymphonicaPlayerService::class.java)
        stopService(intent)
    }

    /**
     * This is the SheetPlayReceiver.
     * It receives a broadcast from [receiverPlay] and involves
     * changes of various UI components including:
     * [bottomSheetSongName], [bottomSheetArtistAndAlbum],
     * [fullSheetSongName], [fullSheetArtist].
     * It also uses [updateAlbumView].
     */
    inner class SheetPlayReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (musicPlayer != null && playlistViewModel.currentLocation !=
                    playlistViewModel.playList.size
            ) {
                bottomSheetSongName.text =
                        playlistViewModel.playList[playlistViewModel.currentLocation].title
                bottomSheetArtistAndAlbum.text =
                        getString(
                            R.string.playlist_metadata_information,
                            playlistViewModel.playList[playlistViewModel.currentLocation].artist,
                            playlistViewModel.playList[playlistViewModel.currentLocation].album
                        )
                fullSheetSongName.text =
                        playlistViewModel.playList[playlistViewModel.currentLocation].title
                fullSheetArtist.text =
                        playlistViewModel.playList[playlistViewModel.currentLocation].artist
                fullSheetDuration.text =
                        convertDurationToTimeStamp(
                            playlistViewModel.playList[playlistViewModel.currentLocation].duration.toString()
                        )

                bottomSheetControlButton.icon =
                        ContextCompat.getDrawable(SymphonicaApplication.context, R.drawable.ic_pause)
                fullSheetControlButton.icon = ContextCompat.getDrawable(SymphonicaApplication.context, R.drawable.ic_pause)

                updateAlbumView(this@MainActivity.findViewById(R.id.global_bottom_sheet))
                setPlaybackState(OPERATION_PLAY)
                handler.postDelayed(sliderTask, SLIDER_UPDATE_INTERVAL)
            }
            checkIfSquigglyProgressBarEnabled()
        }
    }

    /**
     * This is the SheetStopReceiver.
     * It receives a broadcast from [receiverStop] and involves
     * changes of various UI components including:
     * [bottomSheetControlButton], [fullSheetControlButton],
     * [fullSheetSlider].
     */
    inner class SheetStopReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            bottomSheetControlButton.icon =
                    ContextCompat.getDrawable(SymphonicaApplication.context, R.drawable.ic_sheet_play)
            fullSheetControlButton.icon = ContextCompat.getDrawable(SymphonicaApplication.context, R.drawable.ic_refresh)
            fullSheetSlider.isEnabled = false
            fullSheetSlider.value = 0f
            fullSheetTimeStamp.text = convertDurationToTimeStamp("0")
            setPlaybackState(OPERATION_PAUSE)
            checkIfSquigglyProgressBarEnabled()
            if (musicPlayer != null) {
                updateMetadata()
            }
            timer?.cancel()
            timer = null
            updateAlbumView(findViewById(R.id.global_bottom_sheet))
            fullSheetTimerButton.isChecked = false
        }
    }

    /**
     * This is the SheetPauseReceiver.
     * It receives a broadcast from [receiverPause] and involves
     * changes of various UI components including:
     * [bottomSheetControlButton], [fullSheetControlButton].
     */
    inner class SheetPauseReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            bottomSheetControlButton.icon =
                    ContextCompat.getDrawable(SymphonicaApplication.context, R.drawable.ic_sheet_play)
            fullSheetControlButton.icon = ContextCompat.getDrawable(SymphonicaApplication.context, R.drawable.ic_sheet_play)
            setPlaybackState(OPERATION_PAUSE)
            if (musicPlayer != null) {
                updateMetadata()
            }
            if (timer == null) {
                fullSheetTimerButton.isChecked = false
            } else if (timerValue == 0f) {
                fullSheetTimerButton.isChecked = false
                timer!!.cancel()
                timer = null
            }
        }
    }

    /**
     * This is the SheetSeekReceiver
     * It receives a broadcast from [receiverSeek] and involves
     * changes of [setPlaybackState].
     */
    inner class SheetSeekReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            setPlaybackState(OPERATION_PLAY)
        }
    }

    /**
     * This is the SheetUpdateReceiver.
     * It receives a broadcast from [receiverUpdate] and involves
     * changes of various UI components including:
     * [bottomSheetSongName], [bottomSheetArtistAndAlbum],
     * [fullSheetSongName], [fullSheetArtist].
     * This receiver is used when resuming the activity.
     */
    inner class SheetUpdateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (musicPlayer != null && playlistViewModel.currentLocation < playlistViewModel.playList.size) {
                bottomSheetSongName.text =
                        playlistViewModel.playList[playlistViewModel.currentLocation].title
                bottomSheetArtistAndAlbum.text =
                        getString(
                            R.string.playlist_metadata_information,
                            playlistViewModel.playList[playlistViewModel.currentLocation].artist,
                            playlistViewModel.playList[playlistViewModel.currentLocation].album
                        )
                fullSheetSongName.text =
                        playlistViewModel.playList[playlistViewModel.currentLocation].title
                fullSheetArtist.text =
                        playlistViewModel.playList[playlistViewModel.currentLocation].artist
                fullSheetDuration.text =
                        convertDurationToTimeStamp(
                            playlistViewModel.playList[playlistViewModel.currentLocation].duration.toString()
                        )

                if (musicPlayer!!.isPlaying) {
                    bottomSheetControlButton.icon =
                            ContextCompat.getDrawable(
                                SymphonicaApplication.context,
                                R.drawable.ic_pause
                            )
                    fullSheetControlButton.icon = ContextCompat.getDrawable(SymphonicaApplication.context, R.drawable.ic_pause)
                }

                updateAlbumView(this@MainActivity.findViewById(R.id.global_bottom_sheet))

                handler.postDelayed(sliderTask, SLIDER_UPDATE_INTERVAL)
            } else if (playlistViewModel.currentLocation < playlistViewModel.playList.size) {
                bottomSheetSongName.text =
                        playlistViewModel.playList[playlistViewModel.currentLocation].title
                bottomSheetArtistAndAlbum.text =
                        getString(
                            R.string.playlist_metadata_information,
                            playlistViewModel.playList[playlistViewModel.currentLocation].artist,
                            playlistViewModel.playList[playlistViewModel.currentLocation].album
                        )
                fullSheetSongName.text =
                        playlistViewModel.playList[playlistViewModel.currentLocation].title
                fullSheetArtist.text =
                        playlistViewModel.playList[playlistViewModel.currentLocation].artist
                fullSheetDuration.text =
                        convertDurationToTimeStamp(
                            playlistViewModel.playList[playlistViewModel.currentLocation].duration.toString()
                        )
            }
            if (timer == null) {
                fullSheetTimerButton.isChecked = false
            }
        }
    }

    /**
     * This is the [SheetSquigglyReceiver]
     * It receives a broadcast from [receiverSquigglyUpdate].
     */
    inner class SheetSquigglyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            checkIfSquigglyProgressBarEnabled()
        }
    }


    private fun checkIfSquigglyProgressBarEnabled() {
        if (isSquigglyProgressBarEnabled && musicPlayer != null) {
            // When player is active.
            setSquigglyActive()
            if (libraryViewModel.sessionSongPlayed <= 1 && !fullSheetSlider.isEnabled &&
                !controllerViewModel.hasInitializedSquigglyView) {
                fullSheetSquigglyView.visibility = GONE
                controllerViewModel.hasInitializedSquigglyView = true
            }
        } else if (isSquigglyProgressBarEnabled && fullSheetSlider.value != 0f && musicPlayer == null) {
            // When stopped at last song in the list.
            setSquigglyInactive()
            trackSquigglyProgressBar()
        } else if (isSquigglyProgressBarEnabled && fullSheetSlider.value == 0f && musicPlayer == null) {
            // When no music is played and no progress is set.
            setSquigglyGone()
        } else if (isSquigglyProgressBarEnabled) {
            trackSquigglyProgressBar()
        }
        else {
            setSquigglyGone()
        }
    }

    private fun setSquigglyInactive() {
        fullSheetSquigglyView.visibility = VISIBLE
        fullSheetSlider.trackActiveTintList = ColorStateList.valueOf(
            resources.getColor(android.R.color.transparent, theme)
        )
    }

    private fun setSquigglyActive() {
        fullSheetSquigglyView.visibility = VISIBLE
        fullSheetSquigglyView.paint.color = MaterialColors.getColor(
            fullSheetSquigglyView,
            com.google.android.material.R.attr.colorPrimary
        )
        fullSheetSlider.trackActiveTintList = ColorStateList.valueOf(
            resources.getColor(android.R.color.transparent, theme)
        )
    }

    private fun setSquigglyGone() {
        fullSheetSquigglyView.visibility = GONE
        fullSheetSlider.trackActiveTintList = ColorStateList.valueOf(
            MaterialColors.getColor(
                fullSheetSlider,
                com.google.android.material.R.attr.colorPrimary
            )
        )
    }

    private fun trackSquigglyProgressBar() {
        val params = fullSheetSquigglyView.layoutParams as? ViewGroup.MarginLayoutParams
        params?.let {
            val marginVal = (fullSheetSquigglyViewFrame.width *
                    (fullSheetSlider.valueTo - fullSheetSlider.value) / fullSheetSlider.valueTo).toInt()
            if (marginVal != 0) {
                it.marginEnd = marginVal
                controllerViewModel.squigglyViewMargin = marginVal
            } else {
                it.marginEnd = controllerViewModel.squigglyViewMargin
            }
            if (controllerViewModel.squigglyViewMargin != 0) {
                fullSheetSquigglyView.visibility = VISIBLE
            }
            fullSheetSquigglyView.layoutParams = it
        }
    }

    companion object {
        var currentMusicDrawable: Bitmap? = null

        var isMainActivityActive: Boolean? = null
        var isDBSafe = false
        private val historyDatabase = Room.databaseBuilder(
            context, HistoryDatabase::class.java,
            "history_db"
        ).build()
        val playlistDatabase = Room.databaseBuilder(
            context, PlaylistDatabase::class.java,
            "playlist_db"
        ).build()
        val historyDao = historyDatabase.historyDao()
        val playlistDao = playlistDatabase.playlistDao()

        // These are the views inside MainActivity.
        // They're in companion area because some of the companion
        // functions required them or some outer class needs them.
        private var navigationView: NavigationView? = null
        var fullSheetLoopButton: MaterialButton? = null
        var fullSheetShuffleButton: MaterialButton? = null

        // This is drawer needed in companion functions to decide
        // whether the drawer is open.
        var isDrawerOpen = false

        // Below is the custom variables that can be changed throughout
        // the settings.
        var isColorfulButtonEnabled: Boolean = false
        var isGlideCacheEnabled: Boolean = false
        var isForceLoadingEnabled: Boolean = false
        var isForceDarkModeEnabled: Boolean = false
        var isLibraryShuffleButtonEnabled: Boolean = false
        var isListShuffleEnabled: Boolean = true
        var isEasterEggDiscovered: Boolean = false
        var isAkaneVisible: Boolean = false
        var isSquigglyProgressBarEnabled: Boolean = false

        // This is the core of Symphonica, the music player.
        var musicPlayer: MediaPlayer? = null

        // This is used to check if the miniPlayer is running.
        var isMiniPlayerRunning = false

        // This is the handler used to handle the slide task.
        private lateinit var handler: Handler
        lateinit var customFragmentManager: FragmentManager

        // These are the view models used across the app.
        lateinit var libraryViewModel: LibraryViewModel
        lateinit var playlistViewModel: PlaylistViewModel
        lateinit var controllerViewModel: ControllerViewModel

        // This is the default disk cache behavior.
        lateinit var diskCacheStrategyCustom: DiskCacheStrategy

        // This is the animator needed in companion functions.
        lateinit var animator: ValueAnimator
        lateinit var animatorReverse: ValueAnimator

        /**
         * This is the function used to switch the status of
         * [navigationView].
         */
        fun switchDrawer() {
            if (!isDrawerOpen) {
                isDrawerOpen = true
                navigationView?.visibility = VISIBLE
                animator.start()
            } else {
                animatorReverse.start()
            }
        }

        /**
         * This function is used to switch selected item for
         * navigation view across the project.
         * [navigationView]
         *
         * @param index
         * @throws IllegalArgumentException
         */
        fun switchNavigationViewIndex(index: Int) {
            when (index) {
                OPTION_HOME -> navigationView?.post {
                    navigationView?.menu?.let {
                        navigationView?.setCheckedItem(
                            it.findItem(
                                R.id.library_navigation
                            )
                        )
                    }
                }

                OPTION_LIBRARY -> navigationView?.post {
                    navigationView?.menu?.let {
                        navigationView?.setCheckedItem(
                            it.findItem(
                                R.id.settings_navigation
                            )
                        )
                    }
                }

                OPTION_SETTINGS -> navigationView?.post {
                    navigationView?.menu?.let {
                        navigationView?.setCheckedItem(
                            it.findItem(
                                R.id.settings_home
                            )
                        )
                    }
                }

                else -> throw IllegalArgumentException()
            }
        }
    }
}
