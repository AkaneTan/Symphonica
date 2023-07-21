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

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.transition.MaterialSharedAxis
import org.akanework.symphonica.MainActivity.Companion.customFragmentManager
import org.akanework.symphonica.MainActivity.Companion.playlistViewModel
import org.akanework.symphonica.PAGE_TRANSITION_DURATION
import org.akanework.symphonica.R
import org.akanework.symphonica.ui.adapter.HomePlaylistAdapter

/**
 * [HomePlaylistFragment] is the history list
 * page.
 */
class HomePlaylistFragment : Fragment() {
    private lateinit var adapter: HomePlaylistAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition =
                MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true).setDuration(
                    PAGE_TRANSITION_DURATION)
        exitTransition =
                MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false).setDuration(
                    PAGE_TRANSITION_DURATION)
    }

    @SuppressLint("StringFormatMatches")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_home_playlist_view, container, false)

        val topAppBar = rootView.findViewById<MaterialToolbar>(R.id.topAppBar)

        val recyclerView = rootView.findViewById<RecyclerView>(R.id.home_playlist_recyclerview)

        topAppBar.setNavigationOnClickListener {
            customFragmentManager.popBackStack()
        }

        topAppBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.add_playlist -> {
                    customFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, HomePlaylistAddFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }
                else ->
                    throw IllegalAccessException()
            }
        }

        val layoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = layoutManager
        adapter = HomePlaylistAdapter(playlistViewModel.playlistList)
        recyclerView.adapter = adapter

        return rootView
    }
}
