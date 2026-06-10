package com.songapp.ktv.network.source

import android.util.Log
import com.songapp.ktv.network.MusicSource
import com.songapp.ktv.network.NotPlayableException
import com.songapp.ktv.network.TrackMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * 用户自备歌库订阅。
 *
 * 支持两种 JSON：
 * 1) {"name":"家庭歌库","tracks":[{"id":"1","title":"晴天","artist":"周杰伦","mediaUrl":"https://.../晴天.mp3","lyricUrl":"https://.../晴天.lrc"}]}
 * 2) [{"id":"1","title":"晴天","artist":"周杰伦","mediaUrl":"https://.../晴天.mp3","lyricUrl":"https://.../晴天.lrc"}]
 */
class HttpCatalogSource(
    private val config: UserSourceConfig
) : MusicSource {
    override val id: String = config.id
    override val displayName: String = config.name

    override suspend fun search(keyword: String, limit: Int): List<TrackMeta> = withContext(Dispatchers.IO) {
        val tracks = fetchCatalog()
        val q = keyword.trim().lowercase()
        if (q.isBlank()) tracks.take(limit) else tracks.filter {
            "${it.title} ${it.artist} ${it.album}".lowercase().contains(q)
        }.take(limit)
    }

    override suspend fun downloadMp3(
        track: TrackMeta,
        out: OutputStream,
        progress: (Long, Long?) -> Unit
    ): Long = withContext(Dispatchers.IO) {
        val url = track.mediaUrl ?: throw NotPlayableException("歌源记录缺少 mediaUrl")
        download(url, out, progress)
    }

    override suspend fun fetchLyric(track: TrackMeta): String? = withContext(Dispatchers.IO) {
        val url = track.lyricUrl ?: return@withContext null
        httpGet(url)
    }

    private fun fetchCatalog(): List<TrackMeta> {
        val body = config.inlineJson ?: httpGet(config.catalogUrl) ?: return emptyList()
        return runCatching {
            val trimmed = body.trim()
            val obj = if (trimmed.startsWith("[")) null else JSONObject(trimmed)
            val arr = if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else {
                obj?.optJSONArray("tracks") ?: JSONArray()
            }
            val baseUrl = obj?.optString("baseUrl")?.takeIf { it.isNotBlank() }
                ?: config.catalogUrl.substringBeforeLast('/', missingDelimiterValue = "").takeIf { it.startsWith("http") }?.plus("/")
            parseTracks(arr, baseUrl)
        }.getOrElse {
            Log.w(TAG, "parse catalog fail: ${it.message}")
            emptyList()
        }
    }

    private fun parseTracks(arr: JSONArray, baseUrl: String?): List<TrackMeta> = buildList {
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val mediaUrl = obj.optString("mediaUrl")
                .ifBlank { obj.optString("mp3Url") }
                .ifBlank { obj.optString("audioUrl") }
                .takeIf { it.isNotBlank() }
                ?.let { resolveUrl(baseUrl, it) } ?: continue
            val title = obj.optString("title").ifBlank { obj.optString("name") }.takeIf { it.isNotBlank() } ?: continue
            val artist = obj.optString("artist").ifBlank { obj.optString("singer") }.ifBlank { "未知歌手" }
            val rawId = obj.optString("id").ifBlank { mediaUrl.hashCode().toUInt().toString(16) }
            add(
                TrackMeta(
                    sourceId = id,
                    trackId = rawId,
                    title = title,
                    artist = artist,
                    album = obj.optString("album"),
                    durationMs = obj.optLong("durationMs", obj.optLong("duration", 0L) * 1000L),
                    coverUrl = obj.optString("coverUrl").ifBlank { obj.optString("cover") }
                        .takeIf { it.isNotBlank() }
                        ?.let { resolveUrl(baseUrl, it) },
                    mediaUrl = mediaUrl,
                    lyricUrl = obj.optString("lyricUrl").ifBlank { obj.optString("lrcUrl") }
                        .takeIf { it.isNotBlank() }
                        ?.let { resolveUrl(baseUrl, it) }
                )
            )
        }
    }

    private fun resolveUrl(baseUrl: String?, value: String): String {
        if (value.startsWith("http://") || value.startsWith("https://")) return value
        val base = baseUrl ?: return value
        return URL(URL(base), value).toString()
    }

    private fun download(url: String, out: OutputStream, progress: (Long, Long?) -> Unit): Long {
        val conn = open(url)
        return try {
            val code = conn.responseCode
            if (code !in 200..299) throw NotPlayableException("HTTP $code")
            val total = conn.contentLengthLong.takeIf { it > 0 }
            val buf = ByteArray(64 * 1024)
            var written = 0L
            conn.inputStream.use { input ->
                var read: Int
                while (input.read(buf).also { read = it } != -1) {
                    out.write(buf, 0, read)
                    written += read
                    progress(written, total)
                }
            }
            if (written <= 0) throw NotPlayableException("空文件")
            written
        } finally {
            conn.disconnect()
        }
    }

    private fun httpGet(url: String): String? {
        val conn = open(url)
        return try {
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "GET fail: ${e.message}")
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = true
            connectTimeout = 8000
            readTimeout = 30000
            setRequestProperty("User-Agent", "SongKtv/0.1 Android")
            setRequestProperty("Accept", "*/*")
        }

    companion object {
        private const val TAG = "HttpCatalogSource"
    }
}
