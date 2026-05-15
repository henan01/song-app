package com.songapp.ktv.lyrics

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 用网易云的公开搜索/歌词接口在线获取 LRC。
 *
 * - 主路：HTTPS（music.163.com）
 * - 回退：HTTP（部分老安卓 TLS 有问题）
 *
 * 任何环节失败都会抛出 [LyricsFetchException]，由调用方在 UI 上展示原因。
 */
object LyricsFetcher {
    private const val TAG = "LyricsFetcher"

    private const val UA =
        "Mozilla/5.0 (Linux; Android 13; SM-S908) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/118.0.0.0 Mobile Safari/537.36"

    suspend fun fetch(title: String, artist: String?): String = withContext(Dispatchers.IO) {
        val keyword = buildKeyword(title, artist)
        if (keyword.isBlank()) throw LyricsFetchException("歌名为空")
        Log.i(TAG, "search keyword=$keyword")
        val songId = search(keyword)
            ?: search(title.trim())  // 退化到只用歌名
            ?: throw LyricsFetchException("没找到匹配歌曲：$keyword")
        Log.i(TAG, "got songId=$songId")
        val lrc = lyric(songId) ?: throw LyricsFetchException("歌曲没有歌词记录 (id=$songId)")
        if (lrc.isBlank()) throw LyricsFetchException("接口返回空歌词 (id=$songId)")
        lrc
    }

    private fun buildKeyword(title: String, artist: String?): String {
        val a = artist?.takeIf { it.isNotBlank() && it != "未知歌手" }?.let { " $it" } ?: ""
        return (title.trim() + a).trim()
    }

    private fun search(keyword: String): Long? {
        val q = URLEncoder.encode(keyword, "UTF-8")
        val path = "/api/search/get?s=$q&type=1&limit=5&offset=0"
        val body = httpGetWithFallback("music.163.com", path) ?: return null
        return runCatching {
            val songs = JSONObject(body)
                .optJSONObject("result")
                ?.optJSONArray("songs") ?: return@runCatching null
            if (songs.length() == 0) null else songs.getJSONObject(0).optLong("id").takeIf { it > 0 }
        }.getOrNull()
    }

    private fun lyric(songId: Long): String? {
        val path = "/api/song/lyric?id=$songId&lv=1&kv=1&tv=-1"
        val body = httpGetWithFallback("music.163.com", path) ?: return null
        return runCatching {
            JSONObject(body).optJSONObject("lrc")?.optString("lyric")?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun httpGetWithFallback(host: String, path: String): String? {
        val httpsUrl = "https://$host$path"
        val httpUrl = "http://$host$path"
        return try {
            httpGet(httpsUrl)
        } catch (e: Exception) {
            Log.w(TAG, "https failed, falling back to http: ${e.message}")
            try { httpGet(httpUrl) } catch (e2: Exception) {
                Log.e(TAG, "http also failed: ${e2.message}")
                throw LyricsFetchException("网络请求失败: ${e2.message ?: e2.javaClass.simpleName}", e2)
            }
        }
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
            val code = conn.responseCode
            if (code in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                Log.w(TAG, "HTTP $code on $url")
                null
            }
        } finally {
            conn?.disconnect()
        }
    }
}

class LyricsFetchException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
