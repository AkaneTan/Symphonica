package org.akanework.symphonica

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.transition.TransitionManager
import com.google.android.material.animation.AnimationUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.transition.MaterialFade
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import org.akanework.symphonica.logic.util.getAllSongs
import org.akanework.symphonica.ui.fragment.LibraryGridFragment
import org.akanework.symphonica.ui.fragment.LibraryListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.symphonica.SymphonicaApplication.Companion.context
import org.akanework.symphonica.logic.data.Album
import org.akanework.symphonica.logic.data.Song
import org.akanework.symphonica.logic.util.getAllAlbums
import org.akanework.symphonica.logic.util.loadLibrarySongList
import org.akanework.symphonica.logic.util.saveLibrarySongList
import org.akanework.symphonica.ui.viewmodel.LibraryViewModel
import java.io.File
import kotlin.reflect.typeOf
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 123

    companion object {
        lateinit var songList: List<Song>
        lateinit var albumList: List<Album>
        lateinit var navigationView: NavigationView
        lateinit var libraryViewModel: LibraryViewModel
        lateinit var cacheDirDrawable: File
            private set
        lateinit var sharedPreferences: SharedPreferences

        fun switchNavigationView() {
            if (navigationView.isGone) {
                navigationView.visibility = VISIBLE
            } else {
                navigationView.visibility = GONE
            }
        }
        fun switchBottomSheet() {

        }
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        cacheDirDrawable = File(applicationContext.cacheDir, "cache") // 内部缓存目录下的名为 "cache" 的子目录
        cacheDirDrawable.mkdirs() // 创建目录

        songList = listOf()

        albumList = listOf()
        libraryViewModel = ViewModelProvider(this).get(LibraryViewModel::class.java)
        sharedPreferences = getSharedPreferences("library_data", Context.MODE_PRIVATE)

        coroutineScope.launch {
            if (libraryViewModel.librarySongList.isEmpty()) {
                withContext(Dispatchers.IO) {
                    songList = loadLibrarySongList(sharedPreferences)
                    libraryViewModel.librarySongList = songList
                }
                withContext(Dispatchers.Main) {
                    LibraryListFragment.updateRecyclerView(songList)
                    albumList = getAllAlbums(this@MainActivity, songList)
                    libraryViewModel.libraryAlbumList = albumList
                }
            } else {
                songList = libraryViewModel.librarySongList
                albumList = libraryViewModel.libraryAlbumList
            }
            withContext(Dispatchers.Main) {
                LibraryListFragment.dismissPrompt()
                LibraryListFragment.updateRecyclerView(songList)
                LibraryGridFragment.dismissPrompt()
                LibraryGridFragment.updateRecyclerView(albumList)
            }
        }

        setContentView(R.layout.activity_main)

        navigationView = findViewById(R.id.navigation_view)

        val playerBottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.standard_bottom_sheet))
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
            if (libraryViewModel.librarySongList.isEmpty()) {
                withContext(Dispatchers.IO) {
                    if (songList.isEmpty()) {
                        songList = getAllSongs(context)
                        libraryViewModel.librarySongList = songList
                    }
                }
                withContext(Dispatchers.Main) {
                    if (albumList.isEmpty()) {
                        albumList = getAllAlbums(this@MainActivity, songList)
                        libraryViewModel.libraryAlbumList = albumList
                    }
                }
            } else {
                albumList = libraryViewModel.libraryAlbumList
                songList = libraryViewModel.librarySongList
            }

            withContext(Dispatchers.Main) {
                LibraryListFragment.dismissPrompt()
                LibraryListFragment.updateRecyclerView(songList)
                LibraryGridFragment.dismissPrompt()
                LibraryGridFragment.updateRecyclerView(albumList)
            }

            withContext(Dispatchers.IO) {
                saveLibrarySongList(songList, sharedPreferences)
            }
        }
    }

    /*
    data class AlbumListWrapper(val albums: List<Album>)
    data class SongListWrapper(val songs: List<Song>)

    // onSaveInstanceState method
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        val gson = Gson()

        val albumListWrapper = AlbumListWrapper(albumList)
        val albumListJson = gson.toJson(albumListWrapper)
        outState.putString("ALBUM_LIST", albumListJson)

        val songListWrapper = SongListWrapper(songList)
        val songListJson = gson.toJson(songListWrapper)
        outState.putString("SONG_LIST", songListJson)
    }


    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        val gson = Gson()

        val albumListJson = savedInstanceState.getString("ALBUM_LIST")
        if (albumListJson != null && albumListJson.isNotEmpty()) {
            val albumListWrapper = gson.fromJson(albumListJson, AlbumListWrapper::class.java)
            albumList = albumListWrapper.albums
        }

        val songListJson = savedInstanceState.getString("SONG_LIST")
        if (songListJson != null && songListJson.isNotEmpty()) {
            val songListWrapper = gson.fromJson(songListJson, SongListWrapper::class.java)
            songList = songListWrapper.songs
        }
    }


     */

}