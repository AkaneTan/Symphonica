package org.akanework.symphonica.logic.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import org.akanework.symphonica.logic.data.HistoryDataEntity

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