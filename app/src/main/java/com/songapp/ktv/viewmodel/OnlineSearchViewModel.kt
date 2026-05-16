package com.songapp.ktv.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songapp.ktv.KtvApp
import com.songapp.ktv.network.MusicSource
import com.songapp.ktv.network.NotPlayableException
import com.songapp.ktv.network.SongDownloader
import com.songapp.ktv.network.TrackMeta
import com.songapp.ktv.network.source.KuwoSource
import com.songapp.ktv.network.source.NetEaseSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnlineSearchViewModel : ViewModel() {

    private val app = KtvApp.get()
    private val downloader = SongDownloader(app, app.repository)

    /** 所有已知音乐源（按显示顺序）。*/
    val allSources: List<MusicSource> = listOf(NetEaseSource, KuwoSource)

    private val sourceById: Map<String, MusicSource> = allSources.associateBy { it.id }

    private val _enabledSources = MutableStateFlow(allSources.map { it.id }.toSet())
    val enabledSources: StateFlow<Set<String>> = _enabledSources.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching.asStateFlow()

    private val _results = MutableStateFlow<List<TrackMeta>>(emptyList())
    val results: StateFlow<List<TrackMeta>> = _results.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    /** 正在下载的本地 ID（${sourceId}_${trackId}）。 */
    private val _downloading = MutableStateFlow<Set<String>>(emptySet())
    val downloading: StateFlow<Set<String>> = _downloading.asStateFlow()

    /** 已下载到本地曲库的本地 ID。 */
    private val _downloaded = MutableStateFlow<Set<String>>(emptySet())
    val downloaded: StateFlow<Set<String>> = _downloaded.asStateFlow()

    /** 下载进度：本地 ID -> 0~1。 */
    private val _progress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val progress: StateFlow<Map<String, Float>> = _progress.asStateFlow()

    fun setQuery(q: String) { _query.value = q }
    fun consumeToast() { _toast.value = null }

    fun toggleSource(sourceId: String) {
        val cur = _enabledSources.value
        _enabledSources.value = if (sourceId in cur) {
            if (cur.size == 1) cur else cur - sourceId  // 至少保留一个
        } else cur + sourceId
    }

    fun search() {
        val keyword = _query.value.trim()
        if (keyword.isBlank()) return
        val enabled = allSources.filter { it.id in _enabledSources.value }
        if (enabled.isEmpty()) return
        viewModelScope.launch {
            _searching.value = true
            try {
                val deferred = enabled.map { src ->
                    async {
                        runCatching { src.search(keyword, limit = 25) }
                            .onFailure { _toast.value = "${src.displayName} 搜索失败：${it.message ?: it.javaClass.simpleName}" }
                            .getOrDefault(emptyList())
                    }
                }
                val merged = deferred.awaitAll().flatten()
                _results.value = merged
                refreshDownloadedSet(merged)
                if (merged.isEmpty()) _toast.value = "没搜到任何结果"
            } finally {
                _searching.value = false
            }
        }
    }

    private suspend fun refreshDownloadedSet(list: List<TrackMeta>) {
        val present = list.mapNotNull {
            if (downloader.findExisting(it) != null) downloader.localIdOf(it) else null
        }.toSet()
        _downloaded.value = present
    }

    fun download(track: TrackMeta) {
        val source = sourceById[track.sourceId] ?: run {
            _toast.value = "未知音乐源：${track.sourceId}"
            return
        }
        val localId = downloader.localIdOf(track)
        if (localId in _downloading.value || localId in _downloaded.value) return
        viewModelScope.launch {
            _downloading.value = _downloading.value + localId
            _progress.value = _progress.value + (localId to 0f)
            try {
                downloader.download(track, source) { p ->
                    if (p is SongDownloader.Progress.Downloading) {
                        val ratio = p.total?.let { if (it > 0) p.bytes.toFloat() / it else 0f } ?: 0f
                        _progress.value = _progress.value + (localId to ratio.coerceIn(0f, 1f))
                    }
                }
                _downloaded.value = _downloaded.value + localId
                _toast.value = "已下载：${track.title}"
            } catch (e: NotPlayableException) {
                _toast.value = "${track.title}：VIP 歌曲或无法下载"
            } catch (e: Exception) {
                _toast.value = "下载失败：${e.message ?: e.javaClass.simpleName}"
            } finally {
                _downloading.value = _downloading.value - localId
                _progress.value = _progress.value - localId
            }
        }
    }

    fun playDownloaded(track: TrackMeta) {
        viewModelScope.launch {
            val local = downloader.findExisting(track) ?: return@launch
            app.player.playSong(local)
        }
    }
}
