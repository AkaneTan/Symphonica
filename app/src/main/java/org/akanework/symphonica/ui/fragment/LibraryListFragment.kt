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
import android.view.View.GONE
import android.view.ViewGroup
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import org.akanework.symphonica.MainActivity.Companion.songList
import org.akanework.symphonica.R
import org.akanework.symphonica.logic.data.Song
import org.akanework.symphonica.ui.adapter.LibraryListAdapter

class LibraryListFragment : Fragment() {

    companion object {
        lateinit var libraryListView: RecyclerView
        lateinit var adapter: LibraryListAdapter
        lateinit var loadingPrompt: MaterialCardView

        /**
         * This updates the full list of songs.
         * involving [libraryListView].
         */
        fun updateRecyclerView(newSongList: List<Song>) {
            if (::libraryListView.isInitialized) {
                val adapter = LibraryListAdapter(newSongList)
                libraryListView.adapter = adapter
                adapter.notifyItemRangeInserted(0, songList.size)
            }
        }

        /**
         * This is used for outer class to switch [loadingPrompt].
         * e.g. When loading from disk completed.
         */
        fun switchPrompt(operation: Int) {
            if (::loadingPrompt.isInitialized) {
                if (operation == 0) {
                    if (libraryListView.size == 0) {
                        loadingPrompt.visibility = View.VISIBLE
                    }
                } else if (operation == 1) {
                    loadingPrompt.visibility = GONE
                } else {
                    throw IllegalArgumentException()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val rootView = inflater.inflate(R.layout.fragment_library_list, container, false)

        loadingPrompt = rootView.findViewById(R.id.loading_prompt_list)
        libraryListView = rootView.findViewById(R.id.library_listview)

        // Initialize recyclerView.
        val layoutManager = LinearLayoutManager(context)
        libraryListView.layoutManager = layoutManager
        adapter = LibraryListAdapter(songList)
        libraryListView.adapter = adapter

        return rootView
    }

    override fun onResume() {
        super.onResume()
        // When resumed, close the loading prompt.
        if (libraryListView.size != 0) {
            loadingPrompt.visibility = GONE
        }
    }

}