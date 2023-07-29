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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import org.akanework.symphonica.MAX_ALBUM_LIBRARY_LAYOUT
import org.akanework.symphonica.MainActivity.Companion.libraryViewModel
import org.akanework.symphonica.R
import org.akanework.symphonica.ui.adapter.LibraryGridAdapter

/**
 * [LibraryGridFragment] holds a view that contains
 * all albums.
 */
class LibraryGridFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_library_grid, container, false)

        val libraryGridView: RecyclerView = rootView.findViewById(R.id.library_gridview)

        val layoutManager = StaggeredGridLayoutManager(
            MAX_ALBUM_LIBRARY_LAYOUT,
            StaggeredGridLayoutManager.VERTICAL
        )

        libraryGridView.layoutManager = layoutManager
        val adapter = LibraryGridAdapter(libraryViewModel.libraryAlbumList)
        libraryGridView.adapter = adapter

        return rootView
    }
}
