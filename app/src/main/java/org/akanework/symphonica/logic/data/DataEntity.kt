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

package org.akanework.symphonica.logic.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

/**
 * @property id
 * @property value
 */
@Entity(tableName = "history_table")
data class HistoryDataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val value: Long
)

/**
 * @property id
 * @property name
 * @property desc
 * @property songs
 */
@Entity(tableName = "playlist_table")
@TypeConverters(SongListConverters::class)
data class PlaylistDataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val name: String,
    val desc: String,
    var songs: MutableList<Long>
) {
    constructor(
        name: String,
        desc: String,
        songs: MutableList<Long>
    ) : this(
        0,
        name,
        desc,
        songs
    )
}

class SongListConverters {
    /**
     * @param songIds
     * @return
     */
    @TypeConverter
    fun fromSongIds(songIds: MutableList<Long>): String = songIds.joinToString(",")

    /**
     * @param songIdsString
     * @return
     */
    @TypeConverter
    fun toSongIds(songIdsString: String): MutableList<Long> = if (songIdsString.isEmpty()) {
        mutableListOf()
    } else {
        songIdsString.split(",").map { it.toLong() }
            .toMutableList()
    }
}
