package com.whispermmepub.wownote.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.whispermmepub.wownote.model.Note
import com.whispermmepub.wownote.model.NoteBackground
import com.whispermmepub.wownote.model.NoteBackgroundType
import com.whispermmepub.wownote.model.NoteType
import org.json.JSONObject

/** Local-first SQLite note store. */
class NoteStore(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE notes (
                id TEXT PRIMARY KEY NOT NULL,
                title TEXT NOT NULL,
                content TEXT NOT NULL,
                rich_text_json TEXT NOT NULL,
                type TEXT NOT NULL,
                color_argb INTEGER NOT NULL,
                background_json TEXT NOT NULL,
                custom_font_path TEXT,
                custom_font_name TEXT,
                default_font_size REAL NOT NULL,
                reminder_at INTEGER,
                source_url TEXT,
                pinned INTEGER NOT NULL,
                archived INTEGER NOT NULL,
                deleted INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_notes_updated ON notes(updated_at DESC)")
        db.execSQL("CREATE INDEX idx_notes_state ON notes(deleted, archived, pinned)")
        db.execSQL("CREATE INDEX idx_notes_reminder ON notes(reminder_at)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            runCatching { db.execSQL("ALTER TABLE notes ADD COLUMN rich_text_json TEXT NOT NULL DEFAULT ''") }
            runCatching { db.execSQL("ALTER TABLE notes ADD COLUMN custom_font_path TEXT") }
            runCatching { db.execSQL("ALTER TABLE notes ADD COLUMN custom_font_name TEXT") }
            runCatching { db.execSQL("ALTER TABLE notes ADD COLUMN default_font_size REAL NOT NULL DEFAULT 18") }
        }
        if (oldVersion < 3) {
            runCatching { db.execSQL("ALTER TABLE notes ADD COLUMN reminder_at INTEGER") }
            runCatching { db.execSQL("ALTER TABLE notes ADD COLUMN source_url TEXT") }
            runCatching { db.execSQL("CREATE INDEX idx_notes_reminder ON notes(reminder_at)") }
        }
    }

    fun all(): List<Note> = readableDatabase.query(
        "notes", null, null, null, null, null,
        "pinned DESC, updated_at DESC"
    ).use(::readNotes)

    fun get(id: String): Note? = readableDatabase.query(
        "notes", null, "id = ?", arrayOf(id), null, null, null, "1"
    ).use { readNotes(it).firstOrNull() }

    fun dueReminders(afterMillis: Long = System.currentTimeMillis()): List<Note> = readableDatabase.query(
        "notes", null,
        "reminder_at IS NOT NULL AND reminder_at >= ? AND deleted = 0",
        arrayOf(afterMillis.toString()), null, null, "reminder_at ASC"
    ).use(::readNotes)

    fun upsert(note: Note) {
        writableDatabase.insertWithOnConflict(
            "notes",
            null,
            ContentValues().apply {
                put("id", note.id)
                put("title", note.title)
                put("content", note.content)
                put("rich_text_json", note.richTextJson)
                put("type", note.type.name)
                put("color_argb", note.colorArgb)
                put("background_json", encodeBackground(note.background))
                put("custom_font_path", note.customFontPath)
                put("custom_font_name", note.customFontName)
                put("default_font_size", note.defaultFontSizeSp)
                if (note.reminderAt == null) putNull("reminder_at") else put("reminder_at", note.reminderAt)
                put("source_url", note.sourceUrl)
                put("pinned", if (note.pinned) 1 else 0)
                put("archived", if (note.archived) 1 else 0)
                put("deleted", if (note.deleted) 1 else 0)
                put("created_at", note.createdAt)
                put("updated_at", note.updatedAt)
            },
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun deleteForever(id: String) {
        writableDatabase.delete("notes", "id = ?", arrayOf(id))
    }

    fun search(query: String): List<Note> {
        if (query.isBlank()) return all()
        val like = "%${query.trim()}%"
        return readableDatabase.query(
            "notes", null,
            "(title LIKE ? OR content LIKE ? OR source_url LIKE ?)",
            arrayOf(like, like, like), null, null,
            "pinned DESC, updated_at DESC"
        ).use(::readNotes)
    }

    private fun readNotes(cursor: Cursor): List<Note> = buildList {
        while (cursor.moveToNext()) {
            val reminderIndex = cursor.getColumnIndexOrThrow("reminder_at")
            add(
                Note(
                    id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                    title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                    content = cursor.getString(cursor.getColumnIndexOrThrow("content")),
                    richTextJson = cursor.getString(cursor.getColumnIndexOrThrow("rich_text_json")),
                    type = runCatching {
                        NoteType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("type")))
                    }.getOrDefault(NoteType.TEXT),
                    colorArgb = cursor.getLong(cursor.getColumnIndexOrThrow("color_argb")),
                    background = decodeBackground(cursor.getString(cursor.getColumnIndexOrThrow("background_json"))),
                    customFontPath = cursor.getString(cursor.getColumnIndexOrThrow("custom_font_path")),
                    customFontName = cursor.getString(cursor.getColumnIndexOrThrow("custom_font_name")),
                    defaultFontSizeSp = cursor.getFloat(cursor.getColumnIndexOrThrow("default_font_size")),
                    reminderAt = if (cursor.isNull(reminderIndex)) null else cursor.getLong(reminderIndex),
                    sourceUrl = cursor.getString(cursor.getColumnIndexOrThrow("source_url")),
                    pinned = cursor.getInt(cursor.getColumnIndexOrThrow("pinned")) == 1,
                    archived = cursor.getInt(cursor.getColumnIndexOrThrow("archived")) == 1,
                    deleted = cursor.getInt(cursor.getColumnIndexOrThrow("deleted")) == 1,
                    createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                    updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"))
                )
            )
        }
    }

    private fun encodeBackground(background: NoteBackground): String = JSONObject().apply {
        put("type", background.type.name)
        put("solidArgb", background.solidArgb ?: JSONObject.NULL)
        put("gradientStartArgb", background.gradientStartArgb ?: JSONObject.NULL)
        put("gradientEndArgb", background.gradientEndArgb ?: JSONObject.NULL)
        put("imageUri", background.imageUri ?: JSONObject.NULL)
        put("imageOpacity", background.imageOpacity)
        put("imageBlurDp", background.imageBlurDp)
    }.toString()

    private fun decodeBackground(raw: String): NoteBackground = runCatching {
        val o = JSONObject(raw)
        NoteBackground(
            type = runCatching { NoteBackgroundType.valueOf(o.optString("type", "DEFAULT")) }
                .getOrDefault(NoteBackgroundType.DEFAULT),
            solidArgb = if (o.isNull("solidArgb")) null else o.optLong("solidArgb"),
            gradientStartArgb = if (o.isNull("gradientStartArgb")) null else o.optLong("gradientStartArgb"),
            gradientEndArgb = if (o.isNull("gradientEndArgb")) null else o.optLong("gradientEndArgb"),
            imageUri = if (o.isNull("imageUri")) null else o.optString("imageUri"),
            imageOpacity = o.optDouble("imageOpacity", 1.0).toFloat(),
            imageBlurDp = o.optDouble("imageBlurDp", 0.0).toFloat()
        )
    }.getOrDefault(NoteBackground())

    companion object {
        private const val DB_NAME = "wow_note.db"
        private const val DB_VERSION = 3
    }
}
