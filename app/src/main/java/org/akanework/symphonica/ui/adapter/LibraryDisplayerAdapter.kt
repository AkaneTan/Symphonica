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
import org.akanework.symphonica.MainActivity
import org.akanework.symphonica.R
import org.akanework.symphonica.SymphonicaApplication
import org.akanework.symphonica.logic.data.Song
import org.akanework.symphonica.logic.util.convertDurationToTimeStamp
import org.akanework.symphonica.logic.util.getTrackNumber

class LibraryDisplayerAdapter(private val songList: List<Song>) :
    RecyclerView.Adapter<LibraryDisplayerAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val songTitle: TextView = view.findViewById(R.id.song_title)
        val songDuration: TextView = view.findViewById(R.id.song_duration)
        val songTrackNumber: TextView = view.findViewById(R.id.track_number)
        val songUri: TextView = view.findViewById(R.id.song_uri)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.library_displayer_card, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return songList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        // 设置歌曲标题和元数据
        holder.songTitle.text = songList[position].title
        holder.songDuration.text = convertDurationToTimeStamp(songList[position].duration.toString())
        holder.songUri.text = songList[position].path.toUri().toString()
        val trackNumber = getTrackNumber(songList[position].path)
        if (trackNumber != null) {
            holder.songTrackNumber.text = trackNumber
        }

        holder.itemView.setOnClickListener {
            MainActivity.playlistViewModel.currentLocation = position
            MainActivity.playlistViewModel.playList = songList
        }
    }

}