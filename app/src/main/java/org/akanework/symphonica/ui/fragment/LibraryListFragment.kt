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
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.akanework.symphonica.MainActivity.Companion.libraryViewModel
import org.akanework.symphonica.R
import org.akanework.symphonica.logic.data.Song
import org.akanework.symphonica.ui.adapter.LibraryListAdapter

/**
 * [LibraryListFragment] holds a recyclerview
 * which contains all songs.
 */
class LibraryListFragment : Fragment() {

    companion object {
        var libraryListView: RecyclerView? = null
        lateinit var adapter: LibraryListAdapter
        private var isOpposite = false

        /**
         * This updates the full list of songs.
         * involving [libraryListView].
         */
        fun updateRecyclerListViewOppositeOrder(songs: List<Song>) {
            if (libraryListView != null) {
                if (!isOpposite) {
                    val adapter = LibraryListAdapter(songs.reversed())
                    libraryListView!!.adapter = adapter
                    adapter.notifyItemRangeChanged(0, songs.size)
                    isOpposite = true
                } else {
                    val adapter = LibraryListAdapter(songs)
                    libraryListView!!.adapter = adapter
                    adapter.notifyItemRangeChanged(0, songs.size)
                    isOpposite = false
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_library_list, container, false)

        libraryListView = rootView.findViewById(R.id.library_listview)

        // Initialize recyclerView.
        val layoutManager = LinearLayoutManager(context)
        libraryListView?.layoutManager = layoutManager
        adapter = LibraryListAdapter(libraryViewModel.librarySongList)
        libraryListView?.adapter = adapter

        return rootView
    }

    override fun onDestroyView() {
        libraryListView = null
        super.onDestroyView()
    }

}