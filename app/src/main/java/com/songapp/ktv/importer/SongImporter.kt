package com.songapp.ktv.importer

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.songapp.ktv.data.Song
import com.songapp.ktv.data.SongRepository
import com.songapp.ktv.search.PinyinUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Copies the picked MP3 into the app's external files dir to ensure long-term access,
 * extracts ID3 metadata via MediaMetadataRetriever, finds a sibling .lrc if present
 * (in same directory if it's a File path, or by display-name match if it's a content Uri),
 * and persists a [Song] row.
 */
class SongImporter(
    private val context: Context,
    private val repo: SongRepository
) {

    suspend fun importMany(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        var ok = 0
        for (uri in uris) {
            runCatching { importSingle(uri) }
                .onSuccess { ok++ }
                .onFailure { it.printStackTrace() }
        }
        ok
    }

    suspend fun importFile(file: File): Song? = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null
        val songsRoot = ensureSongsRoot()
        val id = computeId(file)
        val songDir = File(songsRoot, id).apply { mkdirs() }
        val target = File(songDir, "original.mp3")
        if (!target.exists() || target.length() != file.length()) {
            file.inputStream().use { input -> FileOutputStream(target).use { input.copyTo(it) } }
        }
        val sibLrc = findSiblingLrc(file)?.let { copyLrcInto(songDir, it) }
        val song = buildSong(id, target, displayName = file.name, lrcPathOverride = sibLrc)
        repo.upsert(song)
        song
    }

    private suspend fun importSingle(uri: Uri): Song = withContext(Dispatchers.IO) {
        val displayName = queryDisplayName(uri) ?: "song.mp3"
        val songsRoot = ensureSongsRoot()

        // Stream copy first, then derive a content-hash for stable id
        val tempFile = File.createTempFile("imp_", ".mp3", context.cacheDir)
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "cannot open input for $uri" }
            FileOutputStream(tempFile).use { input.copyTo(it) }
        }
        val id = computeId(tempFile)
        val songDir = File(songsRoot, id).apply { mkdirs() }
        val target = File(songDir, "original.mp3")
        if (!target.exists()) tempFile.copyTo(target, overwrite = true)
        tempFile.delete()

        val song = buildSong(id, target, displayName = displayName, lrcPathOverride = null)
        repo.upsert(song)
        song
    }

    private fun buildSong(
        id: String,
        mp3File: File,
        displayName: String,
        lrcPathOverride: String?
    ): Song {
        val mmr = MediaMetadataRetriever()
        var title = displayName.substringBeforeLast('.')
        var artist = "未知歌手"
        var album = ""
        var duration = 0L
        var coverPath: String? = null
        runCatching {
            mmr.setDataSource(mp3File.absolutePath)
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }?.let { title = it }
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() }?.let { artist = it }
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.takeIf { it.isNotBlank() }?.let { album = it }
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let { duration = it }
            mmr.embeddedPicture?.let { bytes ->
                val coverFile = File(mp3File.parentFile, "cover.jpg")
                coverFile.outputStream().use { it.write(bytes) }
                coverPath = coverFile.absolutePath
            }
        }
        runCatching { mmr.release() }

        val pinyin = PinyinUtil.fullPinyin("$title $artist")
        val initials = PinyinUtil.initials("$title $artist")

        return Song(
            id = id,
            title = title,
            artist = artist,
            album = album,
            durationMs = duration,
            mp3Path = mp3File.absolutePath,
            lrcPath = lrcPathOverride,
            coverPath = coverPath,
            pinyinFull = pinyin,
            pinyinInitials = initials
        )
    }

    private fun queryDisplayName(uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null) ?: return null
        cursor.use {
            val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && it.moveToFirst()) return it.getString(idx)
        }
        return null
    }

    private fun ensureSongsRoot(): File {
        val root = File(context.getExternalFilesDir(null) ?: context.filesDir, "songs")
        if (!root.exists()) root.mkdirs()
        return root
    }

    private fun findSiblingLrc(mp3: File): File? {
        val base = mp3.nameWithoutExtension
        val parent = mp3.parentFile ?: return null
        val candidates = listOf("$base.lrc", "$base.LRC")
        for (n in candidates) {
            val f = File(parent, n)
            if (f.exists()) return f
        }
        return null
    }

    private fun copyLrcInto(songDir: File, src: File): String {
        val dst = File(songDir, "lyrics.lrc")
        src.copyTo(dst, overwrite = true)
        return dst.absolutePath
    }

    private fun computeId(file: File): String {
        val md = MessageDigest.getInstance("SHA-1")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            // hash first 1MB + length for speed
            var read = input.read(buf)
            var total = 0
            while (read > 0 && total < 1024 * 1024) {
                md.update(buf, 0, read)
                total += read
                if (total >= 1024 * 1024) break
                read = input.read(buf)
            }
        }
        md.update(file.length().toString().toByteArray())
        return md.digest().joinToString("") { "%02x".format(it) }.take(20)
    }
}
