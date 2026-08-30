package com.whispermmepub.wownote.io

import android.content.Context
import android.net.Uri
import com.whispermmepub.wownote.model.Note
import org.json.JSONArray
import org.json.JSONObject
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DocxExporter {
    private data class SpanDef(val type: String, val start: Int, val end: Int, val value: String? = null)

    fun export(context: Context, uri: Uri, note: Note) {
        val root = runCatching { JSONObject(note.richTextJson) }.getOrElse { JSONObject() }
        val text = root.optString("text", note.content)
        val spans = decodeSpans(root.optJSONArray("spans") ?: JSONArray())
        val documentXml = buildDocument(note, text, spans)

        context.contentResolver.openOutputStream(uri, "w")!!.use { stream ->
            ZipOutputStream(stream).use { zip ->
                zip.putText("[Content_Types].xml", contentTypes())
                zip.putText("_rels/.rels", packageRels())
                zip.putText("word/document.xml", documentXml)
                zip.putText("word/styles.xml", stylesXml(note))
                zip.putText("word/_rels/document.xml.rels", documentRels())
            }
        }
    }

    private fun buildDocument(note: Note, text: String, spans: List<SpanDef>): String {
        val body = StringBuilder()
        if (note.title.isNotBlank()) {
            body.append("<w:p><w:pPr><w:spacing w:after=\"160\"/></w:pPr><w:r><w:rPr><w:b/><w:sz w:val=\"32\"/>")
            fontXml(note.customFontName)?.let(body::append)
            body.append("</w:rPr><w:t xml:space=\"preserve\">")
            body.append(xml(note.title))
            body.append("</w:t></w:r></w:p>")
        }

        var paragraphStart = 0
        val lines = text.split('\n')
        lines.forEach { line ->
            val paragraphEnd = paragraphStart + line.length
            val alignment = spans.lastOrNull {
                it.type == "align" && it.start <= paragraphEnd && it.end >= paragraphStart
            }?.value ?: "left"
            body.append("<w:p><w:pPr><w:jc w:val=\"")
            body.append(alignment)
            body.append("\"/><w:spacing w:line=\"360\" w:lineRule=\"auto\"/></w:pPr>")
            appendRuns(body, note, text, paragraphStart, paragraphEnd, spans)
            if (line.isEmpty()) body.append("<w:r><w:t></w:t></w:r>")
            body.append("</w:p>")
            paragraphStart = paragraphEnd + 1
        }

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:body>
$body
<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1134" w:right="1134" w:bottom="1134" w:left="1134"/></w:sectPr>
</w:body>
</w:document>"""
    }

    private fun appendRuns(
        out: StringBuilder,
        note: Note,
        fullText: String,
        start: Int,
        end: Int,
        spans: List<SpanDef>
    ) {
        if (end <= start) return
        val boundaries = sortedSetOf(start, end)
        spans.filter { it.type != "align" && it.end > start && it.start < end }.forEach {
            boundaries += it.start.coerceIn(start, end)
            boundaries += it.end.coerceIn(start, end)
        }
        val points = boundaries.toList()
        for (i in 0 until points.lastIndex) {
            val a = points[i]
            val b = points[i + 1]
            if (b <= a) continue
            val active = spans.filter { it.type != "align" && it.start <= a && it.end >= b }
            val bold = active.any { it.type == "bold" }
            val italic = active.any { it.type == "italic" }
            val underline = active.any { it.type == "underline" }
            val size = active.lastOrNull { it.type == "size" }?.value?.toIntOrNull()
                ?: note.defaultFontSizeSp.toInt()

            out.append("<w:r><w:rPr>")
            if (bold) out.append("<w:b/>")
            if (italic) out.append("<w:i/>")
            if (underline) out.append("<w:u w:val=\"single\"/>")
            out.append("<w:sz w:val=\"").append(size.coerceIn(8, 96) * 2).append("\"/>")
            fontXml(note.customFontName)?.let(out::append)
            out.append("</w:rPr><w:t xml:space=\"preserve\">")
            out.append(xml(fullText.substring(a, b)))
            out.append("</w:t></w:r>")
        }
    }

    private fun decodeSpans(array: JSONArray): List<SpanDef> = buildList {
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            add(
                SpanDef(
                    type = o.optString("type"),
                    start = o.optInt("start"),
                    end = o.optInt("end"),
                    value = if (o.has("value")) o.opt("value")?.toString() else null
                )
            )
        }
    }

    private fun fontXml(name: String?): String? = name?.takeIf { it.isNotBlank() }?.let {
        val n = xml(it)
        "<w:rFonts w:ascii=\"$n\" w:hAnsi=\"$n\" w:eastAsia=\"$n\" w:cs=\"$n\"/>"
    }

    private fun stylesXml(note: Note): String {
        val font = fontXml(note.customFontName).orEmpty()
        val size = (note.defaultFontSizeSp.toInt().coerceIn(8, 96) * 2)
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/><w:rPr>$font<w:sz w:val="$size"/></w:rPr></w:style>
</w:styles>"""
    }

    private fun contentTypes() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
<Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
</Types>"""

    private fun packageRels() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

    private fun documentRels() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    private fun ZipOutputStream.putText(path: String, value: String) {
        putNextEntry(ZipEntry(path))
        write(value.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun xml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
