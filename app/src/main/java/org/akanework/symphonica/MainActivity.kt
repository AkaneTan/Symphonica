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

@file:Suppress("KotlinConstantConditions")

package org.akanework.symphonica

import android.animation.ObjectAnimator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.symphonica.SymphonicaApplication.Companion.context
import org.akanework.symphonica.logic.data.loadDataFromDisk
import org.akanework.symphonica.logic.service.SymphonicaPlayerService.Companion.setPlaybackState
import org.akanework.symphonica.logic.service.SymphonicaPlayerService.Companion.updateMetadata
import org.akanework.symphonica.logic.util.broadcastMetaDataUpdate
import org.akanework.symphonica.logic.util.broadcastSliderSeek
import org.akanework.symphonica.logic.util.changePlayerStatus
import org.akanework.symphonica.logic.util.convertDurationToTimeStamp
import org.akanework.symphonica.logic.util.nextSong
import org.akanework.symphonica.logic.util.prevSong
import org.akanework.symphonica.logic.util.sortAlbumListByTrackNumber
import org.akanework.symphonica.logic.util.thisSong
import org.akanework.symphonica.ui.component.PlaylistBottomSheet
import org.akanework.symphonica.ui.fragment.HomeFragment
import org.akanework.symphonica.ui.fragment.LibraryFragment
import org.akanework.symphonica.ui.fragment.SettingsFragment
import org.akanework.symphonica.ui.viewmodel.BooleanViewModel
import org.akanework.symphonica.ui.viewmodel.LibraryViewModel
import org.akanework.symphonica.ui.viewmodel.PlaylistViewModel

/**
 * [MainActivity] is the heart of Symphonica.
 * Google said, let there be fragments, so akane answered:
 * "There would be fragments."
 */
class MainActivity : AppCompatActivity() {

    private val permissionRequestCode = 123

    // Below are the variables used across MainActivity
    // to update themselves.

    private lateinit var bottomSheetSongName: TextView
    private lateinit var bottomSheetArtistAndAlbum: TextView
    private lateinit var fullSheetSongName: TextView
    private lateinit var fullSheetArtist: TextView
    private lateinit var fullSheetAlbum: TextView
    private lateinit var fullSheetDuration: TextView
    private lateinit var fullSheetLocation: TextView
    private lateinit var fullSheetTimeStamp: TextView

    private lateinit var bottomSheetControlButton: MaterialButton
    private lateinit var fullSheetControlButton: FloatingActionButton

    private lateinit var fullSheetSlider: Slider

    private lateinit var receiverPlay: SheetPlayReceiver
    private lateinit var receiverStop: SheetStopReceiver
    private lateinit var receiverPause: SheetPauseReceiver
    private lateinit var receiverSeek: SheetSeekReceiver
    private lateinit var receiverUpdate: SheetUpdateReceiver

    private lateinit var playlistButton: MaterialButton

    private lateinit var fragmentContainerView: FragmentContainerView

    private lateinit var playerBottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var bottomFullSizePlayerPreview: LinearLayout
    private lateinit var bottomPlayerPreview: FrameLayout

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

                // about the "/ 1000 + 0.2f", check line 396.
                fullSheetSlider.valueTo = musicPlayer!!.duration.toFloat() / 1000

                if (!isUserTracking && musicPlayer!!.currentPosition.toFloat() / 1000 <= fullSheetSlider.valueTo) {
                    fullSheetSlider.value = musicPlayer!!.currentPosition.toFloat() / 1000

                    fullSheetTimeStamp.text =
                        convertDurationToTimeStamp(musicPlayer!!.currentPosition.toString())
                }

                // Update it per 200ms.
                handler.postDelayed(this, 500)
            }
        }
    }

    companion object {

        // This is the handler used to handle the slide task.
        private lateinit var handler: Handler

        // These variables are used inside SymphonicaPlayerService.
        // They are used to manage the MediaControl notifications.
        lateinit var managerSymphonica: NotificationManager
        lateinit var channelSymphonica: NotificationChannel

        // These are the views inside MainActivity.
        // They're in companion area because some of the companion
        // functions required them or some outer class needs them.
        private var navigationView: NavigationView? = null
        var fullSheetLoopButton: MaterialButton? = null
        var fullSheetShuffleButton: MaterialButton? = null

        lateinit var customFragmentManager: FragmentManager

        // These are the view models used across the app.
        lateinit var libraryViewModel: LibraryViewModel
        lateinit var playlistViewModel: PlaylistViewModel
        lateinit var booleanViewModel: BooleanViewModel

        // This is drawer needed in companion functions to decide
        // whether the drawer is open.
        private var isDrawerOpen = false

        // This is the default disk cache behavior.
        lateinit var diskCacheStrategyCustom: DiskCacheStrategy

        // Below is the custom variables that can be changed throughout
        // the settings.
        var isGlideCacheEnabled: Boolean = false
        var isForceLoadingEnabled: Boolean = false
        var isForceDarkModeEnabled: Boolean = false
        var isListShuffleEnabled: Boolean = true
        var isEasterEggDiscovered: Boolean = false
        var isAkaneVisible: Boolean = false

        // This is the core of Symphonica, the music player.
        var musicPlayer: MediaPlayer? = null

        // This is the animator needed in companion functions.
        lateinit var animator: ObjectAnimator

        // This is used to check if the miniPlayer is running.
        var isMiniPlayerRunning = false

        /**
         * This is the function used to switch the status of
         * [navigationView].
         */
        fun switchDrawer() {
            if (!isDrawerOpen) {
                isDrawerOpen = true
                navigationView?.visibility = VISIBLE
                animator.setDuration(400)
                animator.start()
            } else {
                isDrawerOpen = false
                animator.reverse()

                // Make the navigationView disappear delayed.
                val handler = Handler(Looper.getMainLooper())
                val runnable = Runnable {
                    navigationView?.visibility = GONE
                }
                handler.postDelayed(runnable, 400)
            }
        }

        /**
         * This function is used to switch selected item for
         * navigation view across the project.
         * [navigationView]
         */
        fun switchNavigationViewIndex(index: Int) {
            when (index) {
                0 -> {
                    navigationView?.post {
                        navigationView?.menu?.let {
                            navigationView?.setCheckedItem(
                                it.findItem(
                                    R.id.library_navigation
                                )
                            )
                        }
                    }
                }

                1 -> {
                    navigationView?.post {
                        navigationView?.menu?.let {
                            navigationView?.setCheckedItem(
                                it.findItem(
                                    R.id.settings_navigation
                                )
                            )
                        }
                    }
                }

                2 -> {
                    navigationView?.post {
                        navigationView?.menu?.let {
                            navigationView?.setCheckedItem(
                                it.findItem(
                                    R.id.settings_home
                                )
                            )
                        }
                    }
                }

                else -> {
                    throw IllegalArgumentException()
                }
            }
        }

    }

    private fun updateAlbumView(view: View) {
        val sheetAlbumCover: ImageView? = view.findViewById(R.id.sheet_album_cover)
        val fullSheetCover: ImageView? = view.findViewById(R.id.sheet_cover)
        sheetAlbumCover?.setImageResource(R.drawable.ic_song_default_cover)
        if (sheetAlbumCover != null) {
            Glide.with(context)
                .load(playlistViewModel.playList[playlistViewModel.currentLocation].imgUri)
                .diskCacheStrategy(diskCacheStrategyCustom)
                .into(sheetAlbumCover)
        }
        fullSheetCover?.setImageResource(R.drawable.ic_song_default_cover)
        if (fullSheetCover != null) {
            Glide.with(context)
                .load(playlistViewModel.playList[playlistViewModel.currentLocation].imgUri)
                .diskCacheStrategy(diskCacheStrategyCustom)
                .into(fullSheetCover)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get customized options.
        val prefs = getSharedPreferences("data", Context.MODE_PRIVATE)
        isGlideCacheEnabled = prefs.getBoolean("isGlideCacheEnabled", false)
        isForceLoadingEnabled = prefs.getBoolean("isForceLoadingEnabled", false)
        isForceDarkModeEnabled = prefs.getBoolean("isForceDarkModeEnabled", false)
        isListShuffleEnabled = prefs.getBoolean("isListShuffleEnabled", true)
        isEasterEggDiscovered = prefs.getBoolean("isEasterEggDiscovered", false)
        isAkaneVisible = prefs.getBoolean("isAkaneVisible", false)

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

        registerReceiver(receiverPause, IntentFilter("internal.play_pause"), RECEIVER_NOT_EXPORTED)
        registerReceiver(receiverPlay, IntentFilter("internal.play_start"), RECEIVER_NOT_EXPORTED)
        registerReceiver(receiverStop, IntentFilter("internal.play_stop"), RECEIVER_NOT_EXPORTED)
        registerReceiver(receiverSeek, IntentFilter("internal.play_seek"), RECEIVER_NOT_EXPORTED)
        registerReceiver(
            receiverUpdate,
            IntentFilter("internal.play_update"),
            RECEIVER_NOT_EXPORTED
        )

        // Flatten the decors to fit the system windows.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Initialize the instance of supportFragmentManager.
        customFragmentManager = supportFragmentManager

        // Initialize view models.
        libraryViewModel = ViewModelProvider(this)[LibraryViewModel::class.java]
        playlistViewModel = ViewModelProvider(this)[PlaylistViewModel::class.java]
        booleanViewModel = ViewModelProvider(this)[BooleanViewModel::class.java]

        // Open external audio files.
        val intent = intent
        val externalAudioUri: Uri? = intent.data
        if (externalAudioUri != null) {
            thisSong()
        }

        // Initialize loading sequence.
        coroutineScope.launch {
            loadDataFromDisk()

            if (!isForceLoadingEnabled) {
                // Look, listen.
                // I know what you're thinking.
                // Let me explain. Go to sortAlbumListByTrackNumber's define page.
                withContext(Dispatchers.IO) {
                    libraryViewModel.librarySortedAlbumList = sortAlbumListByTrackNumber(
                        libraryViewModel.libraryAlbumList
                    )
                }
            }
        }

        // Initialize notification service.
        managerSymphonica = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        channelSymphonica = NotificationChannel(
            "channel_symphonica",
            "Symphonica",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        managerSymphonica.createNotificationChannel(channelSymphonica)

        // Inflate the view.
        setContentView(R.layout.activity_main)

        if (isAkaneVisible) {
            findViewById<ImageView>(R.id.akane).visibility = VISIBLE
        }

        // Find the views.
        bottomPlayerPreview = findViewById(R.id.bottom_player)
        val bottomSheetNextButton = findViewById<MaterialButton>(R.id.bottom_sheet_next)
        val fullSheetBackButton = findViewById<MaterialButton>(R.id.sheet_extract_player)
        val fullSheetNextButton = findViewById<MaterialButton>(R.id.sheet_next_song)
        val fullSheetPrevButton = findViewById<MaterialButton>(R.id.sheet_previous_song)
        val fullSheetSongInfo = findViewById<MaterialButton>(R.id.sheet_song_info)

        fragmentContainerView = findViewById(R.id.fragmentContainer)
        navigationView = findViewById(R.id.navigation_view)
        bottomSheetSongName = findViewById(R.id.bottom_sheet_song_name)
        bottomSheetArtistAndAlbum = findViewById(R.id.bottom_sheet_artist_album)
        fullSheetSongName = findViewById(R.id.sheet_song_name)
        fullSheetArtist = findViewById(R.id.sheet_author)
        fullSheetAlbum = findViewById(R.id.sheet_album)
        fullSheetLoopButton = findViewById(R.id.sheet_loop)
        fullSheetShuffleButton = findViewById(R.id.sheet_random)
        fullSheetLocation = findViewById(R.id.sheet_song_location)
        bottomSheetControlButton = findViewById(R.id.bottom_sheet_play)
        fullSheetControlButton = findViewById(R.id.sheet_mid_button)
        fullSheetSlider = findViewById(R.id.sheet_slider)
        fullSheetDuration = findViewById(R.id.sheet_end_time)
        fullSheetTimeStamp = findViewById(R.id.sheet_now_time)
        playlistButton = findViewById(R.id.sheet_playlist)
        bottomFullSizePlayerPreview = findViewById(R.id.full_size_sheet_player)
        playerBottomSheetBehavior =
            BottomSheetBehavior.from(findViewById(R.id.standard_bottom_sheet))

        // Initialize the animator. (Since we can't acquire fragmentContainer inside switchDrawer.)
        animator = ObjectAnimator.ofFloat(fragmentContainerView, "translationX", 0f, 600f)

        // The behavior of the global sheet starts here.
        playerBottomSheetBehavior.isHideable = false

        val playlistBottomSheet = PlaylistBottomSheet()

        when (booleanViewModel.loopButtonStatus) {
            0 -> {
                fullSheetLoopButton?.isChecked = false
                fullSheetLoopButton?.icon =
                    AppCompatResources.getDrawable(this, R.drawable.ic_repeat)
            }

            1 -> {
                fullSheetLoopButton?.isChecked = true
                fullSheetLoopButton?.icon =
                    AppCompatResources.getDrawable(this, R.drawable.ic_repeat)
            }

            2 -> {
                fullSheetLoopButton?.isChecked = true
                fullSheetLoopButton?.icon =
                    AppCompatResources.getDrawable(this, R.drawable.ic_repeat_one)
            }

            else -> {
                throw IllegalStateException()
            }
        }
        fullSheetLoopButton?.addOnCheckedChangeListener { _, _ ->

            /**
             * Status 0: Don't loop
             * Status 1: Loop
             * Status 2: Repeat single
             */
            when (booleanViewModel.loopButtonStatus) {
                0 -> {
                    booleanViewModel.loopButtonStatus = 1
                    fullSheetLoopButton?.isChecked = true
                    if (!isListShuffleEnabled) {
                        fullSheetLoopButton?.isChecked = true
                    }
                }

                1 -> {
                    booleanViewModel.loopButtonStatus = 2
                    fullSheetLoopButton?.isChecked = true
                    fullSheetLoopButton?.icon =
                        AppCompatResources.getDrawable(this, R.drawable.ic_repeat_one)
                }

                2 -> {
                    booleanViewModel.loopButtonStatus = 0
                    fullSheetLoopButton?.isChecked = false
                    fullSheetLoopButton?.icon =
                        AppCompatResources.getDrawable(this, R.drawable.ic_repeat)
                }

                else -> {
                    throw IllegalStateException()
                }
            }
        }

        fullSheetShuffleButton?.isChecked = booleanViewModel.shuffleState

        fullSheetShuffleButton?.addOnCheckedChangeListener { _, isChecked ->
            if (!isListShuffleEnabled &&
                playlistViewModel.playList.isNotEmpty()
            ) {
                fullSheetLoopButton?.isChecked = isChecked
                booleanViewModel.shuffleState = isChecked
            } else if (playlistViewModel.playList.isNotEmpty()) {
                val playlist = playlistViewModel.playList
                val originalPlaylist = playlistViewModel.originalPlaylist
                val currentSong = playlist[playlistViewModel.currentLocation]

                if (playlist.isNotEmpty() && originalPlaylist.isEmpty()) {
                    originalPlaylist.addAll(playlist)
                    playlist.shuffle()
                    playlist.remove(currentSong)
                    playlist.add(0, currentSong)
                    playlistViewModel.currentLocation = 0
                    broadcastMetaDataUpdate()

                } else if (playlist.isNotEmpty()) {
                    playlist.clear()
                    playlist.addAll(originalPlaylist)
                    playlistViewModel.currentLocation = playlist.indexOf(currentSong)
                    originalPlaylist.clear()

                    broadcastMetaDataUpdate()
                }
                booleanViewModel.shuffleState = isChecked
            } else {
                fullSheetShuffleButton?.isChecked = false
                booleanViewModel.shuffleState = false
            }
        }


        bottomSheetControlButton.setOnClickListener {
            if (musicPlayer != null) {
                changePlayerStatus()
            } else if (musicPlayer == null && playlistViewModel.playList.size != 0
                && playlistViewModel.currentLocation != playlistViewModel.playList.size
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
                changePlayerStatus()
            } else if (musicPlayer == null && playlistViewModel.playList.size != 0
                && playlistViewModel.currentLocation != playlistViewModel.playList.size
            ) {
                thisSong()
            }
        }

        fullSheetBackButton.setOnClickListener {
            playerBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

            ObjectAnimator.ofFloat(bottomFullSizePlayerPreview, "alpha", 1f, 0f)
                .setDuration(200)
                .start()

            bottomPlayerPreview.visibility = VISIBLE
        }

        fullSheetSongInfo.setOnClickListener {
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
            val dialogArtist: TextInputEditText = rootView.findViewById(R.id.dialog_artist)!!
            val dialogAlbum: TextInputEditText = rootView.findViewById(R.id.dialog_album)!!
            val dialogDuration: TextInputEditText = rootView.findViewById(R.id.dialog_duration)!!
            val dialogPath: TextInputEditText = rootView.findViewById(R.id.dialog_path)!!

            if (playlistViewModel.playList.size > playlistViewModel.currentLocation) {
                val song = playlistViewModel.playList[playlistViewModel.currentLocation]
                dialogID.setText(song.id.toString())
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
        }

        playlistButton.setOnClickListener {
            if (!playlistBottomSheet.isAdded) {
                playlistBottomSheet.show(supportFragmentManager, PlaylistBottomSheet.TAG)
            }
        }

        val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED && bottomPlayerPreview.isVisible) {
                    bottomFullSizePlayerPreview.visibility = GONE
                    booleanViewModel.isBottomSheetOpen = false
                } else if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                    if (booleanViewModel.isBottomSheetOpen) {
                        bottomPlayerPreview.alpha = 0f
                    }
                    bottomFullSizePlayerPreview.visibility = VISIBLE
                    bottomPlayerPreview.visibility = VISIBLE
                } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomPlayerPreview.visibility = GONE
                    booleanViewModel.isBottomSheetOpen = true
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                bottomPlayerPreview.alpha = 1 - (slideOffset * 2f)
                bottomFullSizePlayerPreview.alpha = slideOffset
            }
        }

        playerBottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback)
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
                musicPlayer?.seekTo((slider.value * 1000).toInt())

                broadcastSliderSeek()

                isUserTracking = false
            }
        }

        fullSheetSlider.addOnSliderTouchListener(touchListener)

        fullSheetSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) fullSheetTimeStamp.text =
                convertDurationToTimeStamp((value * 1000).toInt().toString())
        }
        // Slider behavior ends here.

        // Set the drawer's behavior.
        navigationView?.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.title) {
                getString(R.string.navigation_home) -> {
                    val homeFragment = HomeFragment()
                    customFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, homeFragment)
                        .commit()
                }
                getString(R.string.navigation_view_settings) -> {
                    customFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    val settingsFragment = SettingsFragment()
                    customFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, settingsFragment)
                        .addToBackStack("SETTINGS")
                        .commit()
                }
                getString(R.string.navigation_view_all_song) -> {
                    customFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    val libraryFragment = LibraryFragment()
                    customFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, libraryFragment)
                        .addToBackStack("LIBRARY")
                        .commit()
                }
            }
            switchDrawer()
            true
        }


        // Check the permission status.
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_MEDIA_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Ask if was denied.
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.READ_MEDIA_AUDIO),
                permissionRequestCode
            )
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
        if (booleanViewModel.isBottomSheetOpen) {
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
            fullSheetSlider.valueTo = musicPlayer!!.duration.toFloat() / 1000
            fullSheetSlider.value = musicPlayer!!.currentPosition.toFloat() / 1000
            fullSheetTimeStamp.text =
                convertDurationToTimeStamp(musicPlayer!!.currentPosition.toString())
        }
    }

    override fun onDestroy() {
        // Unregister everything.
        handler.removeCallbacks(sliderTask)
        unregisterReceiver(receiverPause)
        unregisterReceiver(receiverStop)
        unregisterReceiver(receiverPlay)
        unregisterReceiver(receiverSeek)
        unregisterReceiver(receiverUpdate)
        navigationView = null
        fullSheetLoopButton = null
        fullSheetShuffleButton = null
        super.onDestroy()
    }

    /**
     * This is the SheetPlayReceiver.
     * It receives a broadcast from [receiverPlay] and involves
     * changes of various UI components including:
     * [bottomSheetSongName], [bottomSheetArtistAndAlbum],
     * [fullSheetSongName], [fullSheetAlbum], [fullSheetArtist],
     * [fullSheetLocation].
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
                fullSheetAlbum.text =
                    playlistViewModel.playList[playlistViewModel.currentLocation].album
                fullSheetArtist.text =
                    playlistViewModel.playList[playlistViewModel.currentLocation].artist
                fullSheetDuration.text =
                    convertDurationToTimeStamp(
                        playlistViewModel.playList[playlistViewModel.currentLocation].duration.toString()
                    )
                // If you don't use a round bracket here the ViewModel would die from +1s.
                fullSheetLocation.text =
                    getString(
                        R.string.full_sheet_playlist_location,
                        ((playlistViewModel.currentLocation) + 1).toString(),
                        playlistViewModel.playList.size.toString()
                    )

                bottomSheetControlButton.icon =
                    ContextCompat.getDrawable(SymphonicaApplication.context, R.drawable.ic_pause)
                fullSheetControlButton.setImageResource(R.drawable.ic_pause)

                updateAlbumView(this@MainActivity.findViewById(R.id.global_bottom_sheet))
                setPlaybackState(0)
                handler.postDelayed(sliderTask, 500)
            }
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
            fullSheetControlButton.setImageResource(R.drawable.ic_sheet_play)
            fullSheetSlider.isEnabled = false
            setPlaybackState(1)
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
            fullSheetControlButton.setImageResource(R.drawable.ic_sheet_play)
            setPlaybackState(1)
        }

    }

    /**
     * This is the SheetSeekReceiver
     * It receives a broadcast from [receiverSeek] and involves
     * changes of [setPlaybackState].
     */
    inner class SheetSeekReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            setPlaybackState(0)
        }
    }

    /**
     * This is the SheetUpdateReceiver.
     * It receives a broadcast from [receiverUpdate] and involves
     * changes of various UI components including:
     * [bottomSheetSongName], [bottomSheetArtistAndAlbum],
     * [fullSheetSongName], [fullSheetAlbum], [fullSheetArtist],
     * [fullSheetLocation].
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
                fullSheetAlbum.text =
                    playlistViewModel.playList[playlistViewModel.currentLocation].album
                fullSheetArtist.text =
                    playlistViewModel.playList[playlistViewModel.currentLocation].artist
                fullSheetDuration.text =
                    convertDurationToTimeStamp(
                        playlistViewModel.playList[playlistViewModel.currentLocation].duration.toString()
                    )
                // If you don't use a round bracket here the ViewModel would die from +1s.
                fullSheetLocation.text =
                    getString(
                        R.string.full_sheet_playlist_location,
                        ((playlistViewModel.currentLocation) + 1).toString(),
                        playlistViewModel.playList.size.toString()
                    )

                if (musicPlayer!!.isPlaying) {
                    bottomSheetControlButton.icon =
                        ContextCompat.getDrawable(
                            SymphonicaApplication.context,
                            R.drawable.ic_pause
                        )
                    fullSheetControlButton.setImageResource(R.drawable.ic_pause)
                }

                updateAlbumView(this@MainActivity.findViewById(R.id.global_bottom_sheet))

                handler.postDelayed(sliderTask, 500)
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
                fullSheetAlbum.text =
                    playlistViewModel.playList[playlistViewModel.currentLocation].album
                fullSheetArtist.text =
                    playlistViewModel.playList[playlistViewModel.currentLocation].artist
                fullSheetDuration.text =
                    convertDurationToTimeStamp(
                        playlistViewModel.playList[playlistViewModel.currentLocation].duration.toString()
                    )
                // If you don't use a round bracket here the ViewModel would die from +1s.
                fullSheetLocation.text =
                    getString(
                        R.string.full_sheet_playlist_location,
                        ((playlistViewModel.currentLocation) + 1).toString(),
                        playlistViewModel.playList.size.toString()
                    )
                updateAlbumView(this@MainActivity.findViewById(R.id.global_bottom_sheet))
                updateMetadata()
            }
        }
    }

}