package com.songapp.ktv.cast

import android.content.Context
import com.songapp.ktv.data.Song
import com.songapp.ktv.data.SongRepository
import com.songapp.ktv.lyrics.LrcParser
import com.songapp.ktv.player.KtvPlayer
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.concurrent.Executors

class KtvRoomServer(
    private val context: Context,
    private val repo: SongRepository,
    private val player: KtvPlayer
) {
    @Volatile private var serverSocket: ServerSocket? = null
    private val executor = Executors.newCachedThreadPool()

    val isRunning: Boolean get() = serverSocket != null
    val port: Int get() = serverSocket?.localPort ?: DEFAULT_PORT
    val host: String get() = localIp() ?: "127.0.0.1"
    val tvUrl: String get() = "http://$host:$port/tv"
    val remoteUrl: String get() = "http://$host:$port/remote"

    fun start(preferredPort: Int = DEFAULT_PORT) {
        if (isRunning) return
        val socket = runCatching { ServerSocket(preferredPort) }.getOrElse { ServerSocket(0) }
        serverSocket = socket
        executor.execute {
            while (!socket.isClosed) {
                val client = runCatching { socket.accept() }.getOrNull() ?: break
                executor.execute { handle(client) }
            }
        }
    }

    fun stop() {
        runCatching { serverSocket?.close() }
        serverSocket = null
    }

    private fun handle(socket: Socket) {
        socket.use { client ->
            val reader = BufferedReader(InputStreamReader(client.getInputStream()))
            val request = reader.readLine().orEmpty()
            val parts = request.split(" ")
            val target = parts.getOrNull(1).orEmpty()
            val path = target.substringBefore("?")
            val query = parseQuery(target.substringAfter("?", ""))
            while (reader.readLine().isNotEmpty()) Unit

            val response = runCatching {
                when (path) {
                    "/", "/tv" -> html(tvPage(), "text/html; charset=utf-8")
                    "/remote" -> html(remotePage(), "text/html; charset=utf-8")
                    "/api/state" -> json(stateJson())
                    "/api/songs" -> json(songsJson(query["q"].orEmpty()))
                    "/api/queue" -> json(queueJson())
                    "/api/enqueue" -> {
                        val id = query["id"].orEmpty()
                        var ok = false
                        if (id.isNotBlank()) {
                            runBlocking { repo.enqueue(id) }
                            if (player.state.value.currentSong == null) {
                                runBlocking { repo.getById(id) }?.let { player.playSong(it) }
                            }
                            ok = true
                        }
                        json(JSONObject().put("ok", ok).toString())
                    }
                    "/api/play" -> {
                        val id = query["id"].orEmpty()
                        val song = if (id.isNotBlank()) runBlocking { repo.getById(id) } else null
                        if (song != null) player.playSong(song)
                        json(JSONObject().put("ok", song != null).toString())
                    }
                    "/api/toggle" -> {
                        player.togglePlay()
                        json(JSONObject().put("ok", true).toString())
                    }
                    "/api/next", "/api/cut" -> {
                        player.playNext()
                        json(JSONObject().put("ok", true).toString())
                    }
                    "/api/queue/remove" -> {
                        val queueId = query["id"]?.toLongOrNull()
                        if (queueId != null) runBlocking { repo.removeFromQueue(queueId) }
                        json(JSONObject().put("ok", queueId != null).toString())
                    }
                    "/api/queue/top" -> {
                        val queueId = query["id"]?.toLongOrNull()
                        if (queueId != null) runBlocking { repo.moveQueueItemToTop(queueId) }
                        json(JSONObject().put("ok", queueId != null).toString())
                    }
                    "/api/seek" -> {
                        val ms = query["ms"]?.toLongOrNull()
                        if (ms != null) player.seekTo(ms)
                        json(JSONObject().put("ok", ms != null).toString())
                    }
                    "/api/vocal" -> {
                        val level = query["level"]?.toFloatOrNull()
                        if (level != null) player.setVocalLevel(level)
                        json(JSONObject().put("ok", level != null).toString())
                    }
                    else -> html("Not found", "text/plain; charset=utf-8", 404)
                }
            }.getOrElse {
                html(it.message ?: it.javaClass.simpleName, "text/plain; charset=utf-8", 500)
            }
            client.getOutputStream().use { it.write(response) }
        }
    }

    private fun stateJson(): String {
        val st = player.state.value
        val song = st.currentSong
        val lyrics = if (song?.lrcPath != null) LrcParser.parseFile(song.lrcPath) else emptyList()
        val active = LrcParser.activeLineIndex(lyrics, st.positionMs)
        val queue = runBlocking { repo.queueSnapshot() }
        val nextSong = queue.firstOrNull()?.let { runBlocking { repo.getById(it.songId) } }
        return JSONObject()
            .put("title", song?.title.orEmpty())
            .put("artist", song?.artist.orEmpty())
            .put("isPlaying", st.isPlaying)
            .put("positionMs", st.positionMs)
            .put("durationMs", st.durationMs)
            .put("vocalLevel", st.vocalLevel)
            .put("queueCount", queue.size)
            .put("nextTitle", nextSong?.title.orEmpty())
            .put("nextArtist", nextSong?.artist.orEmpty())
            .put("remoteUrl", remoteUrl)
            .put("lyric", lyrics.getOrNull(active)?.text.orEmpty())
            .put("nextLyric", lyrics.getOrNull(active + 1)?.text.orEmpty())
            .toString()
    }

    private fun songsJson(q: String): String = runBlocking {
        val arr = JSONArray()
        repo.searchSongs(q).forEach { song -> arr.put(song.toJson()) }
        JSONObject().put("songs", arr).toString()
    }

    private fun queueJson(): String = runBlocking {
        val arr = JSONArray()
        repo.queueSnapshot().forEach { item ->
            repo.getById(item.songId)?.let { song ->
                arr.put(song.toJson().put("queueId", item.id).put("position", item.position))
            }
        }
        JSONObject().put("queue", arr).toString()
    }

    private fun Song.toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("title", title)
            .put("artist", artist)
            .put("durationMs", durationMs)

    private fun tvPage(): String = """
        <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
        <title>星唱 KTV 电视端</title>
        <style>
        body{margin:0;background:#05040a;color:white;font-family:system-ui,-apple-system,BlinkMacSystemFont,sans-serif;min-height:100vh;display:flex;align-items:center;justify-content:center;overflow:hidden}
        body:before{content:"";position:fixed;inset:-30%;background:radial-gradient(circle at 20% 20%,#ff3bd455,transparent 26%),radial-gradient(circle at 80% 15%,#31f7ff44,transparent 28%),radial-gradient(circle at 60% 90%,#9b5cff44,transparent 32%);filter:blur(20px)}
        .wrap{width:min(1180px,92vw);text-align:center;position:relative}.now{font-size:28px;color:#9ff;margin-bottom:40px}.lyric{font-size:72px;font-weight:800;line-height:1.25;text-shadow:0 0 28px #ff3bd4}.next{font-size:34px;color:#aaa;margin-top:28px}
        .side{position:fixed;right:32px;bottom:28px;color:#ddd;font-size:18px;text-align:right}.side code{color:#9ff}.upnext{position:fixed;left:32px;bottom:28px;color:#ddd;font-size:20px}.upnext b{color:#ffdb66}
        .bar{height:6px;background:#222;position:fixed;left:0;right:0;bottom:0}.bar>i{display:block;height:100%;background:linear-gradient(90deg,#31f7ff,#ff3bd4);width:0}
        </style></head><body><div class="wrap"><div class="now" id="now">等待播放</div><div class="lyric" id="lyric">星唱 KTV</div><div class="next" id="next">扫码点歌，电视看词</div></div>
        <div class="upnext">下一首：<b id="upnext">暂无</b></div><div class="side">手机点歌地址<br><code id="remote"></code></div><div class="bar"><i id="p"></i></div>
        <script>
        async function tick(){const s=await fetch('/api/state').then(r=>r.json());now.textContent=s.title?`${'$'}{s.title} · ${'$'}{s.artist}`:'等待播放';lyric.textContent=s.lyric||'暂无歌词';next.textContent=s.nextLyric||'';remote.textContent=s.remoteUrl||'';upnext.textContent=s.nextTitle?`${'$'}{s.nextTitle} · ${'$'}{s.nextArtist}`:'暂无';p.style.width=s.durationMs?((s.positionMs/s.durationMs)*100)+'%':'0'}
        setInterval(tick,700);tick();
        </script></body></html>
    """.trimIndent()

    private fun remotePage(): String = """
        <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
        <title>星唱 KTV 点歌</title>
        <style>
        body{margin:0;background:#0b0912;color:white;font-family:system-ui,-apple-system,BlinkMacSystemFont,sans-serif}.top{position:sticky;top:0;z-index:2;background:#0b0912;padding:14px;border-bottom:1px solid #24202e}.now{font-size:13px;color:#9ff;margin-bottom:10px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}input{width:100%;box-sizing:border-box;border:1px solid #333;background:#15111f;color:white;border-radius:12px;padding:13px;font-size:16px}.tabs,.ctl{display:flex;gap:8px;margin-top:10px}.tabs button,.ctl button{margin-left:0}.tabs button{background:#221b2d;color:#ddd}.tabs button.on{background:#ff3bd4;color:#07040a}button{margin-left:auto;border:0;border-radius:999px;padding:9px 12px;background:#ff3bd4;color:#07040a;font-weight:700}.ctl button{background:#31f7ff}.ctl button.danger{background:#ff6464}.song{display:flex;align-items:center;gap:10px;padding:14px 16px;border-bottom:1px solid #1f1a29}.song b{display:block}.song span{color:#aaa;font-size:13px}.song .actions{margin-left:auto;display:flex;gap:7px}.song .actions button{margin-left:0}.ghost{background:#2a2433;color:#ddd}.empty{text-align:center;color:#aaa;padding:40px 20px}
        </style></head><body><div class="top"><div class="now" id="now">连接中...</div><input id="q" placeholder="搜本机曲库"><div class="ctl"><button onclick="api('/api/toggle')">播放/暂停</button><button class="danger" onclick="api('/api/cut')">切歌</button></div><div class="tabs"><button id="tabSongs" class="on" onclick="tab='songs';render()">搜歌</button><button id="tabQueue" onclick="tab='queue';render()">已点</button></div></div><main id="list"></main>
        <script>
        let tab='songs', songs=[], queue=[];
        async function api(u){await fetch(u);await load()}
        async function load(){const [st,ss,qq]=await Promise.all([fetch('/api/state').then(r=>r.json()),fetch('/api/songs?q='+encodeURIComponent(q.value)).then(r=>r.json()),fetch('/api/queue').then(r=>r.json())]);songs=ss.songs;queue=qq.queue;now.textContent=st.title?`正在唱：${'$'}{st.title} · ${'$'}{st.artist}｜队列 ${'$'}{st.queueCount} 首`:'等待播放｜先点一首歌';render()}
        function render(){tabSongs.className=tab==='songs'?'on':'';tabQueue.className=tab==='queue'?'on':'';const data=tab==='songs'?songs:queue;if(!data.length){list.innerHTML='<div class="empty">这里还没有歌</div>';return}list.innerHTML=data.map(s=>tab==='songs'?songRow(s):queueRow(s)).join('')}
        function songRow(s){return `<div class="song"><div><b>${'$'}{s.title}</b><span>${'$'}{s.artist}</span></div><div class="actions"><button onclick="api('/api/enqueue?id=${'$'}{encodeURIComponent(s.id)}')">点歌</button><button class="ghost" onclick="api('/api/play?id=${'$'}{encodeURIComponent(s.id)}')">直接唱</button></div></div>`}
        function queueRow(s){return `<div class="song"><div><b>${'$'}{s.title}</b><span>${'$'}{s.artist}</span></div><div class="actions"><button onclick="api('/api/queue/top?id=${'$'}{s.queueId}')">顶歌</button><button class="ghost" onclick="api('/api/queue/remove?id=${'$'}{s.queueId}')">删除</button></div></div>`}
        q.oninput=()=>load();setInterval(load,2500);load();
        </script></body></html>
    """.trimIndent()

    private fun html(body: String, contentType: String, status: Int = 200): ByteArray =
        respond(status, contentType, body.toByteArray(Charsets.UTF_8))

    private fun json(body: String): ByteArray =
        respond(200, "application/json; charset=utf-8", body.toByteArray(Charsets.UTF_8))

    private fun respond(status: Int, contentType: String, body: ByteArray): ByteArray {
        val head = "HTTP/1.1 $status OK\r\nContent-Type: $contentType\r\nContent-Length: ${body.size}\r\nConnection: close\r\nAccess-Control-Allow-Origin: *\r\n\r\n"
        return head.toByteArray(Charsets.UTF_8) + body
    }

    private fun parseQuery(query: String): Map<String, String> =
        query.split('&').filter { it.contains('=') }.associate {
            val key = it.substringBefore('=')
            val value = URLDecoder.decode(it.substringAfter('='), "UTF-8")
            key to value
        }

    private fun localIp(): String? {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        for (intf in interfaces) {
            val addresses = intf.inetAddresses
            for (addr in addresses) {
                if (!addr.isLoopbackAddress && addr is Inet4Address) return addr.hostAddress
            }
        }
        return null
    }

    companion object {
        private const val DEFAULT_PORT = 8989
    }
}
