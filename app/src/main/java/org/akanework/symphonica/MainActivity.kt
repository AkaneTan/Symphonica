package org.akanework.symphonica

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.DelicateCoroutinesApi
import org.akanework.symphonica.logic.util.Song
import org.akanework.symphonica.logic.util.getAllSongs
import org.akanework.symphonica.ui.fragment.LibraryGridFragment
import org.akanework.symphonica.ui.fragment.LibraryListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.akanework.symphonica.SymphonicaApplication.Companion.context
import org.akanework.symphonica.logic.util.Album
import org.akanework.symphonica.logic.util.getAllAlbums

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 123

    companion object {
        lateinit var songList: List<Song>
        lateinit var albumList: List<Album>
        lateinit var navigationView: NavigationView

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

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        songList = listOf()
        albumList = listOf()

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
        } else {
            runOnUiThread {
                if (songList.size == 0) {
                    songList = getAllSongs(this)
                    LibraryListFragment.updateRecyclerView(songList)
                }
                if (albumList.size == 0) {
                    albumList = getAllAlbums(this@MainActivity, songList)
                    LibraryGridFragment.updateRecyclerView(albumList)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // 检查请求码
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                runOnUiThread {
                    if (songList.size == 0) {
                        songList = getAllSongs(this)
                        LibraryListFragment.updateRecyclerView(songList)
                    }
                    if (albumList.size == 0) {
                        albumList = getAllAlbums(this@MainActivity, songList)
                        LibraryGridFragment.updateRecyclerView(albumList)
                    }
                }
            } else {
                // 用户拒绝了权限，可以显示一个提示或采取其他操作
                // ...
            }
        }
    }

}