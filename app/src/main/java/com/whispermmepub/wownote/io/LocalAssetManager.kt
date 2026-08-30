package com.whispermmepub.wownote.io

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.util.UUID

object LocalAssetManager {
    data class ImportedAsset(val path: String, val displayName: String)

    fun importFont(context: Context, uri: Uri): ImportedAsset {
        val name = queryName(context, uri).ifBlank { "Custom Font.ttf" }
        val ext = name.substringAfterLast('.', "ttf").lowercase().let {
            if (it == "otf") "otf" else "ttf"
        }
        val dir = File(context.filesDir, "fonts").apply { mkdirs() }
        val out = File(dir, "${UUID.randomUUID()}.$ext")
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Unable to open font" }
            out.outputStream().use { output -> input.copyTo(output) }
        }
        runCatching { Typeface.createFromFile(out) }.getOrElse {
            out.delete()
            throw IllegalArgumentException("Invalid font file", it)
        }
        return ImportedAsset(out.absolutePath, name.substringBeforeLast('.'))
    }

    fun importBackgroundImage(context: Context, uri: Uri): ImportedAsset {
        val name = queryName(context, uri).ifBlank { "Background image" }
        val ext = name.substringAfterLast('.', "jpg").lowercase().takeIf {
            it in setOf("jpg", "jpeg", "png", "webp")
        } ?: "jpg"
        val dir = File(context.filesDir, "backgrounds").apply { mkdirs() }
        val out = File(dir, "${UUID.randomUUID()}.$ext")
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Unable to open image" }
            out.outputStream().use { output -> input.copyTo(output) }
        }
        return ImportedAsset(out.absolutePath, name)
    }

    private fun queryName(context: Context, uri: Uri): String {
        return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0).orEmpty() else ""
            }.orEmpty()
    }
}
