package com.whispermmepub.wownote.data

import android.content.Context
import com.whispermmepub.wownote.model.Note
import com.whispermmepub.wownote.model.NoteBackground
import com.whispermmepub.wownote.model.NoteBackgroundType
import com.whispermmepub.wownote.model.NoteType
import org.json.JSONArray
import org.json.JSONObject

class NoteStore(context: Context) {
    private val prefs = context.getSharedPreferences("wow_note_store", Context.MODE_PRIVATE)

    fun load(): List<Note> {
        val raw = prefs.getString(KEY_NOTES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) add(array.getJSONObject(i).toNote())
            }
        }.getOrDefault(emptyList())
    }

    fun save(notes: List<Note>) {
        val array = JSONArray()
        notes.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_NOTES, array.toString()).apply()
    }

    private fun Note.toJson() = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("content", content)
        put("type", type.name)
        put("colorArgb", colorArgb)
        put("pinned", pinned)
        put("archived", archived)
        put("deleted", deleted)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
        put("background", JSONObject().apply {
            put("type", background.type.name)
            putNullable("solidArgb", background.solidArgb)
            putNullable("gradientStartArgb", background.gradientStartArgb)
            putNullable("gradientEndArgb", background.gradientEndArgb)
            putNullable("imageUri", background.imageUri)
            put("imageOpacity", background.imageOpacity.toDouble())
            put("imageBlurDp", background.imageBlurDp.toDouble())
        })
    }

    private fun JSONObject.toNote(): Note {
        val bg = optJSONObject("background") ?: JSONObject()
        return Note(
            id = optString("id"),
            title = optString("title"),
            content = optString("content"),
            type = runCatching { NoteType.valueOf(optString("type", NoteType.TEXT.name)) }.getOrDefault(NoteType.TEXT),
            colorArgb = optLong("colorArgb", 0xFFFFFFFF),
            background = NoteBackground(
                type = runCatching { NoteBackgroundType.valueOf(bg.optString("type", NoteBackgroundType.DEFAULT.name)) }.getOrDefault(NoteBackgroundType.DEFAULT),
                solidArgb = bg.optNullableLong("solidArgb"),
                gradientStartArgb = bg.optNullableLong("gradientStartArgb"),
                gradientEndArgb = bg.optNullableLong("gradientEndArgb"),
                imageUri = bg.optNullableString("imageUri"),
                imageOpacity = bg.optDouble("imageOpacity", 1.0).toFloat(),
                imageBlurDp = bg.optDouble("imageBlurDp", 0.0).toFloat()
            ),
            pinned = optBoolean("pinned", false),
            archived = optBoolean("archived", false),
            deleted = optBoolean("deleted", false),
            createdAt = optLong("createdAt", System.currentTimeMillis()),
            updatedAt = optLong("updatedAt", System.currentTimeMillis())
        )
    }

    private fun JSONObject.putNullable(key: String, value: Any?) {
        if (value == null) put(key, JSONObject.NULL) else put(key, value)
    }

    private fun JSONObject.optNullableLong(key: String): Long? =
        if (!has(key) || isNull(key)) null else optLong(key)

    private fun JSONObject.optNullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key)

    companion object { private const val KEY_NOTES = "notes" }
}
