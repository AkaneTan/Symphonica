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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.akanework.symphonica.R
import org.akanework.symphonica.logic.data.Lyric

/**
 * [LyricAdapter] is the lyric adapter.
 */
class LyricAdapter(private val lyric: Lyric) :
    RecyclerView.Adapter<LyricAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.lyric_card, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = lyric.lines.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.lyric.text = lyric.lines[position]
    }

    /**
     * Upon creation, viewbinding everything.
     */
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val lyric: TextView = view.findViewById(R.id.lyric_content)
    }
}
