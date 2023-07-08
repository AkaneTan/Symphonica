package org.akanework.symphonica.logic.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import org.akanework.symphonica.logic.data.HistoryDataEntity
import org.akanework.symphonica.logic.data.PlaylistDataEntity

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history_table")
    fun getAllItems(): List<HistoryDataEntity>

    @Insert
    fun insertItem(item: HistoryDataEntity)

    @Delete
    fun deleteItem(item: HistoryDataEntity)

    @Query("DELETE FROM history_table")
    suspend fun clearHistoryItems()
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlist_table")
    suspend fun getAllPlaylists(): List<PlaylistDataEntity>

    @Insert
    suspend fun createPlaylist(playlist: PlaylistDataEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistDataEntity)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistDataEntity)
}
