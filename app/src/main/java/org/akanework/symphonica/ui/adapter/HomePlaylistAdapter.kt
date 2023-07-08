/*
 *     Copyright (C) 2023 Akane Foundation
 *
 *     This file is part of Symphonica.
 *
 *     Symphonica is free software: you can redistribute it and/or modify it under the terms
 *     of the GNU General Public License as published by the Free Software Foundation,
 *     either version 3 of the License, or (at your option) any later version.
 *
 *     Symphonica is distributed in the hope that it will be useful, but WITHOUT ANY
 *     WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 *     FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along with
 *     Symphonica. If not, see <https://www.gnu.org/licenses/>.
 */

package org.akanework.symphonica.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.symphonica.MainActivity
import org.akanework.symphonica.MainActivity.Companion.playlistViewModel
import org.akanework.symphonica.R
import org.akanework.symphonica.SymphonicaApplication
import org.akanework.symphonica.logic.data.PlaylistDataEntity
import org.akanework.symphonica.logic.data.Song
import org.akanework.symphonica.logic.util.addToNext
import org.akanework.symphonica.logic.util.broadcastMetaDataUpdate
import org.akanework.symphonica.logic.util.convertDurationToTimeStamp
import org.akanework.symphonica.logic.util.replacePlaylist
import org.akanework.symphonica.logic.util.thisSong
import org.akanework.symphonica.ui.fragment.LibraryAlbumDisplayFragment

/**
 * [HomePlaylistAdapter] is used for displaying song lists
 * inside an album. Used in library album display fragment.
 */
class HomePlaylistAdapter(private val playlistList: List<PlaylistDataEntity>) :
    RecyclerView.Adapter<HomePlaylistAdapter.ViewHolder>() {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    /**
     * Upon creation, viewbinding everything.
     */
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val playlistTitle: TextView = view.findViewById(R.id.playlist_title)
        val playlistDesc: TextView = view.findViewById(R.id.playlist_desc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.home_playlist_card, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return playlistList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.playlistTitle.text = playlistList[position].name
        if (playlistList[position].desc.isNotEmpty()) {
            holder.playlistDesc.text = playlistList[position].desc
        }

        holder.itemView.setOnClickListener {
        }

        holder.itemView.setOnLongClickListener {
            val adapterPosition = holder.adapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                // Delete the item.
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        playlistViewModel.deletePlaylist(playlistViewModel.playlistList[adapterPosition])
                        withContext(Dispatchers.Main) {
                            playlistViewModel.playlistList.removeAt(adapterPosition)
                        }
                    }
                }
                notifyItemRemoved(adapterPosition)
            }
            true
        }
    }

}