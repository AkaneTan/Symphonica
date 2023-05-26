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
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.carousel.MaskableFrameLayout
import org.akanework.symphonica.MainActivity
import org.akanework.symphonica.MainActivity.Companion.booleanViewModel
import org.akanework.symphonica.MainActivity.Companion.fullSheetShuffleButton
import org.akanework.symphonica.MainActivity.Companion.playlistViewModel
import org.akanework.symphonica.R
import org.akanework.symphonica.logic.data.Song
import org.akanework.symphonica.logic.util.replacePlaylist

class SongCarouselAdapter(private val songList: MutableList<Song>) :
    RecyclerView.Adapter<SongCarouselAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val songCover: ImageView = view.findViewById(R.id.carousel_image_view)
        val container: MaskableFrameLayout = view.findViewById(R.id.carousel_item_container)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.home_carousel_card, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return songList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(holder.songCover.context)
            .load(songList[position].imgUri)
            .diskCacheStrategy(MainActivity.diskCacheStrategyCustom)
            .placeholder(R.drawable.ic_song_outline_default_cover)
            .into(holder.songCover)

        val tempSongList: MutableList<Song> = mutableListOf()
        tempSongList.addAll(songList)
        holder.container.setOnClickListener {
            playlistViewModel.currentLocation = holder.adapterPosition
            playlistViewModel.playList = tempSongList
            if (booleanViewModel.shuffleState) {
                booleanViewModel.shuffleState = false
                fullSheetShuffleButton!!.isChecked = false
            }
            replacePlaylist(playlistViewModel.playList, position)
        }

    }

}