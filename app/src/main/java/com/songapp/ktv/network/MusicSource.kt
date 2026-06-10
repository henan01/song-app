package com.songapp.ktv.network

import java.io.OutputStream

/**
 * 一个"音乐源"需要提供的能力。
 *
 * 所有方法都是 suspend 函数，必须在 IO 上下文执行。
 */
interface MusicSource {
    val id: String              // 唯一标识，例如 "kuwo" / "catalog_xxx"
    val displayName: String     // 给用户看的名字

    suspend fun search(keyword: String, limit: Int = 30): List<TrackMeta>

    /** 把 MP3 字节流写入 [out]，返回写入字节数。失败抛 [NotPlayableException]。 */
    suspend fun downloadMp3(
        track: TrackMeta,
        out: OutputStream,
        progress: (Long, Long?) -> Unit = { _, _ -> }
    ): Long

    suspend fun fetchLyric(track: TrackMeta): String?
}

/**
 * 跨音乐源的歌曲元数据。
 *
 * [trackId] 是该音乐源内部 ID（字符串以兼容不同源的格式）。
 * 本地缓存到 DB 时，统一用 `"${sourceId}_${trackId}"` 作为主键，避免不同源 ID 撞车。
 */
data class TrackMeta(
    val sourceId: String,
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val coverUrl: String?,
    val mediaUrl: String? = null,
    val lyricUrl: String? = null
)

class NotPlayableException(msg: String) : RuntimeException(msg)
