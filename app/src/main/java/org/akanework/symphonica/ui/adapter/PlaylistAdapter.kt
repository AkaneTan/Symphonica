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

import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import org.akanework.symphonica.MainActivity.Companion.playlistViewModel
import org.akanework.symphonica.R
import org.akanework.symphonica.logic.data.Song
import org.akanework.symphonica.logic.util.jumpTo
import org.akanework.symphonica.logic.util.thisSong
import org.akanework.symphonica.ui.component.PlaylistBottomSheet.Companion.updatePlaylistSheetLocation

/**
 * [PlaylistAdapter] is the adapter
 * used for playlist bottom sheet.
 */
class PlaylistAdapter(private val songList: MutableList<Song>) :
    RecyclerView.Adapter<PlaylistAdapter.ViewHolder>() {

    /**
     * Upon creation, viewbinding everything.
     */
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val songTitle: TextView = view.findViewById(R.id.song_title)
        val songMeta: TextView = view.findViewById(R.id.song_meta)
        val dismissButton: MaterialButton = view.findViewById(R.id.playlist_delete)
        val activeBackground: ImageView = view.findViewById(R.id.active_background)
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

        holder.songTitle.text = songList[position].title
        "${songList[position].artist} - ${songList[position].album}".also { holder.songMeta.text = it }

        holder.itemView.setOnClickListener {
            val previousLocation = playlistViewModel.currentLocation
            playlistViewModel.currentLocation = holder.adapterPosition
            jumpTo(holder.adapterPosition)
            updatePlaylistSheetLocation(previousLocation)
        }
        holder.dismissButton.setOnClickListener {
            if (playlistViewModel.playList.size != 1) {
                val buttonPosition = holder.adapterPosition
                if (buttonPosition == playlistViewModel.currentLocation) {
                    thisSong()
                }
                if (buttonPosition != RecyclerView.NO_POSITION) {
                    // Delete the item.
                    songList.removeAt(buttonPosition)
                    notifyItemRemoved(buttonPosition)
                    if (buttonPosition < playlistViewModel.currentLocation) {
                        playlistViewModel.currentLocation--
                    }
                }
            }
        }

        if (holder.adapterPosition == playlistViewModel.currentLocation) {
            holder.songTitle.setTextColor(
                MaterialColors.getColor(
                    holder.itemView,
                    com.google.android.material.R.attr.colorOnPrimaryContainer
                )
            )
            holder.songMeta.setTextColor(
                MaterialColors.getColor(
                    holder.itemView,
                    com.google.android.material.R.attr.colorOnPrimaryContainer
                )
            )
            holder.activeBackground.visibility = VISIBLE
        } else {
            holder.songTitle.setTextColor(
                MaterialColors.getColor(
                    holder.itemView,
                    com.google.android.material.R.attr.colorOnSurface
                )
            )
            holder.songMeta.setTextColor(
                MaterialColors.getColor(
                    holder.itemView,
                    com.google.android.material.R.attr.colorOnSurfaceVariant
                )
            )
            holder.activeBackground.visibility = GONE
        }
    }

}