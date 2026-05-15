package com.songapp.ktv.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter fun toSeparationStatus(s: String): SeparationStatus = SeparationStatus.valueOf(s)
    @TypeConverter fun fromSeparationStatus(s: SeparationStatus): String = s.name
}

@Database(
    entities = [Song::class, QueueItem::class, SearchHistory::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KtvDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun queueDao(): QueueDao
    abstract fun searchDao(): SearchDao

    companion object {
        fun create(context: Context): KtvDatabase = Room
            .databaseBuilder(context.applicationContext, KtvDatabase::class.java, "ktv.db")
            .fallbackToDestructiveMigration()
            .build()
    }
}
