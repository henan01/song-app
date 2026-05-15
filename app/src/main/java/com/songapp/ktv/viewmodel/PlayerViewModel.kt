package com.songapp.ktv.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songapp.ktv.KtvApp
import com.songapp.ktv.lyrics.LrcLine
import com.songapp.ktv.lyrics.LrcParser
import com.songapp.ktv.lyrics.LyricsFetcher
import com.songapp.ktv.player.PlayerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PlayerViewModel : ViewModel() {
    private val app = KtvApp.get()
    private val player = app.player
    private val repo = app.repository

    val state: StateFlow<PlayerState> = player.state

    private val _lyrics = MutableStateFlow<List<LrcLine>>(emptyList())
    val lyrics: StateFlow<List<LrcLine>> = _lyrics.asStateFlow()

    private val _fetchingLyrics = MutableStateFlow(false)
    val fetchingLyrics: StateFlow<Boolean> = _fetchingLyrics.asStateFlow()

    /** 最后一次自动/手动搜索歌词的状态消息（成功/失败/空），UI 直接渲染。 */
    private val _lyricsStatus = MutableStateFlow<String?>(null)
    val lyricsStatus: StateFlow<String?> = _lyricsStatus.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    private var _lastSongId: String? = null
    private var _lastLrcPath: String? = null
    private val _autoTried = mutableSetOf<String>()
    private var _fetchJob: Job? = null

    init {
        viewModelScope.launch {
            state.collect { st ->
                val song = st.currentSong ?: return@collect
                val songChanged = song.id != _lastSongId
                val pathChanged = song.lrcPath != _lastLrcPath
                if (songChanged || pathChanged) {
                    _lastSongId = song.id
                    _lastLrcPath = song.lrcPath
                    _lyricsStatus.value = null
                    val parsed = withContext(Dispatchers.IO) { LrcParser.parseFile(song.lrcPath) }
                    _lyrics.value = parsed
                    if (songChanged && parsed.isEmpty() && song.id !in _autoTried) {
                        _autoTried += song.id
                        fetchLyricsOnline()
                    }
                }
            }
        }
    }

    fun togglePlay() = player.togglePlay()
    fun seek(ms: Long) = player.seekTo(ms)
    fun setVocalLevel(v: Float) = player.setVocalLevel(v)
    fun setVolume(v: Float) = player.setVolume(v)
    fun next() = player.playNext()
    fun prev() = player.playPrev()
    fun consumeToast() { _toast.value = null }

    /** 用户从文件选择器手动导入一份 .lrc / .txt 歌词。 */
    fun importLyricsFromUri(uri: Uri) {
        val song = state.value.currentSong ?: run {
            _toast.value = "请先播放一首歌"
            return
        }
        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    app.contentResolver.openInputStream(uri)?.use {
                        it.bufferedReader(Charsets.UTF_8).readText()
                    }
                } ?: throw RuntimeException("无法读取所选文件")
                val saved = withContext(Dispatchers.IO) {
                    val mp3 = File(song.mp3Path)
                    val dir = mp3.parentFile ?: app.filesDir
                    val lrcFile = File(dir, "lyrics.lrc")
                    lrcFile.writeText(text, Charsets.UTF_8)
                    lrcFile.absolutePath
                }
                repo.updateLrcPath(song.id, saved)
                player.refreshCurrentSong(song.copy(lrcPath = saved))
                val parsed = withContext(Dispatchers.IO) { LrcParser.parse(text) }
                _lyrics.value = parsed
                _lastLrcPath = saved
                if (parsed.isEmpty()) {
                    _lyricsStatus.value = "导入的文件没有时间戳，无法滚动同步"
                    _toast.value = "歌词文件没有时间戳"
                } else {
                    _lyricsStatus.value = null
                    _toast.value = "已导入 ${parsed.size} 行歌词"
                }
            } catch (e: Exception) {
                val reason = e.message ?: e.javaClass.simpleName
                _lyricsStatus.value = "导入失败：$reason"
                _toast.value = "歌词导入失败：$reason"
            }
        }
    }

    fun fetchLyricsOnline() {
        val song = state.value.currentSong ?: run {
            _toast.value = "请先播放一首歌"
            return
        }
        // 取消上一次（如果还在跑）
        _fetchJob?.cancel()
        _fetchJob = viewModelScope.launch {
            _fetchingLyrics.value = true
            _lyricsStatus.value = "正在搜索：${song.title}"
            try {
                val text = LyricsFetcher.fetch(song.title, song.artist)
                val saved = withContext(Dispatchers.IO) {
                    val mp3 = File(song.mp3Path)
                    val dir = mp3.parentFile ?: app.filesDir
                    val lrcFile = File(dir, "lyrics.lrc")
                    lrcFile.writeText(text, Charsets.UTF_8)
                    lrcFile.absolutePath
                }
                repo.updateLrcPath(song.id, saved)
                // 同步刷新播放器内 currentSong 快照，避免下一次 state 触发把刚保存的 lrcPath 又洗回 null
                player.refreshCurrentSong(song.copy(lrcPath = saved))
                val parsed = withContext(Dispatchers.IO) { LrcParser.parse(text) }
                _lyrics.value = parsed
                _lastLrcPath = saved
                if (parsed.isEmpty()) {
                    _lyricsStatus.value = "网易云返回的歌词没有时间戳"
                    _toast.value = "歌词为空"
                } else {
                    _lyricsStatus.value = null
                    _toast.value = "已下载 ${parsed.size} 行歌词"
                }
            } catch (e: Exception) {
                val reason = e.message ?: e.javaClass.simpleName
                _lyricsStatus.value = "歌词搜索失败：$reason"
                _toast.value = "歌词获取失败：$reason"
            } finally {
                _fetchingLyrics.value = false
            }
        }
    }
}
