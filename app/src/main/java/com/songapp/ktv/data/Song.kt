package com.songapp.ktv.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SeparationStatus { PENDING, RUNNING, DONE, FAILED }

@Entity(
    tableName = "songs",
    indices = [
        Index(value = ["title"]),
        Index(value = ["artist"]),
        Index(value = ["pinyinInitials"]),
        Index(value = ["isFavorite"]),
        Index(value = ["createdAt"])
    ]
)
data class Song(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val mp3Path: String,
    val vocalsPath: String? = null,
    val accompPath: String? = null,
    val lrcPath: String? = null,
    val coverPath: String? = null,
    val separationStatus: SeparationStatus = SeparationStatus.PENDING,
    val separationProgress: Float = 0f,
    val pinyinInitials: String = "",
    val pinyinFull: String = "",
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastPlayedAt: Long = 0L,
    val playCount: Int = 0
)
