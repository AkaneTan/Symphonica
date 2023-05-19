package org.akanework.symphonica.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.size
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.transition.MaterialSharedAxis
import org.akanework.symphonica.MainActivity.Companion.albumList
import org.akanework.symphonica.R
import org.akanework.symphonica.logic.data.Album
import org.akanework.symphonica.ui.adapter.LibraryGridAdapter

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
                libraryGridView.adapter = adapter
                adapter.notifyItemRangeInserted(0, albumList.size)
            }
        }

        fun getViewSize(): Int {
            if (::libraryGridView.isInitialized) {
                return libraryGridView.size
            }
            return 0
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