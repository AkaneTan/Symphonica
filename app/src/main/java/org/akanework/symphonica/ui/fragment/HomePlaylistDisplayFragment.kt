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

package org.akanework.symphonica.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.transition.MaterialSharedAxis
import org.akanework.symphonica.MainActivity
import org.akanework.symphonica.MainActivity.Companion.customFragmentManager
import org.akanework.symphonica.MainActivity.Companion.fullSheetLoopButton
import org.akanework.symphonica.MainActivity.Companion.fullSheetShuffleButton
import org.akanework.symphonica.MainActivity.Companion.isListShuffleEnabled
import org.akanework.symphonica.MainActivity.Companion.libraryViewModel
import org.akanework.symphonica.MainActivity.Companion.playlistViewModel
import org.akanework.symphonica.PAGE_TRANSITION_DURATION
import org.akanework.symphonica.R
import org.akanework.symphonica.logic.data.Song
import org.akanework.symphonica.logic.util.getYear
import org.akanework.symphonica.logic.util.replacePlaylist
import org.akanework.symphonica.ui.adapter.LibraryDisplayAdapter
import org.akanework.symphonica.ui.adapter.PlaylistDisplayAdapter
import org.akanework.symphonica.ui.viewmodel.AlbumDisplayViewModel

/**
 * [HomePlaylistDisplayFragment] is the album detail
 * page.
 */
class HomePlaylistDisplayFragment : Fragment() {
    private var albumDisplayViewModel: AlbumDisplayViewModel? = null
    private var position: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition =
                MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true).setDuration(
                    PAGE_TRANSITION_DURATION)
        exitTransition =
                MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false).setDuration(
                    PAGE_TRANSITION_DURATION)
        albumDisplayViewModel ?: run {
            albumDisplayViewModel = ViewModelProvider(this)[AlbumDisplayViewModel::class.java]
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_home_playlist_display, container, false)
        val topAppBar: MaterialToolbar = rootView.findViewById(R.id.topAppBar)
        val albumCover: ImageView = rootView.findViewById(R.id.library_album_view_cover)
        val albumName: TextView = rootView.findViewById(R.id.library_album_view_name)
        val albumArtist: TextView = rootView.findViewById(R.id.library_album_view_artist)
        val displayPlay: MaterialButton = rootView.findViewById(R.id.library_album_view_play)
        val displayShuffle: MaterialButton = rootView.findViewById(R.id.library_album_view_shuffle)

        albumDisplayViewModel ?: run {
            albumDisplayViewModel = ViewModelProvider(this)[AlbumDisplayViewModel::class.java]
        }

        position = albumDisplayViewModel!!.position ?: requireArguments().getInt("Position")

        val firstSongInList = playlistViewModel.playlistList[position!!].songList.first()

        try {
            Glide.with(requireContext())
                .load(firstSongInList.imgUri)
                .diskCacheStrategy(MainActivity.diskCacheStrategyCustom)
                .placeholder(R.drawable.ic_album_default_cover)
                .into(albumCover)
        } catch (_: Exception) {
            // Placeholder
        }

        albumName.text = playlistViewModel.playlistList[position!!].title
        albumArtist.text = playlistViewModel.playlistList[position!!].desc

        topAppBar.setNavigationOnClickListener {
            customFragmentManager.popBackStack()
        }
        val libraryAlbumView: RecyclerView =
            rootView.findViewById(R.id.library_album_view_recyclerview)

        libraryAlbumView.adapter =
                    PlaylistDisplayAdapter(playlistViewModel.playlistList[position!!].songList)
        val sortedSongList: List<Song> = playlistViewModel.playlistList[position!!].songList
        libraryAlbumView.layoutManager = LinearLayoutManager(context)

        displayPlay.setOnClickListener {
            replacePlaylist(sortedSongList.toMutableList(), 0)
        }

        displayShuffle.setOnClickListener {
            if (!isListShuffleEnabled) {
                replacePlaylist(sortedSongList.toMutableList(), 0)
                fullSheetLoopButton?.isChecked = true
            } else {
                fullSheetShuffleButton?.isChecked = true
                val playlist = mutableListOf<Song>()
                val originalPlaylist = playlistViewModel.originalPlaylist
                val shuffleSong = sortedSongList.random()

                playlist.addAll(sortedSongList)
                originalPlaylist.clear()
                originalPlaylist.addAll(sortedSongList)
                playlist.shuffle()
                playlist.remove(shuffleSong)
                playlist.add(0, shuffleSong)
                replacePlaylist(playlist, 0)
            }
        }

        return rootView
    }
}
