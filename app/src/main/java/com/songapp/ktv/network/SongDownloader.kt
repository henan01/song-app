package com.songapp.ktv.network

import android.content.Context
import com.songapp.ktv.data.SeparationStatus
import com.songapp.ktv.data.Song
import com.songapp.ktv.data.SongRepository
import com.songapp.ktv.search.PinyinUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * 把任一音乐源（[MusicSource]）的搜索结果（[TrackMeta]）落地为本地曲库 [Song]：
 *   1. 下载 MP3 → songs/<sourceId>_<trackId>/original.mp3（VIP 抛 [NotPlayableException]）
 *   2. 下载封面 → cover.jpg（失败容忍）
 *   3. 拉歌词 → lyrics.lrc（失败容忍）
 *   4. 写入 DB
 */
class SongDownloader(
    private val context: Context,
    private val repo: SongRepository
) {

    sealed class Progress {
        data object Starting : Progress()
        data class Downloading(val bytes: Long, val total: Long?) : Progress()
        data object Finalizing : Progress()
    }

    fun localIdOf(track: TrackMeta): String = "${track.sourceId}_${track.trackId}"

    suspend fun findExisting(track: TrackMeta): Song? = repo.getById(localIdOf(track))

    suspend fun download(
        track: TrackMeta,
        source: MusicSource,
        progress: (Progress) -> Unit = {}
    ): Song = withContext(Dispatchers.IO) {
        require(source.id == track.sourceId) { "Source mismatch: ${source.id} vs ${track.sourceId}" }
        progress(Progress.Starting)

        val id = localIdOf(track)
        val songsRoot = ensureSongsRoot()
        val dir = File(songsRoot, id).apply { mkdirs() }
        val mp3File = File(dir, "original.mp3")
        val coverFile = File(dir, "cover.jpg")
        val lrcFile = File(dir, "lyrics.lrc")

        // 1) MP3（核心，失败直接抛）
        val tmp = File(dir, "original.part")
        FileOutputStream(tmp).use { out ->
            source.downloadMp3(track, out) { read, total ->
                progress(Progress.Downloading(read, total))
            }
        }
        if (!tmp.renameTo(mp3File)) {
            tmp.copyTo(mp3File, overwrite = true)
            tmp.delete()
        }

        progress(Progress.Finalizing)

        // 2) 封面（容忍失败）
        var coverPath: String? = null
        if (!track.coverUrl.isNullOrBlank()) {
            runCatching {
                FileOutputStream(coverFile).use { out ->
                    val written = downloadHttpToStream(track.coverUrl, out)
                    if (written > 0) coverPath = coverFile.absolutePath
                }
            }
        }

        // 3) 歌词（容忍失败）
        var lrcPath: String? = null
        runCatching {
            val text = source.fetchLyric(track)
            if (!text.isNullOrBlank()) {
                lrcFile.writeText(text, Charsets.UTF_8)
                lrcPath = lrcFile.absolutePath
            }
        }

        // 4) DB
        val entity = Song(
            id = id,
            title = track.title,
            artist = track.artist.ifBlank { "未知歌手" },
            album = track.album,
            durationMs = track.durationMs,
            mp3Path = mp3File.absolutePath,
            vocalsPath = null,
            accompPath = null,
            lrcPath = lrcPath,
            coverPath = coverPath,
            separationStatus = SeparationStatus.PENDING,
            separationProgress = 0f,
            pinyinFull = PinyinUtil.fullPinyin("${track.title} ${track.artist}"),
            pinyinInitials = PinyinUtil.initials("${track.title} ${track.artist}")
        )
        repo.upsert(entity)
        entity
    }

    private fun downloadHttpToStream(url: String, out: java.io.OutputStream): Long {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = true
            connectTimeout = 6000
            readTimeout = 10000
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android)")
        }
        return try {
            if (conn.responseCode !in 200..299) return 0L
            val buf = ByteArray(32 * 1024)
            var read: Int
            var written = 0L
            conn.inputStream.use { input ->
                while (input.read(buf).also { read = it } != -1) {
                    out.write(buf, 0, read); written += read
                }
            }
            written
        } finally { conn.disconnect() }
    }

    private fun ensureSongsRoot(): File {
        val root = File(context.getExternalFilesDir(null) ?: context.filesDir, "songs")
        if (!root.exists()) root.mkdirs()
        return root
    }
}
