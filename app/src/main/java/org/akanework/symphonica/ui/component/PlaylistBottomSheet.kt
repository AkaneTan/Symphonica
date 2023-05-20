package org.akanework.symphonica.ui.component

import android.content.res.Resources.Theme
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.akanework.symphonica.MainActivity.Companion.playlistViewModel
import org.akanework.symphonica.R
import org.akanework.symphonica.SymphonicaApplication
import org.akanework.symphonica.ui.adapter.PlaylistAdapter


class PlaylistBottomSheet : BottomSheetDialogFragment() {


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = inflater.inflate(R.layout.playlist_bottom_sheet, container, false)
        playlistView = rootView.findViewById(R.id.playlist_recyclerview)
        playlistView.layoutManager = LinearLayoutManager(SymphonicaApplication.context)
        playlistView.adapter = PlaylistAdapter(playlistViewModel.playList)
        scrollToCurrent()
        return rootView
    }

    companion object {
        const val TAG = "PlaylistBottomSheet"
        lateinit var playlistView: RecyclerView
        fun deletePlaylistEntry(index: Int) {
            playlistView.adapter?.notifyItemRemoved(index)
        }
        fun scrollToCurrent() {
            playlistView.layoutManager!!.scrollToPosition(playlistViewModel.currentLocation)
        }
    }
}