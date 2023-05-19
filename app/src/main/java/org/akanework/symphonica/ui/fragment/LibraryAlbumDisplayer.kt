package org.akanework.symphonica

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.transition.MaterialSharedAxis
import org.akanework.symphonica.MainActivity.Companion.albumList
import org.akanework.symphonica.MainActivity.Companion.customFragmentManager
import org.akanework.symphonica.MainActivity.Companion.songList
import org.akanework.symphonica.logic.util.getTrackNumber
import org.akanework.symphonica.logic.util.getYear
import org.akanework.symphonica.ui.adapter.LibraryDisplayerAdapter
import org.w3c.dom.Text

class LibraryAlbumDisplayer : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true).setDuration(500)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false).setDuration(500)
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

        val position = requireArguments().getInt("Position")
        if (albumList[position].cover != null) {
            albumCover.setImageDrawable(albumList[position].cover)
        }
        albumName.text = albumList[position].title
        albumArtist.text = albumList[position].artist
        val year = getYear(albumList[position].songList.first().path)
        if (year != null) {
            albumYear.text = year
        }

        topAppBar.setNavigationOnClickListener {
            customFragmentManager.popBackStack()
        }
        val libraryAlbumView: RecyclerView = rootView.findViewById(R.id.displayer_recyclerview)
        libraryAlbumView.layoutManager = LinearLayoutManager(context)
        libraryAlbumView.adapter = LibraryDisplayerAdapter(albumList[position].songList)

        return rootView
    }
}