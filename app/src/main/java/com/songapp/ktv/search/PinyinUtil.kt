package com.songapp.ktv.search

/**
 * 极简版本：保留接口与字段，但暂不依赖外部库做汉字转拼音。
 * 中文歌名/歌手仍可通过原文 LIKE 子串匹配；ASCII 字母数字会归一化到小写以便搜索。
 * 后续若要真正的「拼音首字母」搜索，可在此处接入 ICU4J Transliterator 或离线表。
 */
object PinyinUtil {
    fun fullPinyin(text: String): String {
        if (text.isBlank()) return ""
        val sb = StringBuilder()
        for (c in text) {
            if (c.isLetterOrDigit()) sb.append(c.lowercaseChar())
        }
        return sb.toString()
    }

    fun initials(text: String): String = fullPinyin(text).take(64)
}
