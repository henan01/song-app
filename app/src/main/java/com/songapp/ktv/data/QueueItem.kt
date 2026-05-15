package com.songapp.ktv.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "queue")
data class QueueItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: String,
    val position: Int,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history")
data class SearchHistory(
    @PrimaryKey val keyword: String,
    val searchedAt: Long = System.currentTimeMillis()
)
