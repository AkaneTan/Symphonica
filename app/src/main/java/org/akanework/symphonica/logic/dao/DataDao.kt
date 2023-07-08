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
    /**
     * @return
     */
    @Query("SELECT * FROM history_table")
    fun getAllItems(): List<HistoryDataEntity>

    /**
     * @param item
     */
    @Insert
    fun insertItem(item: HistoryDataEntity)

    /**
     * @param item
     */
    @Delete
    fun deleteItem(item: HistoryDataEntity)

    @Query("DELETE FROM history_table")
    suspend fun clearHistoryItems()
}

@Dao
interface PlaylistDao {
    /**
     * @return
     */
    @Query("SELECT * FROM playlist_table")
    suspend fun getAllPlaylists(): List<PlaylistDataEntity>

    /**
     * @param playlist
     * @return
     */
    @Insert
    suspend fun createPlaylist(playlist: PlaylistDataEntity): Long

    /**
     * @param playlist
     */
    @Update
    suspend fun updatePlaylist(playlist: PlaylistDataEntity)

    /**
     * @param playlist
     */
    @Delete
    suspend fun deletePlaylist(playlist: PlaylistDataEntity)
}
