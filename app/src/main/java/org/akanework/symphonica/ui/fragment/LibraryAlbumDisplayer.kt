package org.akanework.symphonica.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.transition.MaterialSharedAxis
import org.akanework.symphonica.MainActivity.Companion.albumList
import org.akanework.symphonica.MainActivity.Companion.customFragmentManager
import org.akanework.symphonica.MainActivity.Companion.fullSheetLoopButton
import org.akanework.symphonica.MainActivity.Companion.fullSheetShuffleButton
import org.akanework.symphonica.MainActivity.Companion.isLoopEnabled
import org.akanework.symphonica.MainActivity.Companion.isShuffleEnabled
import org.akanework.symphonica.MainActivity.Companion.libraryViewModel
import org.akanework.symphonica.R
import org.akanework.symphonica.logic.util.getYear
import org.akanework.symphonica.logic.util.replacePlaylist
import org.akanework.symphonica.ui.adapter.LibraryDisplayerAdapter
import org.akanework.symphonica.ui.viewmodel.AlbumDisplayViewModel

class LibraryAlbumDisplayer : Fragment() {

    private var albumDisplayViewModel: AlbumDisplayViewModel? = null
    var position: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true).setDuration(500)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false).setDuration(500)
        if (albumDisplayViewModel == null) {
            albumDisplayViewModel = ViewModelProvider(this)[AlbumDisplayViewModel::class.java]
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_library_album_displayer, container, false)
        val topAppBar: MaterialToolbar = rootView.findViewById(R.id.topAppBar)
        val albumCover: ImageView = rootView.findViewById(R.id.displayer_album_cover)
        val albumName: TextView = rootView.findViewById(R.id.displayer_album_name)
        val albumArtist: TextView = rootView.findViewById(R.id.displayer_album_artist)
        val albumYear: TextView = rootView.findViewById(R.id.displayer_year)
        val displayPlay: MaterialButton = rootView.findViewById(R.id.displayer_play)
        val displayShuffle: MaterialButton = rootView.findViewById(R.id.displayer_shuffle)

        if (albumDisplayViewModel == null) {
            albumDisplayViewModel = ViewModelProvider(this)[AlbumDisplayViewModel::class.java]
        }

        if (albumDisplayViewModel!!.position == null) {
            position = requireArguments().getInt("Position")
        } else {
            position = albumDisplayViewModel!!.position
        }
        if (albumList.isEmpty()) {
            albumList = libraryViewModel.libraryAlbumList
        }
        if (albumList[position!!].cover != null) {
            albumCover.setImageDrawable(albumList[position!!].cover)
        }
        albumName.text = albumList[position!!].title
        albumArtist.text = albumList[position!!].artist
        val year = getYear(albumList[position!!].songList.first().path)
        if (year != null) {
            albumYear.text = year
        }

        topAppBar.setNavigationOnClickListener {
            customFragmentManager.popBackStack()
        }
        val libraryAlbumView: RecyclerView = rootView.findViewById(R.id.displayer_recyclerview)
        libraryAlbumView.layoutManager = LinearLayoutManager(context)
        libraryAlbumView.adapter = LibraryDisplayerAdapter(albumList[position!!].songList)

        displayPlay.setOnClickListener {
            replacePlaylist(albumList[position!!].songList.toMutableList(), 0)
        }

        displayShuffle.setOnClickListener {
            replacePlaylist(albumList[position!!].songList.toMutableList(), 0)
            isLoopEnabled = true
            isShuffleEnabled = true
            fullSheetLoopButton.isChecked = true
            fullSheetShuffleButton.isChecked = true
        }

        return rootView
    }

    override fun onResume() {
        super.onResume()
    }
}