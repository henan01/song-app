package com.songapp.ktv.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.songapp.ktv.KtvApp
import com.songapp.ktv.data.Song
import com.songapp.ktv.data.SongFilter
import com.songapp.ktv.data.SongSort
import com.songapp.ktv.importer.SongImporter
import com.songapp.ktv.player.PlayerState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.net.Uri

class LibraryViewModel : ViewModel() {
    private val app = KtvApp.get()
    private val repo = app.repository
    private val importer = SongImporter(app, repo)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filter = MutableStateFlow(SongFilter.ALL)
    val filter: StateFlow<SongFilter> = _filter.asStateFlow()

    private val _sort = MutableStateFlow(SongSort.RECENT)
    val sort: StateFlow<SongSort> = _sort.asStateFlow()

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    val totalCount: StateFlow<Int> = run {
        val s = MutableStateFlow(0)
        viewModelScope.launch { repo.count().collect { s.value = it } }
        s
    }

    val nowPlaying: StateFlow<PlayerState> = app.player.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerState())

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val songs: Flow<PagingData<Song>> =
        combine(_query.debounce(150), _filter, _sort) { q, f, s -> Triple(q, f, s) }
            .flatMapLatest { (q, f, s) ->
                if (q.isBlank()) repo.page(f, s) else repo.searchPage(q.trim().lowercase())
            }
            .cachedIn(viewModelScope)

    fun setQuery(q: String) { _query.value = q }
    fun setFilter(f: SongFilter) { _filter.value = f }
    fun setSort(s: SongSort) { _sort.value = s }
    fun consumeToast() { _toast.value = null }

    fun importUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _importing.value = true
            val n = importer.importMany(uris)
            _importing.value = false
            _toast.value = if (n > 0) "成功导入 $n 首歌" else "导入失败，请重试"
        }
    }

    fun toggleFavorite(song: Song) = viewModelScope.launch { repo.toggleFavorite(song) }
    fun delete(song: Song) = viewModelScope.launch { repo.delete(song.id) }
    fun enqueue(song: Song) = viewModelScope.launch {
        repo.enqueue(song.id); _toast.value = "已加入点歌单"
    }
    fun togglePlayPause() = app.player.togglePlay()

    fun rememberSearch() = viewModelScope.launch {
        if (_query.value.isNotBlank()) repo.rememberSearch(_query.value)
    }
}
