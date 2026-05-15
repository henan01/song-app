package com.songapp.ktv.lyrics

import java.io.File

data class LrcLine(val timeMs: Long, val text: String)

object LrcParser {
    private val timeTagRegex = Regex("\\[(\\d{1,2}):(\\d{1,2})(?:[.:](\\d{1,3}))?]")

    fun parseFile(path: String?): List<LrcLine> {
        if (path.isNullOrBlank()) return emptyList()
        val f = File(path)
        if (!f.exists() || !f.isFile) return emptyList()
        return runCatching {
            parse(f.readText(Charsets.UTF_8))
        }.getOrElse {
            runCatching { parse(f.readText(Charsets.ISO_8859_1)) }.getOrDefault(emptyList())
        }
    }

    fun parse(content: String): List<LrcLine> {
        val out = mutableListOf<LrcLine>()
        content.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEach
            val matches = timeTagRegex.findAll(line).toList()
            if (matches.isEmpty()) return@forEach
            val text = line.substring(matches.last().range.last + 1).trim()
            for (m in matches) {
                val mm = m.groupValues[1].toLongOrNull() ?: continue
                val ss = m.groupValues[2].toLongOrNull() ?: continue
                val frac = m.groupValues.getOrNull(3)?.takeIf { it.isNotBlank() }?.toLongOrNull() ?: 0L
                val msDigits = m.groupValues.getOrNull(3)?.length ?: 0
                val ms = when (msDigits) {
                    1 -> frac * 100
                    2 -> frac * 10
                    3 -> frac
                    else -> 0L
                }
                val total = (mm * 60 + ss) * 1000 + ms
                if (text.isNotEmpty()) out += LrcLine(total, text)
            }
        }
        return out.sortedBy { it.timeMs }
    }

    fun activeLineIndex(lines: List<LrcLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        var lo = 0; var hi = lines.size - 1; var ans = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (lines[mid].timeMs <= positionMs) {
                ans = mid; lo = mid + 1
            } else hi = mid - 1
        }
        return ans
    }
}
