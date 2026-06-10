package com.songapp.ktv.network.source

import android.content.Context
import org.json.JSONArray

data class UserSourceConfig(
    val id: String,
    val name: String,
    val catalogUrl: String,
    val inlineJson: String? = null
)

class UserSourceStore(context: Context) {
    private val prefs = context.getSharedPreferences("user_sources", Context.MODE_PRIVATE)

    fun list(): List<UserSourceConfig> {
        val raw = prefs.getString(KEY_SOURCES, "[]").orEmpty()
        val arr = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        return buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id").takeIf { it.isNotBlank() } ?: continue
                val name = obj.optString("name").ifBlank { "我的歌源" }
                val url = obj.optString("catalogUrl")
                val inlineJson = obj.optString("inlineJson").takeIf { it.isNotBlank() }
                if (url.isBlank() && inlineJson == null) continue
                add(UserSourceConfig(id, name, url, inlineJson))
            }
        }
    }

    fun add(name: String, catalogUrl: String): UserSourceConfig {
        val cleanUrl = catalogUrl.trim()
        val cleanName = name.trim().ifBlank { hostName(cleanUrl) ?: "我的歌源" }
        val config = UserSourceConfig(
            id = "catalog_" + cleanUrl.hashCode().toUInt().toString(16),
            name = cleanName,
            catalogUrl = cleanUrl
        )
        val next = (list().filterNot { it.id == config.id } + config)
        save(next)
        return config
    }

    fun addInline(name: String, json: String): UserSourceConfig {
        val cleanName = name.trim().ifBlank { "本地歌源" }
        val config = UserSourceConfig(
            id = "catalog_local_" + json.hashCode().toUInt().toString(16),
            name = cleanName,
            catalogUrl = "",
            inlineJson = json
        )
        val next = (list().filterNot { it.id == config.id } + config)
        save(next)
        return config
    }

    fun remove(id: String) {
        save(list().filterNot { it.id == id })
    }

    private fun save(list: List<UserSourceConfig>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                org.json.JSONObject()
                    .put("id", it.id)
                    .put("name", it.name)
                    .put("catalogUrl", it.catalogUrl)
                    .put("inlineJson", it.inlineJson ?: "")
            )
        }
        prefs.edit().putString(KEY_SOURCES, arr.toString()).apply()
    }

    private fun hostName(url: String): String? =
        runCatching { java.net.URL(url).host }.getOrNull()?.takeIf { it.isNotBlank() }

    companion object {
        private const val KEY_SOURCES = "sources"
    }
}
