package org.akanework.symphonica.logic.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Entity(tableName = "history_table")
data class HistoryDataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val value: Long
)

@Entity(tableName = "playlist_table")
@TypeConverters(SongListConverters::class)
data class PlaylistDataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val name: String,
    val desc: String,
    val songs: MutableList<Long>
) {
    constructor(name: String, desc: String, songs: MutableList<Long>) : this(0, name, desc, songs)
}

class SongListConverters {
    @TypeConverter
    fun fromSongIds(songIds: MutableList<Long>): String {
        return songIds.joinToString(",")
    }

    @TypeConverter
    fun toSongIds(songIdsString: String): MutableList<Long> {
        return if (songIdsString.isEmpty()) mutableListOf() else songIdsString.split(",").map { it.toLong() }.toMutableList()
    }
}
