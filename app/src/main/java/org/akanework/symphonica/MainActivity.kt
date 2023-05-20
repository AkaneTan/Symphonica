package org.akanework.symphonica

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.MediaPlayer
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
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.slider.Slider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.akanework.symphonica.SymphonicaApplication.Companion.context
import org.akanework.symphonica.logic.data.Album
import org.akanework.symphonica.logic.data.Song
import org.akanework.symphonica.logic.data.loadDataFromCache
import org.akanework.symphonica.logic.data.loadDataFromDisk
import org.akanework.symphonica.logic.service.SymphonicaPlayerService.Companion.updatePlaybackState
import org.akanework.symphonica.logic.util.changePlayer
import org.akanework.symphonica.logic.util.convertDurationToTimeStamp
import org.akanework.symphonica.logic.util.nextSong
import org.akanework.symphonica.logic.util.prevSong
import org.akanework.symphonica.ui.component.PlaylistBottomSheet
import org.akanework.symphonica.ui.viewmodel.LibraryViewModel
import org.akanework.symphonica.ui.viewmodel.PlaylistViewModel
import java.io.File
import kotlin.time.ExperimentalTime


class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 123

    companion object {
        lateinit var songList: List<Song>
        lateinit var albumList: List<Album>
        lateinit var navigationView: NavigationView
        lateinit var libraryViewModel: LibraryViewModel
        lateinit var playlistViewModel: PlaylistViewModel
        lateinit var cacheDirDrawable: File
            private set
        lateinit var sharedPreferences: SharedPreferences
        lateinit var customFragmentManager: FragmentManager
        lateinit var playlistButton: MaterialButton
        lateinit var fullSheetLoopButton: MaterialButton
        lateinit var fullSheetShuffleButton: MaterialButton
        lateinit var managerSymphonica: NotificationManager
        lateinit var channelSymphonica: NotificationChannel
        var isShuffleEnabled = false
        var isLoopEnabled = false
        var actuallyPlaying = false
        var musicPlayer: MediaPlayer? = null
        var isUserTracking = false

        fun switchNavigationView() {
            if (navigationView.isGone) {
                navigationView.visibility = VISIBLE
            } else {
                navigationView.visibility = GONE
            }
        }
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        cacheDirDrawable = File(applicationContext.cacheDir, "cache") // 内部缓存目录下的名为 "cache" 的子目录
        cacheDirDrawable.mkdirs() // 创建目录

        customFragmentManager = supportFragmentManager

        songList = listOf()

        albumList = listOf()
        libraryViewModel = ViewModelProvider(this)[LibraryViewModel::class.java]
        playlistViewModel = ViewModelProvider(this)[PlaylistViewModel::class.java]
        sharedPreferences = getSharedPreferences("library_data", Context.MODE_PRIVATE)

        coroutineScope.launch {
            if (sharedPreferences.getString("song_list", null) == null) {
                loadDataFromDisk()
            } else {
                loadDataFromCache()
            }
        }

        setContentView(R.layout.activity_main)

        managerSymphonica = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        channelSymphonica = NotificationChannel("channel_symphonica", "Symphonica", NotificationManager.IMPORTANCE_DEFAULT)
        managerSymphonica.createNotificationChannel(channelSymphonica)


        navigationView = findViewById(R.id.navigation_view)

        val playerBottomSheetBehavior =
            BottomSheetBehavior.from(findViewById(R.id.standard_bottom_sheet))
        playerBottomSheetBehavior.isHideable = false

        val bottomPlayerPreview: FrameLayout = findViewById(R.id.bottom_player)
        val bottomFullSizePlayerPreview: LinearLayout = findViewById(R.id.full_size_sheet_player)

        bottomPlayerPreview.setOnClickListener {
            if (playerBottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                playerBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                bottomFullSizePlayerPreview.visibility = VISIBLE
                bottomPlayerPreview.visibility = GONE
            }
        }

        // Register handler for updating metadata
        val handler = Handler(Looper.getMainLooper())
        val playlistBottomSheet = PlaylistBottomSheet()

        val sheetAlbumCover = findViewById<ImageView>(R.id.sheet_album_cover)
        val bottomSheetSongName = findViewById<TextView>(R.id.bottom_sheet_song_name)
        val bottomSheetArtistAndAlbum = findViewById<TextView>(R.id.bottom_sheet_artist_album)
        val bottomSheetControlButton = findViewById<MaterialButton>(R.id.bottom_sheet_play)
        val bottomSheetNextButton = findViewById<MaterialButton>(R.id.bottom_sheet_next)
        val fullSheetBackButton = findViewById<MaterialButton>(R.id.sheet_extract_player)
        val fullSheetLocation = findViewById<TextView>(R.id.sheet_song_location)
        val fullSheetSongName = findViewById<TextView>(R.id.sheet_song_name)
        val fullSheetArtist = findViewById<TextView>(R.id.sheet_author)
        val fullSheetAlbum = findViewById<TextView>(R.id.sheet_album)
        val fullSheetCover = findViewById<ImageView>(R.id.sheet_cover)
        val fullSheetControlButton = findViewById<FloatingActionButton>(R.id.sheet_mid_button)
        val fullSheetNextButton = findViewById<MaterialButton>(R.id.sheet_next_song)
        val fullSheetPrevButton = findViewById<MaterialButton>(R.id.sheet_previous_song)
        val fullSheetSlider = findViewById<Slider>(R.id.sheet_slider)
        val fullSheetDuration = findViewById<TextView>(R.id.sheet_end_time)
        val fullSheetTimeStamp = findViewById<TextView>(R.id.sheet_now_time)
        fullSheetLoopButton = findViewById(R.id.sheet_loop)
        fullSheetShuffleButton = findViewById(R.id.sheet_random)

        fullSheetLoopButton.addOnCheckedChangeListener { _, isChecked ->
            if (isShuffleEnabled) {
                isLoopEnabled = true
                fullSheetLoopButton.isChecked = true
            } else {
                isLoopEnabled = isChecked
            }
        }

        fullSheetShuffleButton.addOnCheckedChangeListener { _, isChecked ->
            isShuffleEnabled = isChecked
            fullSheetLoopButton.isChecked = isShuffleEnabled
        }

        bottomSheetControlButton.setOnClickListener {
            changePlayer()
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
            changePlayer()
        }

        fullSheetBackButton.setOnClickListener {
            playerBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            bottomFullSizePlayerPreview.visibility = GONE
            bottomPlayerPreview.visibility = VISIBLE
        }

        val touchListener: Slider.OnSliderTouchListener = object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                isUserTracking = true
            }

            override fun onStopTrackingTouch(slider: Slider) {
                musicPlayer?.seekTo((slider.value * 1000).toInt())
                updatePlaybackState()
                isUserTracking = false
            }
        }

        fullSheetSlider.addOnSliderTouchListener(touchListener)

        fullSheetSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) fullSheetTimeStamp.text = convertDurationToTimeStamp((value * 1000).toInt().toString())
        }

        val standardTask = object: Runnable {
            override fun run() {
                if (musicPlayer != null && playlistViewModel.currentLocation !=
                    playlistViewModel.playList.size) {
                    if (playlistViewModel.playList[playlistViewModel.currentLocation].cover != null) {
                        sheetAlbumCover.setImageDrawable(playlistViewModel.playList[playlistViewModel.currentLocation].cover)
                        fullSheetCover.setImageDrawable(playlistViewModel.playList[playlistViewModel.currentLocation].cover)
                    }
                    else {
                        sheetAlbumCover.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_album_default_cover))
                        fullSheetCover.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_album_default_cover))
                    }
                    bottomSheetSongName.text = playlistViewModel.playList[playlistViewModel.currentLocation].title
                    fullSheetSongName.text = playlistViewModel.playList[playlistViewModel.currentLocation].title
                    bottomSheetArtistAndAlbum.text = "${playlistViewModel.playList[playlistViewModel.currentLocation].artist} • ${playlistViewModel.playList[playlistViewModel.currentLocation].album}"
                    fullSheetAlbum.text = playlistViewModel.playList[playlistViewModel.currentLocation].album
                    fullSheetArtist.text = playlistViewModel.playList[playlistViewModel.currentLocation].artist
                    if (musicPlayer!!.isPlaying) {
                        fullSheetSlider.isEnabled = true
                        fullSheetSlider.valueTo = musicPlayer!!.duration.toFloat() / 1000 + 0.2f
                        fullSheetDuration.text = convertDurationToTimeStamp(musicPlayer!!.duration.toString())
                        if (!isUserTracking || (isUserTracking && musicPlayer!!.duration == 0)) {
                            fullSheetSlider.value = musicPlayer!!.currentPosition.toFloat() / 1000
                            fullSheetTimeStamp.text = convertDurationToTimeStamp(musicPlayer!!.currentPosition.toInt().toString())
                        }
                        bottomSheetControlButton.icon = ContextCompat.getDrawable(context, R.drawable.ic_pause)
                        fullSheetControlButton.setImageResource(R.drawable.ic_pause)

                    } else if (!musicPlayer!!.isPlaying && !actuallyPlaying) {
                        bottomSheetControlButton.icon = ContextCompat.getDrawable(context, R.drawable.ic_sheet_play)
                        fullSheetControlButton.setImageResource(R.drawable.ic_sheet_play)
                    }
                    fullSheetLocation.text = "${playlistViewModel.currentLocation + 1} in ${playlistViewModel.playList.size} song(s)"

                }
                else if (musicPlayer == null) {
                    fullSheetSlider.isEnabled = false
                }
                handler.postDelayed(this, 200)
            }
        }

        handler.postDelayed(standardTask, 200)

        playlistButton = findViewById(R.id.sheet_playlist)

        playlistButton.setOnClickListener {
            playlistBottomSheet.show(supportFragmentManager, PlaylistBottomSheet.TAG)

        }

        val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED && bottomPlayerPreview.isVisible) {
                    bottomFullSizePlayerPreview.visibility = GONE
                } else if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                    bottomFullSizePlayerPreview.visibility = VISIBLE
                    bottomPlayerPreview.visibility = VISIBLE
                } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomPlayerPreview.visibility = GONE
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                bottomPlayerPreview.alpha = 1 - (slideOffset * 2f)
                bottomFullSizePlayerPreview.alpha = slideOffset
            }
        }

        playerBottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback)

        // 检查权限状态
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_MEDIA_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // 如果权限未授予，显示权限请求弹窗
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.READ_MEDIA_AUDIO),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    @OptIn(ExperimentalTime::class)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // 检查请求码
        coroutineScope.launch {
            loadDataFromDisk()
        }
    }
}