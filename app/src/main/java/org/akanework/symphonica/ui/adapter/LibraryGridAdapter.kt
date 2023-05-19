package org.akanework.symphonica.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import org.akanework.symphonica.MainActivity.Companion.albumList
import org.akanework.symphonica.R
import org.akanework.symphonica.SymphonicaApplication
import org.akanework.symphonica.logic.util.Album
import org.akanework.symphonica.logic.util.Song
import org.akanework.symphonica.logic.util.convertDurationToTimeStamp

class LibraryGridAdapter(private val albumList: List<Album>) :
    RecyclerView.Adapter<LibraryGridAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val songCover: ImageView = view.findViewById(R.id.song_cover)
        val songTitle: TextView = view.findViewById(R.id.song_title)
        val songMeta: TextView = view.findViewById(R.id.song_meta)
        val songUri: TextView = view.findViewById(R.id.song_uri)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.library_grid_card, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return albumList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = albumList[position]

        // 设置歌曲标题和元数据
        holder.songTitle.text = song.title
        holder.songMeta.text = song.artist
        holder.songUri.text = position.toString()
        if (song.cover == null) {
            holder.songCover.setImageDrawable(AppCompatResources.getDrawable(SymphonicaApplication.context, R.drawable.ic_album_default_cover))
        } else {
            holder.songCover.setImageDrawable(song.cover)
        }
    }

}