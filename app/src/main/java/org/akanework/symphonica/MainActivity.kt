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

@file:Suppress("KotlinConstantConditions", "KotlinConstantConditions")

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
import org.akanework.symphonica.logic.data.Album
import org.akanework.symphonica.logic.data.Song
import org.akanework.symphonica.logic.data.loadDataFromDisk
import org.akanework.symphonica.logic.service.SymphonicaPlayerService.Companion.setPlaybackState
import org.akanework.symphonica.logic.service.SymphonicaPlayerService.Companion.updateMetadata
import org.akanework.symphonica.logic.util.changePlayer
import org.akanework.symphonica.logic.util.convertDurationToTimeStamp
import org.akanework.symphonica.logic.util.nextSong
import org.akanework.symphonica.logic.util.prevSong
import org.akanework.symphonica.logic.util.sortAlbumListByTrackNumber
import org.akanework.symphonica.logic.util.thisSong
import org.akanework.symphonica.ui.component.PlaylistBottomSheet
import org.akanework.symphonica.ui.fragment.LibraryFragment
import org.akanework.symphonica.ui.fragment.SettingsFragment
import org.akanework.symphonica.ui.viewmodel.BooleanViewModel
import org.akanework.symphonica.ui.viewmodel.LibraryViewModel
import org.akanework.symphonica.ui.viewmodel.PlaylistViewModel


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

    private lateinit var bottomSheetControlButton: MaterialButton
    private lateinit var fullSheetControlButton: FloatingActionButton

    private lateinit var fullSheetSlider: Slider

    private lateinit var receiverPlay: SheetPlayReceiver
    private lateinit var receiverStop: SheetStopReceiver
    private lateinit var receiverPause: SheetPauseReceiver
    private lateinit var receiverSeek: SheetSeekReceiver

    private lateinit var playlistButton: MaterialButton

    private lateinit var fragmentContainerView: FragmentContainerView

    private lateinit var playerBottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var bottomFullSizePlayerPreview: LinearLayout
    private lateinit var bottomPlayerPreview: FrameLayout

    // These are the variables needed throughout MainActivity.
    private var isUserTracking = false

    // This is the coroutineScope used across MainActivity.
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    companion object {

        // These are the global song/album list.
        // They should be updated from ListView when resumed.
        lateinit var songList: List<Song>
        lateinit var albumList: List<Album>

        // This is the handler used to handle the slide task.
        private lateinit var handler: Handler
        private lateinit var sliderTask: Runnable

        // These variables are used inside SymphonicaPlayerService.
        // They are used to manage the MediaControl notifications.
        lateinit var managerSymphonica: NotificationManager
        lateinit var channelSymphonica: NotificationChannel

        // These are the views inside MainActivity.
        // They're in companion area because some of the companion
        // functions required them or some outer class needs them.
        private lateinit var navigationView: NavigationView
        lateinit var fullSheetLoopButton: MaterialButton
        lateinit var fullSheetShuffleButton: MaterialButton

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
                navigationView.visibility = VISIBLE
                animator.setDuration(400)
                animator.start()
            } else {
                isDrawerOpen = false
                animator.reverse()

                // Make the navigationView disappear delayed.
                val handler = Handler(Looper.getMainLooper())
                val runnable = Runnable {
                    navigationView.visibility = GONE
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
                    navigationView.post {
                        navigationView.setCheckedItem(
                            navigationView.menu.findItem(
                                R.id.library_navigation
                            )
                        )
                    }
                }

                1 -> {
                    navigationView.post {
                        navigationView.setCheckedItem(
                            navigationView.menu.findItem(
                                R.id.settings_navigation
                            )
                        )
                    }
                }

                else -> {
                    throw IllegalArgumentException()
                }
            }
        }

    }

    /**
     * This is a packaged function to update Album ImageViews.
     * It gets the MainActivity's view from it's parameter and
     * use it to find the global sheet.
     */
    private fun updateAlbumView(view: View) {
        val sheetAlbumCover: ImageView = view.findViewById(R.id.sheet_album_cover)
        val fullSheetCover: ImageView = view.findViewById(R.id.sheet_cover)
        sheetAlbumCover.setImageResource(R.drawable.ic_album_default_cover)
        Glide.with(context)
            .load(playlistViewModel.playList[playlistViewModel.currentLocation].imgUri)
            .diskCacheStrategy(diskCacheStrategyCustom)
            .into(sheetAlbumCover)
        fullSheetCover.setImageResource(R.drawable.ic_album_default_cover)
        Glide.with(context)
            .load(playlistViewModel.playList[playlistViewModel.currentLocation].imgUri)
            .diskCacheStrategy(diskCacheStrategyCustom)
            .into(fullSheetCover)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get customized options.
        val prefs = getSharedPreferences("data", Context.MODE_PRIVATE)
        isGlideCacheEnabled = prefs.getBoolean("isGlideCacheEnabled", false)
        isForceLoadingEnabled = prefs.getBoolean("isForceLoadingEnabled", false)

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

        registerReceiver(receiverPause, IntentFilter("internal.play_pause"), RECEIVER_NOT_EXPORTED)
        registerReceiver(receiverPlay, IntentFilter("internal.play_start"), RECEIVER_NOT_EXPORTED)
        registerReceiver(receiverStop, IntentFilter("internal.play_stop"), RECEIVER_NOT_EXPORTED)
        registerReceiver(receiverSeek, IntentFilter("internal.play_seek"), RECEIVER_NOT_EXPORTED)

        // Flatten the decors to fit the system windows.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Initialize the instance of supportFragmentManager.
        customFragmentManager = supportFragmentManager

        // Initialize an empty list of song/album.
        songList = listOf()
        albumList = listOf()

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
                    libraryViewModel.librarySortedAlbumList = sortAlbumListByTrackNumber(albumList)
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

        // Find the views.
        bottomPlayerPreview = findViewById(R.id.bottom_player)
        val bottomSheetNextButton = findViewById<MaterialButton>(R.id.bottom_sheet_next)
        val fullSheetBackButton = findViewById<MaterialButton>(R.id.sheet_extract_player)
        val fullSheetNextButton = findViewById<MaterialButton>(R.id.sheet_next_song)
        val fullSheetPrevButton = findViewById<MaterialButton>(R.id.sheet_previous_song)
        val fullSheetTimeStamp = findViewById<TextView>(R.id.sheet_now_time)
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
        playlistButton = findViewById(R.id.sheet_playlist)
        bottomFullSizePlayerPreview = findViewById(R.id.full_size_sheet_player)
        playerBottomSheetBehavior =
            BottomSheetBehavior.from(findViewById(R.id.standard_bottom_sheet))

        // Initialize the animator. (Since we can't acquire fragmentContainer inside switchDrawer.)
        animator = ObjectAnimator.ofFloat(fragmentContainerView, "translationX", 0f, 600f)

        // The behavior of the global sheet starts here.
        playerBottomSheetBehavior.isHideable = false

        val playlistBottomSheet = PlaylistBottomSheet()

        // So when we open the shuffle button, loop button
        // is enabled too. This is because shuffle button
        // no longer restricts from the current playlist's order.
        // You might get the same song as your next when you
        // turn the shuffle button on, so this behavior is
        // needed.
        // TODO: Make it an option in settings
        fullSheetLoopButton.addOnCheckedChangeListener { _, isChecked ->
            if (fullSheetShuffleButton.isChecked) {
                fullSheetLoopButton.isChecked = true
                fullSheetLoopButton.isChecked = true
            } else {
                fullSheetLoopButton.isChecked = isChecked
            }
        }

        fullSheetShuffleButton.addOnCheckedChangeListener { _, isChecked ->
            fullSheetShuffleButton.isChecked = isChecked
            fullSheetLoopButton.isChecked = fullSheetShuffleButton.isChecked
        }

        bottomSheetControlButton.setOnClickListener {
            if (musicPlayer != null) {
                changePlayer()
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
                changePlayer()
            } else if (musicPlayer == null && playlistViewModel.playList.size != 0
                && playlistViewModel.currentLocation != playlistViewModel.playList.size
            ) {
                thisSong()
            }
        }

        fullSheetBackButton.setOnClickListener {
            playerBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            bottomFullSizePlayerPreview.visibility = GONE
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
                val intentBroadcast = Intent("internal.play_seek")
                sendBroadcast(intentBroadcast)
                isUserTracking = false
            }
        }

        fullSheetSlider.addOnSliderTouchListener(touchListener)

        fullSheetSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) fullSheetTimeStamp.text =
                convertDurationToTimeStamp((value * 1000).toInt().toString())
        }

        sliderTask = object : Runnable {
            override fun run() {
                if (musicPlayer != null && playlistViewModel.currentLocation !=
                    playlistViewModel.playList.size
                ) {
                    if (musicPlayer!!.isPlaying) {
                        fullSheetSlider.isEnabled = true

                        // about the "/ 1000 + 0.2f", check line 396.
                        fullSheetSlider.valueTo = musicPlayer!!.duration.toFloat() / 1000 + 0.2f

                        if (!isUserTracking || (isUserTracking && musicPlayer!!.duration == 0)) {
                            fullSheetSlider.value = musicPlayer!!.currentPosition.toFloat() / 1000
                            val intentBroadcast = Intent("internal.play_seek")
                            sendBroadcast(intentBroadcast)
                            fullSheetTimeStamp.text =
                                convertDurationToTimeStamp(musicPlayer!!.currentPosition.toString())
                        }
                    }
                }
                // Update it per 200ms.
                handler.postDelayed(this, 200)
            }
        }

        handler.postDelayed(sliderTask, 200)
        // Slider behavior ends here.

        // Set the drawer's behavior.
        navigationView.setNavigationItemSelectedListener { menuItem ->
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

        // Tell the program to update the sheet when resuming.
        if (musicPlayer != null && musicPlayer!!.isPlaying) {
            val intentBroadcast = Intent("internal.play_start")
            sendBroadcast(intentBroadcast)
        } else if (musicPlayer != null && !musicPlayer!!.isPlaying) {
            val intentBroadcast = Intent("internal.play_pause")
            sendBroadcast(intentBroadcast)
        } else {
            val intentBroadcast = Intent("internal.play_stop")
            sendBroadcast(intentBroadcast)
        }
    }

    override fun onDestroy() {

        // Unregister everything.
        handler.removeCallbacks(sliderTask)
        unregisterReceiver(receiverPause)
        unregisterReceiver(receiverStop)
        unregisterReceiver(receiverPlay)
        unregisterReceiver(receiverSeek)
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

}