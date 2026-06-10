package com.songapp.ktv.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

class SongRepository(
    private val songDao: SongDao,
    private val queueDao: QueueDao,
    private val searchDao: SearchDao
) {

    fun page(filter: SongFilter, sort: SongSort): Flow<PagingData<Song>> =
        Pager(PagingConfig(pageSize = 50, enablePlaceholders = false)) {
            songDao.pagingAll(filter.name, sort.name)
        }.flow

    fun searchPage(query: String): Flow<PagingData<Song>> =
        Pager(PagingConfig(pageSize = 50, enablePlaceholders = false)) {
            songDao.pagingSearch(query)
        }.flow

    suspend fun upsert(song: Song) = songDao.upsert(song)
    suspend fun upsertAll(songs: List<Song>) = songDao.upsertAll(songs)
    suspend fun delete(id: String) = songDao.delete(id)
    suspend fun toggleFavorite(song: Song) = songDao.setFavorite(song.id, !song.isFavorite)
    suspend fun markPlayed(id: String) = songDao.markPlayed(id, System.currentTimeMillis())
    suspend fun getById(id: String): Song? = songDao.getById(id)
    suspend fun recentSongs(limit: Int = 200): List<Song> = songDao.recentSongs(limit)
    suspend fun searchSongs(query: String, limit: Int = 80): List<Song> =
        if (query.isBlank()) songDao.recentSongs(limit) else songDao.searchSongs(query, limit)
    fun observe(id: String): Flow<Song?> = songDao.observe(id)
    fun count(): Flow<Int> = songDao.countFlow()
    suspend fun updateLrcPath(id: String, path: String?) = songDao.updateLrcPath(id, path)

    // Queue
    fun queueFlow(): Flow<List<QueueRow>> = queueDao.observeAll()
    suspend fun enqueue(songId: String) {
        queueDao.insert(QueueItem(songId = songId, position = queueDao.nextPosition()))
    }
    suspend fun removeFromQueue(id: Long) = queueDao.remove(id)
    suspend fun removeSongFromQueue(songId: String) = queueDao.removeBySongId(songId)
    suspend fun moveQueueItemToTop(id: Long) = queueDao.updatePosition(id, queueDao.topPosition())
    suspend fun clearQueue() = queueDao.clear()
    suspend fun queueSnapshot() = queueDao.snapshot()

    // Search history
    fun recentSearches(): Flow<List<SearchHistory>> = searchDao.recent()
    suspend fun rememberSearch(keyword: String) {
        if (keyword.isNotBlank()) searchDao.add(SearchHistory(keyword.trim()))
    }
}
