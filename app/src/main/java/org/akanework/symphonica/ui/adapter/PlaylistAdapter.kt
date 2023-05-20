package org.akanework.symphonica.ui.adapter

import android.content.res.Resources.Theme
import android.graphics.Color
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import org.akanework.symphonica.MainActivity.Companion.playlistViewModel
import org.akanework.symphonica.R
import org.akanework.symphonica.SymphonicaApplication
import org.akanework.symphonica.SymphonicaApplication.Companion.context
import org.akanework.symphonica.logic.data.Song
import org.akanework.symphonica.logic.util.jumpTo
import org.akanework.symphonica.logic.util.nextSong
import org.akanework.symphonica.logic.util.replacePlaylist
import org.akanework.symphonica.logic.util.thisSong


class PlaylistAdapter(private val songList: MutableList<Song>) :
    RecyclerView.Adapter<PlaylistAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val songTitle: TextView = view.findViewById(R.id.song_title)
        val songMeta: TextView = view.findViewById(R.id.song_meta)
        val dismissButton: MaterialButton = view.findViewById(R.id.playlist_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.playlist_card, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return songList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        // 设置歌曲标题和元数据
        holder.songTitle.text = songList[position].title
        holder.songMeta.text = "${songList[position].artist} - ${songList[position].album}"

        holder.itemView.setOnClickListener {
            playlistViewModel.currentLocation = holder.adapterPosition
            jumpTo(holder.adapterPosition)
        }
        holder.dismissButton.setOnClickListener {
            val buttonPosition = holder.adapterPosition
            if (buttonPosition == playlistViewModel.currentLocation) {
                thisSong()
            }
            if (buttonPosition != RecyclerView.NO_POSITION) {
                Log.d("TAGTAG", "Previous: ${playlistViewModel.playList.size}")
                // 从列表中删除对应项
                songList.removeAt(buttonPosition)
                notifyItemRemoved(buttonPosition)
                if (buttonPosition < playlistViewModel.currentLocation) {
                    playlistViewModel.currentLocation --
                }
            }
        }
    }

}