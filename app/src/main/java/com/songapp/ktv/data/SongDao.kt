package com.songapp.ktv.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

enum class SongSort { RECENT, TITLE, ARTIST, MOST_PLAYED }
enum class SongFilter { ALL, SEPARATED, PENDING, FAVORITE }

@Dao
interface SongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(song: Song)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun upsertAll(songs: List<Song>)

    @Update
    suspend fun update(song: Song)

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getById(id: String): Song?

    @Query("SELECT * FROM songs WHERE id = :id")
    fun observe(id: String): Flow<Song?>

    @Query("SELECT COUNT(*) FROM songs")
    fun countFlow(): Flow<Int>

    @Query("SELECT * FROM songs ORDER BY createdAt DESC LIMIT :limit")
    suspend fun recentSongs(limit: Int = 200): List<Song>

    @Query("""
        SELECT * FROM songs
        WHERE title    LIKE '%' || :q || '%'
           OR artist   LIKE '%' || :q || '%'
           OR album    LIKE '%' || :q || '%'
           OR pinyinInitials LIKE '%' || :q || '%'
           OR pinyinFull     LIKE '%' || :q || '%'
        ORDER BY createdAt DESC
        LIMIT :limit
    """)
    suspend fun searchSongs(q: String, limit: Int = 80): List<Song>

    @Query("""
        SELECT * FROM songs
        WHERE (:filter = 'ALL')
           OR (:filter = 'SEPARATED' AND separationStatus = 'DONE')
           OR (:filter = 'PENDING'   AND separationStatus != 'DONE')
           OR (:filter = 'FAVORITE'  AND isFavorite = 1)
        ORDER BY
          CASE WHEN :sort = 'RECENT'       THEN createdAt END DESC,
          CASE WHEN :sort = 'TITLE'        THEN title END ASC,
          CASE WHEN :sort = 'ARTIST'       THEN artist END ASC,
          CASE WHEN :sort = 'MOST_PLAYED'  THEN playCount END DESC
    """)
    fun pagingAll(filter: String, sort: String): PagingSource<Int, Song>

    @Query("""
        SELECT * FROM songs
        WHERE title    LIKE '%' || :q || '%'
           OR artist   LIKE '%' || :q || '%'
           OR album    LIKE '%' || :q || '%'
           OR pinyinInitials LIKE '%' || :q || '%'
           OR pinyinFull     LIKE '%' || :q || '%'
        ORDER BY createdAt DESC
    """)
    fun pagingSearch(q: String): PagingSource<Int, Song>

    @Query("UPDATE songs SET isFavorite = :fav WHERE id = :id")
    suspend fun setFavorite(id: String, fav: Boolean)

    @Query("UPDATE songs SET lastPlayedAt = :ts, playCount = playCount + 1 WHERE id = :id")
    suspend fun markPlayed(id: String, ts: Long)

    @Query("UPDATE songs SET separationStatus = :status, separationProgress = :progress WHERE id = :id")
    suspend fun updateSeparation(id: String, status: SeparationStatus, progress: Float)

    @Query("UPDATE songs SET lrcPath = :path WHERE id = :id")
    suspend fun updateLrcPath(id: String, path: String?)
}

@Dao
interface QueueDao {
    @Query("SELECT q.*, s.title as title, s.artist as artist, s.album as album, s.coverPath as coverPath, s.durationMs as durationMs FROM queue q JOIN songs s ON q.songId = s.id ORDER BY q.position ASC")
    fun observeAll(): Flow<List<QueueRow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: QueueItem): Long

    @Query("SELECT IFNULL(MAX(position), -1) + 1 FROM queue")
    suspend fun nextPosition(): Int

    @Query("DELETE FROM queue WHERE id = :id")
    suspend fun remove(id: Long)

    @Query("DELETE FROM queue WHERE songId = :songId")
    suspend fun removeBySongId(songId: String)

    @Query("DELETE FROM queue")
    suspend fun clear()

    @Query("UPDATE queue SET position = :pos WHERE id = :id")
    suspend fun updatePosition(id: Long, pos: Int)

    @Query("SELECT IFNULL(MIN(position), 0) - 1 FROM queue")
    suspend fun topPosition(): Int

    @Query("SELECT * FROM queue ORDER BY position ASC")
    suspend fun snapshot(): List<QueueItem>
}

data class QueueRow(
    val id: Long,
    val songId: String,
    val position: Int,
    val addedAt: Long,
    val title: String,
    val artist: String,
    val album: String,
    val coverPath: String?,
    val durationMs: Long
)

@Dao
interface SearchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(history: SearchHistory)

    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT 10")
    fun recent(): Flow<List<SearchHistory>>

    @Query("DELETE FROM search_history")
    suspend fun clear()
}
