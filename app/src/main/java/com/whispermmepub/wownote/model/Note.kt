package com.whispermmepub.wownote.model

import java.util.UUID

enum class NoteType { TEXT, CHECKLIST }

enum class NoteBackgroundType { DEFAULT, SOLID, GRADIENT, IMAGE }

data class NoteBackground(
    val type: NoteBackgroundType = NoteBackgroundType.DEFAULT,
    val solidArgb: Long? = null,
    val gradientStartArgb: Long? = null,
    val gradientEndArgb: Long? = null,
    val imageUri: String? = null,
    val imageOpacity: Float = 1f,
    val imageBlurDp: Float = 0f
)

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    /** Plain text fallback used for search and card previews. */
    val content: String = "",
    /** JSON representation of the editor spans. */
    val richTextJson: String = "",
    val type: NoteType = NoteType.TEXT,
    val colorArgb: Long = 0xFFFFFFFF,
    val background: NoteBackground = NoteBackground(),
    /** Internal copied .ttf/.otf path, applied note-wide. */
    val customFontPath: String? = null,
    val customFontName: String? = null,
    val defaultFontSizeSp: Float = 18f,
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val deleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
