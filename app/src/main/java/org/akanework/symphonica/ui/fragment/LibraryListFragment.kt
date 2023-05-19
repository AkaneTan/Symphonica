package org.akanework.symphonica.ui.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.akanework.symphonica.MainActivity
import org.akanework.symphonica.R
import org.akanework.symphonica.logic.util.Song
import org.akanework.symphonica.ui.adapter.LibraryGridAdapter
import org.akanework.symphonica.ui.adapter.LibraryListAdapter

class LibraryListFragment : Fragment() {

    companion object {
        lateinit var libraryListView: RecyclerView
        lateinit var adapter: LibraryListAdapter
        fun updateRecyclerView(newSongList: List<Song>) {
            if (::libraryListView.isInitialized) {
                val adapter = LibraryListAdapter(newSongList)
                libraryListView.adapter = adapter
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val rootView = inflater.inflate(R.layout.fragment_library_list, container, false)
        val layoutManager = LinearLayoutManager(context)
        libraryListView = rootView.findViewById(R.id.library_listview)
        libraryListView.layoutManager = layoutManager
        adapter = LibraryListAdapter(MainActivity.songList)
        libraryListView.adapter = adapter

        return rootView
    }

}