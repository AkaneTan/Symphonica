package org.akanework.symphonica.ui.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.transition.TransitionManager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.transition.MaterialFade
import org.akanework.symphonica.MainActivity
import org.akanework.symphonica.MainActivity.Companion.albumList
import org.akanework.symphonica.R
import org.akanework.symphonica.logic.data.Album
import org.akanework.symphonica.ui.adapter.LibraryGridAdapter
import org.akanework.symphonica.ui.adapter.LibraryListAdapter

class LibraryGridFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    companion object {
        lateinit var libraryGridView: RecyclerView
        lateinit var adapter: LibraryGridAdapter
        lateinit var loadingPrompt: MaterialCardView
        fun updateRecyclerView(newAlbumList: List<Album>) {
            if (::libraryGridView.isInitialized) {
                val adapter = LibraryGridAdapter(newAlbumList)
                LibraryGridFragment.libraryGridView.adapter = adapter
                adapter.notifyDataSetChanged()
            }
        }

        fun dismissPrompt() {
            val materialFade = MaterialFade().apply {
                duration = 84L
            }
            TransitionManager.beginDelayedTransition(loadingPrompt, materialFade)
            loadingPrompt.visibility = View.GONE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val rootView = inflater.inflate(R.layout.fragment_library_grid, container, false)
        val layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        loadingPrompt = rootView.findViewById(R.id.loading_prompt_grid)
        libraryGridView = rootView.findViewById(R.id.library_gridview)
        libraryGridView.layoutManager = layoutManager
        adapter = LibraryGridAdapter(albumList)
        libraryGridView.adapter = adapter

        return rootView
    }

}