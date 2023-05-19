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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import org.akanework.symphonica.MainActivity.Companion.albumList
import org.akanework.symphonica.R
import org.akanework.symphonica.logic.data.Album
import org.akanework.symphonica.ui.adapter.LibraryGridAdapter

class LibraryGridFragment : Fragment() {

    companion object {
        lateinit var libraryGridView: RecyclerView
        lateinit var adapter: LibraryGridAdapter

        /**
         * This updates gridview.
         * involving [libraryGridView].
         */
        fun updateRecyclerView(newAlbumList: List<Album>) {
            if (::libraryGridView.isInitialized) {
                val adapter = LibraryGridAdapter(newAlbumList)
                libraryGridView.adapter = adapter
                adapter.notifyItemRangeInserted(0, albumList.size)
            }
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val rootView = inflater.inflate(R.layout.fragment_library_grid, container, false)

        libraryGridView = rootView.findViewById(R.id.library_gridview)

        val layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)

        libraryGridView.layoutManager = layoutManager
        adapter = LibraryGridAdapter(albumList)
        libraryGridView.adapter = adapter

        return rootView
    }

}