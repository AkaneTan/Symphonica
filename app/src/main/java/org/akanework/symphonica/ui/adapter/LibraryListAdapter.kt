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

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.withContext
import org.akanework.symphonica.MainActivity.Companion.diskCacheStrategyCustom
import org.akanework.symphonica.MainActivity.Companion.musicPlayer
import org.akanework.symphonica.MainActivity.Companion.playlistViewModel
import org.akanework.symphonica.R
import org.akanework.symphonica.SymphonicaApplication
import org.akanework.symphonica.logic.data.Song
import org.akanework.symphonica.logic.util.addToNext
import org.akanework.symphonica.logic.util.changePlayer
import org.akanework.symphonica.logic.util.convertDurationToTimeStamp
import org.akanework.symphonica.logic.util.replacePlaylist
import org.akanework.symphonica.logic.util.resumePlayer
import org.akanework.symphonica.logic.util.thisSong


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

        holder.songTitle.text = songList[position].title
        holder.songMeta.text = SymphonicaApplication.context.getString(
            R.string.library_list_metadata,
            songList[position].artist,
            songList[position].album
        )
        holder.songDuration.text =
            convertDurationToTimeStamp(songList[position].duration.toString())
        holder.songUri.text = songList[position].path.toUri().toString()

        try {
            Glide.with(holder.songCover.context)
                .load(songList[position].imgUri)
                .diskCacheStrategy(diskCacheStrategyCustom)
                .placeholder(R.drawable.ic_album_default_cover)
                .into(holder.songCover)
        } catch (_: Exception) {
            // Placeholder
        }

        holder.itemView.setOnClickListener {
            playlistViewModel.currentLocation = position
            playlistViewModel.playList = songList.toMutableList()
            replacePlaylist(playlistViewModel.playList, position)
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
            addToNextButton!!.setOnClickListener {
                addToNext(songList[position])
                rootView.dismiss()
            }

            // Update MetaData.
            // TODO: Add a separate broadcast receiver for this
            val intentBroadcast = Intent("internal.play_start")
            holder.itemView.context.sendBroadcast(intentBroadcast)
            true
        }
    }

}