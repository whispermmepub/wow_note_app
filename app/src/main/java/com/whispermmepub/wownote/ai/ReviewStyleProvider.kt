package com.whispermmepub.wownote.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fetches recent writing samples from the user's published Review site and
 * keeps a small local cache. This is retrieval/few-shot style context, not
 * model fine-tuning, so it works with both Groq and Gemini API keys.
 */
class ReviewStyleProvider(context: Context) {
    private val prefs = context.getSharedPreferences("wow_note_review_style", Context.MODE_PRIVATE)

    suspend fun styleContext(forceRefresh: Boolean = false): String = withContext(Dispatchers.IO) {
        val cached = prefs.getString(KEY_TEXT, "").orEmpty()
        val cachedAt = prefs.getLong(KEY_AT, 0L)
        val fresh = System.currentTimeMillis() - cachedAt < CACHE_MS
        if (!forceRefresh && fresh && cached.isNotBlank()) return@withContext cached

        val fetched = runCatching { fetchSamples() }.getOrNull()
        if (!fetched.isNullOrBlank()) {
            prefs.edit().putString(KEY_TEXT, fetched).putLong(KEY_AT, System.currentTimeMillis()).apply()
            fetched
        } else cached
    }

    private fun fetchSamples(): String {
        val connection = URL(POSTS_URL).openConnection() as HttpURLConnection
        connection.connectTimeout = 12_000
        connection.readTimeout = 18_000
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
            val array = JSONArray(reader.readText())
            val out = StringBuilder()
            var used = 0
            for (i in 0 until array.length()) {
                val post = array.optJSONObject(i) ?: continue
                val excerpt = post.optString("excerpt").trim()
                if (excerpt.length < 80) continue
                val title = post.optString("title").trim()
                val author = post.optString("author").trim()
                out.append("\n--- SAMPLE ").append(used + 1).append(" ---\n")
                if (title.isNotBlank()) out.append("ခေါင်းစဉ်: ").append(title).append('\n')
                if (author.isNotBlank()) out.append("ရေးသားသူ: ").append(author).append('\n')
                out.append(excerpt.take(1_100)).append('\n')
                used++
                if (used >= 10 || out.length >= 10_000) break
            }
            return out.toString().take(10_500)
        }.also { connection.disconnect() }
    }

    companion object {
        private const val POSTS_URL = "https://whispermmepub.github.io/Review/assets/posts.json"
        private const val KEY_TEXT = "style_text"
        private const val KEY_AT = "style_cached_at"
        private const val CACHE_MS = 24L * 60L * 60L * 1000L
    }
}
