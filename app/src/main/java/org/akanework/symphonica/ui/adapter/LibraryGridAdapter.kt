package org.akanework.symphonica.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import org.akanework.symphonica.ui.fragment.LibraryAlbumDisplayer
import org.akanework.symphonica.MainActivity.Companion.customFragmentManager
import org.akanework.symphonica.MainActivity.Companion.diskCacheStrategyCustom
import org.akanework.symphonica.R
import org.akanework.symphonica.logic.data.Album

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

        Glide.with(holder.songCover.context)
            .load(song.songList.first().imgUri)
            .diskCacheStrategy(diskCacheStrategyCustom)
            .placeholder(R.drawable.ic_album_default_cover)
            .into(holder.songCover)

        val albumBundle = Bundle().apply {
            putInt("Position", position)
        }
        val albumFragment = LibraryAlbumDisplayer().apply {
            arguments = albumBundle
        }

        holder.itemView.setOnClickListener {
            customFragmentManager.beginTransaction()
                .replace(R.id.egfrag, albumFragment)
                .addToBackStack(null)
                .commit()
        }
    }

}