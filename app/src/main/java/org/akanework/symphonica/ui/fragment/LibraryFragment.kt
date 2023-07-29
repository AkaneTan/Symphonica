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
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.akanework.symphonica.MainActivity
import org.akanework.symphonica.MainActivity.Companion.isAkaneVisible
import org.akanework.symphonica.MainActivity.Companion.isLibraryShuffleButtonEnabled
import org.akanework.symphonica.MainActivity.Companion.libraryViewModel
import org.akanework.symphonica.MainActivity.Companion.switchDrawer
import org.akanework.symphonica.MainActivity.Companion.switchNavigationViewIndex
import org.akanework.symphonica.R
import org.akanework.symphonica.TAB_ALBUM
import org.akanework.symphonica.TAB_LIST
import org.akanework.symphonica.logic.data.Song
import org.akanework.symphonica.logic.util.replacePlaylist
import org.akanework.symphonica.ui.adapter.NavFragmentPageAdapter
import org.akanework.symphonica.ui.fragment.LibraryListFragment.Companion.updateRecyclerListViewOppositeOrder

/**
 * [LibraryFragment] is the fragment that
 * contains two child fragment views which is
 * [LibraryGridFragment] and [LibraryListFragment].
 *
 * At one time this was the front page but then replaced by
 * [HomeFragment].
 */
class LibraryFragment : Fragment() {
    private lateinit var fragmentPager: ViewPager2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment.
        val rootView = inflater.inflate(R.layout.fragment_library, container, false)

        if (isAkaneVisible) {
            rootView.findViewById<ImageView>(R.id.akane).visibility = VISIBLE
        }

        val topAppBar: MaterialToolbar = rootView.findViewById(R.id.topAppBar)
        val libraryTabLayout: TabLayout = rootView.findViewById(R.id.library_tablayout)
        fragmentPager = rootView.findViewById(R.id.fragmentSwitch)

        val libraryShuffleButton =
            rootView.findViewById<FloatingActionButton>(R.id.library_shuffle_button)

        if (isLibraryShuffleButtonEnabled) {
            libraryShuffleButton.visibility = VISIBLE
        }

        libraryShuffleButton.setOnClickListener {
            if (libraryViewModel.librarySongList.isNotEmpty()) {
                if (!MainActivity.isListShuffleEnabled) {
                    replacePlaylist(
                        libraryViewModel.librarySongList.toMutableList(),
                        (0 until libraryViewModel.librarySongList.size).random()
                    )
                    MainActivity.fullSheetShuffleButton?.isChecked = true
                    MainActivity.fullSheetLoopButton?.isChecked = true
                } else {
                    MainActivity.fullSheetShuffleButton?.isChecked = true
                    val playlist = mutableListOf<Song>()
                    playlist.addAll(libraryViewModel.librarySongList)
                    val shuffleSong = playlist.random()

                    val originalPlaylist = MainActivity.playlistViewModel.originalPlaylist
                    originalPlaylist.clear()
                    originalPlaylist.addAll(playlist)
                    playlist.shuffle()
                    playlist.remove(shuffleSong)
                    playlist.add(0, shuffleSong)
                    replacePlaylist(playlist, 0)
                }
            }
        }

        topAppBar.setNavigationOnClickListener {
            switchDrawer()
        }

        topAppBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.filter -> {
                    updateRecyclerListViewOppositeOrder(libraryViewModel.librarySongList)
                    true
                }

                else ->
                    throw IllegalStateException()
            }
        }

        fragmentPager.adapter = NavFragmentPageAdapter(requireActivity())

        // Set the offscreenPageLimit to 1 to avoid stuttering.
        fragmentPager.offscreenPageLimit = 1

        TabLayoutMediator(libraryTabLayout, fragmentPager) { tab, position ->
            tab.text = when (position) {
                TAB_LIST -> getString(R.string.library_tab_list)
                TAB_ALBUM -> getString(R.string.library_tab_album)
                else -> "Unknown"
            }
        }.attach()

        return rootView
    }

    override fun onResume() {
        super.onResume()
        // Set the current fragment to library
        switchNavigationViewIndex(0)
    }
}
