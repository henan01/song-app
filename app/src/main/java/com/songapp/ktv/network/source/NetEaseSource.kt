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
import java.net.URLEncoder

object NetEaseSource : MusicSource {
    override val id: String = "netease"
    override val displayName: String = "网易云"

    private const val TAG = "NetEaseSource"
    private const val UA =
        "Mozilla/5.0 (Linux; Android 13; SM-S908) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/118.0.0.0 Mobile Safari/537.36"

    override suspend fun search(keyword: String, limit: Int): List<TrackMeta> = withContext(Dispatchers.IO) {
        if (keyword.isBlank()) return@withContext emptyList()
        val q = URLEncoder.encode(keyword.trim(), "UTF-8")
        val body = httpGet("https://music.163.com/api/search/get?s=$q&type=1&limit=$limit&offset=0") ?: return@withContext emptyList()
        runCatching { parseSearch(body) }.getOrDefault(emptyList())
    }

    private fun parseSearch(body: String): List<TrackMeta> {
        val obj = JSONObject(body)
        val songs = obj.optJSONObject("result")?.optJSONArray("songs") ?: return emptyList()
        val list = mutableListOf<TrackMeta>()
        for (i in 0 until songs.length()) {
            val s = songs.getJSONObject(i)
            val sid = s.optLong("id").takeIf { it > 0 } ?: continue
            val title = s.optString("name").takeIf { it.isNotBlank() } ?: continue
            val artist = (s.optJSONArray("artists") ?: JSONArray()).let { arr ->
                (0 until arr.length()).joinToString(" / ") { arr.getJSONObject(it).optString("name") }
            }
            val album = s.optJSONObject("album")
            list += TrackMeta(
                sourceId = id,
                trackId = sid.toString(),
                title = title,
                artist = artist,
                album = album?.optString("name") ?: "",
                durationMs = s.optLong("duration").coerceAtLeast(0L),
                coverUrl = album?.optString("picUrl")?.takeIf { it.isNotBlank() }
            )
        }
        return list
    }

    override suspend fun downloadMp3(
        track: TrackMeta,
        out: OutputStream,
        progress: (Long, Long?) -> Unit
    ): Long = withContext(Dispatchers.IO) {
        val songId = track.trackId.toLongOrNull() ?: throw NotPlayableException("Bad NetEase id: ${track.trackId}")
        val url = URL("https://music.163.com/song/media/outer/url?id=$songId.mp3")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = true
            connectTimeout = 8000
            readTimeout = 30000
            setRequestProperty("User-Agent", UA)
            setRequestProperty("Referer", "https://music.163.com/")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw NotPlayableException("HTTP $code")
            val ct = conn.contentType?.lowercase().orEmpty()
            if (ct.isNotBlank() && !ct.contains("audio") && !ct.contains("mpeg") && !ct.contains("octet-stream")) {
                throw NotPlayableException("Non-audio content: $ct")
            }
            val total = conn.contentLengthLong.takeIf { it > 0 }
            if (total != null && total < 50_000) throw NotPlayableException("Too short ($total B), likely VIP")
            val buf = ByteArray(64 * 1024)
            var written = 0L
            conn.inputStream.use { input ->
                var read: Int
                while (input.read(buf).also { read = it } != -1) {
                    out.write(buf, 0, read); written += read
                    progress(written, total)
                }
            }
            if (written < 50_000) throw NotPlayableException("Wrote $written B, likely VIP")
            written
        } finally {
            conn.disconnect()
        }
    }

    override suspend fun fetchLyric(track: TrackMeta): String? = withContext(Dispatchers.IO) {
        val songId = track.trackId.toLongOrNull() ?: return@withContext null
        val body = httpGet("https://music.163.com/api/song/lyric?id=$songId&lv=1&kv=1&tv=-1") ?: return@withContext null
        runCatching {
            JSONObject(body).optJSONObject("lrc")?.optString("lyric")?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun httpGet(url: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 8000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", UA)
                setRequestProperty("Referer", "https://music.163.com/")
                setRequestProperty("Accept", "application/json,text/plain,*/*")
            }
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                Log.w(TAG, "HTTP ${conn.responseCode} on $url"); null
            }
        } catch (e: Exception) {
            Log.w(TAG, "GET fail: ${e.message}"); null
        } finally {
            conn?.disconnect()
        }
    }
}
