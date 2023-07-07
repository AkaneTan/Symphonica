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

package org.akanework.symphonica.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.transition.MaterialSharedAxis
import org.akanework.symphonica.MainActivity.Companion.isLibraryShuffleButtonEnabled
import org.akanework.symphonica.MainActivity.Companion.libraryViewModel
import org.akanework.symphonica.MainActivity.Companion.switchDrawer
import org.akanework.symphonica.MainActivity.Companion.switchNavigationViewIndex
import org.akanework.symphonica.R
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the transition animation.
        exitTransition =
            MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true).setDuration(500)
        reenterTransition =
            MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false).setDuration(500)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment.
        val rootView = inflater.inflate(R.layout.fragment_library, container, false)

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
                replacePlaylist(
                    libraryViewModel.librarySongList.toMutableList(),
                    (0 until libraryViewModel.librarySongList.size).random()
                )
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

        // Set the offscreenPageLimit to 2 to avoid stuttering.
        fragmentPager.offscreenPageLimit = 2

        TabLayoutMediator(libraryTabLayout, fragmentPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.library_tab_list)
                1 -> getString(R.string.library_tab_album)
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