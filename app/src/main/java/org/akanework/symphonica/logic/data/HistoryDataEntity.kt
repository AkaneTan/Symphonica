package org.akanework.symphonica.logic.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_table")
data class HistoryDataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val value: Long
)
