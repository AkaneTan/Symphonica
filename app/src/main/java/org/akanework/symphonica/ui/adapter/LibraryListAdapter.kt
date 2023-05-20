package org.akanework.symphonica.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import org.akanework.symphonica.MainActivity.Companion.playlistViewModel
import org.akanework.symphonica.R
import org.akanework.symphonica.SymphonicaApplication
import org.akanework.symphonica.logic.data.Song
import org.akanework.symphonica.logic.util.convertDurationToTimeStamp
import org.akanework.symphonica.logic.util.replacePlaylist

class LibraryListAdapter(private val songList: List<Song>) :
    RecyclerView.Adapter<LibraryListAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val songCover: ImageView = view.findViewById(R.id.song_cover)
        val songTitle: TextView = view.findViewById(R.id.song_title)
        val songMeta: TextView = view.findViewById(R.id.song_meta)
        val songDuration: TextView = view.findViewById(R.id.song_duration)
        val songUri: TextView = view.findViewById(R.id.song_uri)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.library_list_card, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return songList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        // 设置歌曲标题和元数据
        holder.songTitle.text = songList[position].title
        holder.songMeta.text = "${songList[position].artist} - ${songList[position].album}"
        holder.songDuration.text = convertDurationToTimeStamp(songList[position].duration.toString())
        holder.songUri.text = songList[position].path.toUri().toString()

        if (songList[position].cover == null) {
            holder.songCover.setImageResource(R.drawable.ic_album_default_cover)
        } else {
            holder.songCover.setImageDrawable(songList[position].cover)
        }

        holder.itemView.setOnClickListener {
            playlistViewModel.currentLocation = position
            playlistViewModel.playList = songList.toMutableList()
            replacePlaylist(playlistViewModel.playList, position)
        }
    }

}