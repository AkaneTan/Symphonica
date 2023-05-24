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
import org.akanework.symphonica.R
import org.akanework.symphonica.logic.data.Song
import org.akanework.symphonica.logic.util.getYear
import org.akanework.symphonica.logic.util.replacePlaylist
import org.akanework.symphonica.ui.adapter.LibraryDisplayAdapter
import org.akanework.symphonica.ui.viewmodel.AlbumDisplayViewModel

class LibraryAlbumDisplayFragment : Fragment() {

    private var albumDisplayViewModel: AlbumDisplayViewModel? = null
    private var position: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition =
            MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true).setDuration(500)
        returnTransition =
            MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false).setDuration(500)
        if (albumDisplayViewModel == null) {
            albumDisplayViewModel = ViewModelProvider(this)[AlbumDisplayViewModel::class.java]
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_library_album_view, container, false)
        val topAppBar: MaterialToolbar = rootView.findViewById(R.id.topAppBar)
        val albumCover: ImageView = rootView.findViewById(R.id.library_album_view_cover)
        val albumName: TextView = rootView.findViewById(R.id.library_album_view_name)
        val albumArtist: TextView = rootView.findViewById(R.id.library_album_view_artist)
        val albumYear: TextView = rootView.findViewById(R.id.library_album_view_year)
        val displayPlay: MaterialButton = rootView.findViewById(R.id.library_album_view_play)
        val displayShuffle: MaterialButton = rootView.findViewById(R.id.library_album_view_shuffle)

        if (albumDisplayViewModel == null) {
            albumDisplayViewModel = ViewModelProvider(this)[AlbumDisplayViewModel::class.java]
        }

        position = if (albumDisplayViewModel!!.position == null) {
            requireArguments().getInt("Position")
        } else {
            albumDisplayViewModel!!.position
        }

        try {
            Glide.with(requireContext())
                .load(libraryViewModel.libraryAlbumList[position!!].songList.first().imgUri)
                .diskCacheStrategy(MainActivity.diskCacheStrategyCustom)
                .placeholder(R.drawable.ic_album_default_cover)
                .into(albumCover)
        } catch (_: Exception) {
            // Placeholder
        }

        albumName.text = libraryViewModel.libraryAlbumList[position!!].title
        albumArtist.text = libraryViewModel.libraryAlbumList[position!!].artist
        if (libraryViewModel.libraryAlbumList[position!!].artist == requireActivity().getString(R.string.library_album_view_unknown_artist)) {
            albumArtist.text = libraryViewModel.libraryAlbumList[position!!].songList.first().artist
        } else {
            albumArtist.text = libraryViewModel.libraryAlbumList[position!!].artist
        }

        val year = getYear(libraryViewModel.libraryAlbumList[position!!].songList.first().path)
        if (year != null) {
            albumYear.text = year
        }

        topAppBar.setNavigationOnClickListener {
            customFragmentManager.popBackStack()
        }
        val libraryAlbumView: RecyclerView =
            rootView.findViewById(R.id.library_album_view_recyclerview)

        val sortedSongList: List<Song>
        if (libraryViewModel.librarySortedAlbumList.isNotEmpty()) {
            libraryAlbumView.adapter =
                LibraryDisplayAdapter(libraryViewModel.librarySortedAlbumList[position!!].songList)
            sortedSongList = libraryViewModel.librarySortedAlbumList[position!!].songList
        } else {
            libraryAlbumView.adapter =
                LibraryDisplayAdapter(libraryViewModel.libraryAlbumList[position!!].songList)
            sortedSongList = libraryViewModel.libraryAlbumList[position!!].songList
        }
        libraryAlbumView.layoutManager = LinearLayoutManager(context)

        displayPlay.setOnClickListener {
            replacePlaylist(sortedSongList.toMutableList(), 0)
        }

        displayShuffle.setOnClickListener {
            replacePlaylist(sortedSongList.toMutableList(), 0)
            if (!isListShuffleEnabled) {
                fullSheetLoopButton.isChecked = true
            }
            fullSheetShuffleButton.isChecked = true
        }

        topAppBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.more -> {
                    val rootDialogView = MaterialAlertDialogBuilder(
                        requireContext(),
                        com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
                    )
                        .setTitle(getString(R.string.dialog_album_info))
                        .setView(R.layout.alert_dialog_album)
                        .setNeutralButton(getString(R.string.dialog_song_dismiss)) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                    val dialogName: TextInputEditText =
                        rootDialogView.findViewById(R.id.dialog_album_name)!!
                    val dialogArtist: TextInputEditText =
                        rootDialogView.findViewById(R.id.dialog_album_artist)!!
                    val dialogDuration: TextInputEditText =
                        rootDialogView.findViewById(R.id.dialog_album_duration)!!
                    val dialogYear: TextInputEditText =
                        rootDialogView.findViewById(R.id.dialog_album_year)!!

                    dialogName.setText(libraryViewModel.libraryAlbumList[position!!].title)
                    if (libraryViewModel.libraryAlbumList[position!!].artist == requireActivity().getString(
                            R.string.library_album_view_unknown_artist
                        )
                    ) {
                        dialogArtist.setText(libraryViewModel.libraryAlbumList[position!!].songList.first().artist)
                    } else {
                        dialogArtist.setText(libraryViewModel.libraryAlbumList[position!!].artist)
                    }

                    var duration: Long = 0
                    for (i in libraryViewModel.libraryAlbumList[position!!].songList) {
                        duration += i.duration
                    }

                    dialogDuration.setText(duration.toString())

                    val acquireYear =
                        getYear(
                            libraryViewModel.libraryAlbumList[position!!].songList.first().path.toUri()
                                .toString()
                        )
                    if (!year.isNullOrEmpty()) {
                        dialogYear.setText(acquireYear)
                    }
                }

                else ->
                    throw IllegalStateException()
            }
            true
        }

        return rootView
    }

}
