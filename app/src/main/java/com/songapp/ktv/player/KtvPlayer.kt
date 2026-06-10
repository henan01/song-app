package com.songapp.ktv.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.songapp.ktv.data.Song
import com.songapp.ktv.data.SongRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class KtvPlayer(context: Context, private val repo: SongRepository) {

    val karaoke = KaraokeAudioProcessor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private val factory = object : DefaultRenderersFactory(context) {
        override fun buildAudioSink(
            context: Context,
            enableFloatOutput: Boolean,
            enableAudioTrackPlaybackParams: Boolean
        ): AudioSink {
            // 强制 16-bit + 默认关闭 offload，把 KaraokeAudioProcessor 真正挂到音频链中
            return DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(false)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioProcessors(arrayOf<AudioProcessor>(karaoke))
                .build()
        }
    }.setEnableAudioFloatOutput(false)

    val exo: ExoPlayer = ExoPlayer.Builder(context, factory)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var positionJob: Job? = null

    init {
        exo.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(isPlaying = isPlaying)
                if (isPlaying) startPositionTicker() else stopPositionTicker()
            }
            override fun onPlaybackStateChanged(state: Int) {
                _state.value = _state.value.copy(
                    isBuffering = state == Player.STATE_BUFFERING,
                    durationMs = exo.duration.coerceAtLeast(0L)
                )
                if (state == Player.STATE_ENDED) playNext()
            }
            override fun onPlayerError(error: PlaybackException) {
                _state.value = _state.value.copy(error = error.errorCodeName)
            }
        })
    }

    fun playSong(song: Song) = runOnMain {
        playSongInternal(song)
    }

    private fun playSongInternal(song: Song) {
        val current = _state.value.currentSong
        if (current?.id == song.id && current.mp3Path == song.mp3Path) {
            // 已经是这首：保持当前进度，只确保在播放
            if (!exo.isPlaying) exo.play()
            return
        }
        _state.value = _state.value.copy(currentSong = song, error = null, positionMs = 0L)
        exo.setMediaItem(MediaItem.fromUri(song.mp3Path))
        exo.prepare()
        exo.playWhenReady = true
        scope.launch { repo.markPlayed(song.id) }
    }

    /** 当 DB 的 Song 字段（如 lrcPath、coverPath）变化后，把 state 里的快照同步刷新。 */
    fun refreshCurrentSong(updated: Song) {
        if (_state.value.currentSong?.id == updated.id) {
            _state.value = _state.value.copy(currentSong = updated)
        }
    }

    fun togglePlay() = runOnMain {
        if (exo.isPlaying) exo.pause() else exo.play()
    }

    fun seekTo(ms: Long) = runOnMain { exo.seekTo(ms) }

    fun setVocalLevel(level: Float) = runOnMain {
        karaoke.vocalLevel = level.coerceIn(0f, 1f)
        _state.value = _state.value.copy(vocalLevel = karaoke.vocalLevel)
    }

    fun setVolume(v: Float) = runOnMain {
        exo.volume = v.coerceIn(0f, 1f)
        _state.value = _state.value.copy(volume = exo.volume)
    }

    fun playNext() {
        scope.launch {
            _state.value.currentSong?.let { repo.removeSongFromQueue(it.id) }
            val q = repo.queueSnapshot()
            val nextItem = q.firstOrNull()
            if (nextItem == null) {
                stopInternal()
            } else {
                repo.getById(nextItem.songId)?.let { s -> playSongInternal(s) } ?: stopInternal()
            }
        }
    }

    fun playPrev() {
        scope.launch {
            val q = repo.queueSnapshot()
            val current = _state.value.currentSong ?: return@launch
            val idx = q.indexOfFirst { it.songId == current.id }
            if (idx > 0) repo.getById(q[idx - 1].songId)?.let { playSongInternal(it) }
        }
    }

    private fun stopInternal() {
        exo.stop()
        _state.value = _state.value.copy(
            currentSong = null,
            isPlaying = false,
            isBuffering = false,
            positionMs = 0L,
            durationMs = 0L
        )
        stopPositionTicker()
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }

    private fun startPositionTicker() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (true) {
                _state.value = _state.value.copy(
                    positionMs = exo.currentPosition.coerceAtLeast(0L),
                    durationMs = exo.duration.coerceAtLeast(0L)
                )
                delay(250)
            }
        }
    }

    private fun stopPositionTicker() {
        positionJob?.cancel()
        positionJob = null
    }

    fun release() {
        stopPositionTicker()
        scope.cancel()
        exo.release()
    }
}

data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val vocalLevel: Float = 1.0f,
    val volume: Float = 1.0f,
    val error: String? = null
)
