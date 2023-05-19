/*
 *     Copyright (C) 2023 AkaneWork Organization
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.akanework.symphonica.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.akanework.symphonica.MainActivity.Companion.customFragmentManager
import org.akanework.symphonica.MainActivity.Companion.diskCacheStrategyCustom
import org.akanework.symphonica.R
import org.akanework.symphonica.logic.data.Album
import org.akanework.symphonica.ui.fragment.LibraryAlbumDisplayFragment

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

        holder.songTitle.text = song.title
        holder.songMeta.text = song.artist
        holder.songUri.text = position.toString()

        try {
            Glide.with(holder.songCover.context)
                .load(song.songList.first().imgUri)
                .diskCacheStrategy(diskCacheStrategyCustom)
                .placeholder(R.drawable.ic_album_default_cover)
                .into(holder.songCover)
        } catch (_: Exception) {

        }

        val albumBundle = Bundle().apply {
            putInt("Position", position)
        }
        val albumFragment = LibraryAlbumDisplayFragment().apply {
            arguments = albumBundle
        }

        holder.itemView.setOnClickListener {
            customFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, albumFragment)
                .addToBackStack(null)
                .commit()
        }
    }

}