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

package org.akanework.symphonica.logic.database

import androidx.room.Database
import androidx.room.RoomDatabase
import org.akanework.symphonica.logic.dao.HistoryDao
import org.akanework.symphonica.logic.dao.PlaylistDao
import org.akanework.symphonica.logic.data.HistoryDataEntity
import org.akanework.symphonica.logic.data.PlaylistDataEntity

@Database(
    entities = [HistoryDataEntity::class],
    version = 1,
    exportSchema = true
)
abstract class HistoryDatabase : RoomDatabase() {
    /**
     * @return
     */
    abstract fun historyDao(): HistoryDao
}

@Database(
    entities = [PlaylistDataEntity::class],
    version = 1,
    exportSchema = true
)
abstract class PlaylistDatabase : RoomDatabase() {
    /**
     * @return
     */
    abstract fun playlistDao(): PlaylistDao
}
