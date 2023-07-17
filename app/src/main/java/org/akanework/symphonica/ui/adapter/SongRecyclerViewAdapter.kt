/*
 *     Copyright (C) 2023  Akane Foundation
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.akanework.symphonica.MainActivity
import org.akanework.symphonica.MainActivity.Companion.controllerViewModel
import org.akanework.symphonica.MainActivity.Companion.fullSheetShuffleButton
import org.akanework.symphonica.R
import org.akanework.symphonica.SymphonicaApplication
import org.akanework.symphonica.logic.data.Song
import org.akanework.symphonica.logic.util.addToNext
import org.akanework.symphonica.logic.util.broadcastMetaDataUpdate
import org.akanework.symphonica.logic.util.replacePlaylist
import org.akanework.symphonica.ui.fragment.LibraryAlbumDisplayFragment

/**
 * This is the carousel adapter used for
 * songs.
 */
class SongRecyclerViewAdapter(private val songList: MutableList<Song>) :
    RecyclerView.Adapter<SongRecyclerViewAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.home_carousel_card, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = songList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(holder.songCover.context)
            .load(songList[position].imgUri)
            .diskCacheStrategy(MainActivity.diskCacheStrategyCustom)
            .placeholder(R.drawable.ic_song_outline_default_cover)
            .into(holder.songCover)

        holder.songName.text = songList[position].title

        holder.container.setOnClickListener {
            if (controllerViewModel.shuffleState) {
                controllerViewModel.shuffleState = false
                fullSheetShuffleButton!!.isChecked = false
            }
            val dummyList: MutableList<Song> = mutableListOf()
            dummyList.addAll(songList)
            replacePlaylist(dummyList, position)
        }

        holder.itemView.setOnLongClickListener {
            val rootView = MaterialAlertDialogBuilder(
                holder.itemView.context,
                com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
            )
                .setTitle(holder.itemView.context.getString(R.string.dialog_long_press_title))
                .setView(R.layout.alert_dialog_long_press)
                .setNeutralButton(SymphonicaApplication.context.getString(R.string.dialog_song_dismiss)) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()

            val addToNextButton = rootView.findViewById<FrameLayout>(R.id.dialog_add_to_next)
            val checkAlbumButton = rootView.findViewById<FrameLayout>(R.id.dialog_check_album)

            checkAlbumButton!!.setOnClickListener {
                val albumBundle = Bundle().apply {
                    if (MainActivity.libraryViewModel.librarySortedAlbumList.isNotEmpty()) {
                        putInt("Position",
                            MainActivity.libraryViewModel.librarySortedAlbumList.indexOf(
                                MainActivity.libraryViewModel.librarySortedAlbumList.find {
                                    it.songList.contains(songList[position])
                                }
                            ))
                    } else {
                        putInt("Position", MainActivity.libraryViewModel.libraryAlbumList.indexOf(
                            MainActivity.libraryViewModel.libraryAlbumList.find {
                                it.songList.contains(songList[position])
                            }
                        ))
                    }
                }
                val albumFragment = LibraryAlbumDisplayFragment().apply {
                    arguments = albumBundle
                }

                MainActivity.customFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, albumFragment)
                    .addToBackStack(null)
                    .commit()
                rootView.dismiss()
            }

            addToNextButton!!.setOnClickListener {
                addToNext(songList[position])

                broadcastMetaDataUpdate()

                rootView.dismiss()
            }

            true
        }
    }

    /**
     * Upon creation, viewbinding everything.
     */
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val songCover: ImageView = view.findViewById(R.id.carousel_image_view)
        val container: MaterialCardView = view.findViewById(R.id.carousel_item_container)
        val songName: TextView = view.findViewById(R.id.carousel_song_name)
    }
}
