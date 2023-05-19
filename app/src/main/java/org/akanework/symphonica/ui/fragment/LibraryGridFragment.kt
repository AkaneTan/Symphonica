package org.akanework.symphonica.ui.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import org.akanework.symphonica.MainActivity
import org.akanework.symphonica.MainActivity.Companion.albumList
import org.akanework.symphonica.R
import org.akanework.symphonica.logic.util.Album
import org.akanework.symphonica.logic.util.Song
import org.akanework.symphonica.ui.adapter.LibraryGridAdapter
import org.akanework.symphonica.ui.adapter.LibraryListAdapter

class LibraryGridFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    companion object {
        lateinit var libraryGridView: RecyclerView
        lateinit var adapter: LibraryGridAdapter
        fun updateRecyclerView(newAlbumList: List<Album>) {
            if (::libraryGridView.isInitialized) {
                val adapter = LibraryGridAdapter(newAlbumList)
                LibraryGridFragment.libraryGridView.adapter = adapter
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val rootView = inflater.inflate(R.layout.fragment_library_grid, container, false)
        val layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        libraryGridView = rootView.findViewById(R.id.library_gridview)
        libraryGridView.layoutManager = layoutManager
        adapter = LibraryGridAdapter(albumList)
        libraryGridView.adapter = adapter

        return rootView
    }

}