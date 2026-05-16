package com.songapp.ktv.network.source

import android.util.Log
import com.songapp.ktv.network.MusicSource
import com.songapp.ktv.network.NotPlayableException
import com.songapp.ktv.network.TrackMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 酷我音乐公开接口适配。仅供个人使用，VIP 曲目无法下载。
 *
 * 用到的接口：
 *   - 搜索（旧版无鉴权）：http://search.kuwo.cn/r.s
 *   - MP3 直链：http://antiserver.kuwo.cn/anti.s?type=convert_url3
 *   - 歌词：http://m.kuwo.cn/newh5/singles/songinfoandlrc?musicId=
 */
object KuwoSource : MusicSource {
    override val id: String = "kuwo"
    override val displayName: String = "酷我"

    private const val TAG = "KuwoSource"
    private const val UA_DESKTOP =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/118.0.0.0 Safari/537.36"
    private const val UA_MOBILE =
        "Mozilla/5.0 (Linux; Android 13; SM-S908) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/118.0.0.0 Mobile Safari/537.36"

    override suspend fun search(keyword: String, limit: Int): List<TrackMeta> = withContext(Dispatchers.IO) {
        if (keyword.isBlank()) return@withContext emptyList()
        val q = URLEncoder.encode(keyword.trim(), "UTF-8")
        // 老版 search.kuwo.cn/r.s 接口，不需要 csrf
        val url = "http://search.kuwo.cn/r.s?ft=music&itemset=web_2013&client=kt&pn=0&rn=$limit" +
            "&rformat=json&encoding=utf8&all=$q"
        val body = httpGet(url, UA_DESKTOP, referer = "http://www.kuwo.cn/") ?: return@withContext emptyList()
        runCatching { parseSearch(body) }.getOrElse {
            Log.w(TAG, "parse search fail: ${it.message}")
            emptyList()
        }
    }

    private fun parseSearch(body: String): List<TrackMeta> {
        val out = mutableListOf<TrackMeta>()
        // r.s 接口返回非标准 JSON：字段名/字符串没加双引号，需要先简单规整
        // 但当 client=kt + rformat=json 时大多情况下是合法 JSON，先按合法 JSON 试
        val obj = runCatching { JSONObject(body) }.getOrNull() ?: return parseLooseSearch(body)
        val abs = obj.optJSONArray("abslist") ?: return emptyList()
        for (i in 0 until abs.length()) {
            val s = abs.getJSONObject(i)
            val ridStr = s.optString("MUSICRID").removePrefix("MUSIC_").takeIf { it.isNotBlank() } ?: continue
            val title = s.optString("SONGNAME").takeIf { it.isNotBlank() } ?: continue
            val artist = s.optString("ARTIST").ifBlank { "未知歌手" }
            val album = s.optString("ALBUM").orEmpty()
            val durSec = s.optString("DURATION").toLongOrNull() ?: 0L
            val cover = s.optString("hts_MVPIC").takeIf { it.isNotBlank() }
                ?: s.optString("pic").takeIf { it.isNotBlank() }
            out += TrackMeta(
                sourceId = id,
                trackId = ridStr,
                title = title,
                artist = artist,
                album = album,
                durationMs = durSec * 1000,
                coverUrl = cover
            )
        }
        return out
    }

    // 兼容旧 r.s 输出的"非严格 JSON"：字段名不带引号
    private fun parseLooseSearch(body: String): List<TrackMeta> {
        return try {
            val fixed = body
                .replace(Regex("([{,])\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*:"), "$1\"$2\":")
                .replace(Regex(":'([^']*)'"), ":\"$1\"")
            parseSearch(fixed.also { /* avoid recursion if loose -> strict already failed */ })
        } catch (e: Exception) {
            Log.w(TAG, "loose parse fail: ${e.message}")
            emptyList()
        }
    }

    override suspend fun downloadMp3(
        track: TrackMeta,
        out: OutputStream,
        progress: (Long, Long?) -> Unit
    ): Long = withContext(Dispatchers.IO) {
        val rid = track.trackId
        val urlApi = "http://antiserver.kuwo.cn/anti.s?type=convert_url3&rid=$rid&format=mp3&response=url"
        // 先拿 mp3 直链
        val antiBody = httpGet(urlApi, UA_DESKTOP, referer = "http://www.kuwo.cn/") ?: throw NotPlayableException("无法获取 MP3 地址")
        val mp3Url = parseAntiserverUrl(antiBody) ?: throw NotPlayableException("接口返回未识别: ${antiBody.take(60)}")
        // 再下载
        val conn = (URL(mp3Url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = true
            connectTimeout = 8000
            readTimeout = 30000
            setRequestProperty("User-Agent", UA_DESKTOP)
            setRequestProperty("Referer", "http://www.kuwo.cn/")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw NotPlayableException("HTTP $code")
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

    /** antiserver 返回有可能是裸 URL，也可能是 JSON {"url":"..."}。 */
    private fun parseAntiserverUrl(body: String): String? {
        val trimmed = body.trim()
        if (trimmed.startsWith("http")) return trimmed
        return runCatching {
            JSONObject(trimmed).optString("url").takeIf { it.startsWith("http") }
        }.getOrNull()
    }

    override suspend fun fetchLyric(track: TrackMeta): String? = withContext(Dispatchers.IO) {
        val rid = track.trackId
        val url = "http://m.kuwo.cn/newh5/singles/songinfoandlrc?musicId=$rid"
        val body = httpGet(url, UA_MOBILE, referer = "http://m.kuwo.cn/") ?: return@withContext null
        runCatching {
            val obj = JSONObject(body)
            val arr = obj.optJSONObject("data")?.optJSONArray("lrclist") ?: return@runCatching null
            val sb = StringBuilder()
            for (i in 0 until arr.length()) {
                val line = arr.getJSONObject(i)
                val sec = line.optString("time").toDoubleOrNull() ?: continue
                val text = line.optString("lineLyric").orEmpty()
                val m = (sec / 60).toInt()
                val s = (sec - m * 60)
                sb.append('[')
                    .append(String.format("%02d", m)).append(':')
                    .append(String.format("%05.2f", s))
                    .append(']').append(text).append('\n')
            }
            sb.toString().takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun httpGet(url: String, ua: String, referer: String? = null): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 10000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", ua)
                if (referer != null) setRequestProperty("Referer", referer)
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
