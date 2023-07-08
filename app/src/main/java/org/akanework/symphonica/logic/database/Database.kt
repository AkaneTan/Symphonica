package org.akanework.symphonica.logic.database

import androidx.room.Database
import androidx.room.RoomDatabase
import org.akanework.symphonica.logic.dao.HistoryDao
import org.akanework.symphonica.logic.dao.PlaylistDao
import org.akanework.symphonica.logic.data.HistoryDataEntity
import org.akanework.symphonica.logic.data.PlaylistDataEntity

@Database(entities = [HistoryDataEntity::class], version = 1, exportSchema = true)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}

@Database(entities = [PlaylistDataEntity::class], version = 1, exportSchema = true)
abstract class PlaylistDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
}
