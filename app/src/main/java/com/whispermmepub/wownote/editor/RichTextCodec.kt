package com.whispermmepub.wownote.editor

import android.graphics.Typeface
import android.text.Layout
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.AlignmentSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import org.json.JSONArray
import org.json.JSONObject

object RichTextCodec {
    fun encode(text: Spanned): String {
        val root = JSONObject()
        root.put("text", text.toString())
        val spans = JSONArray()

        text.getSpans(0, text.length, Any::class.java).forEach { span ->
            val start = text.getSpanStart(span)
            val end = text.getSpanEnd(span)
            if (start < 0 || end <= start) return@forEach
            when (span) {
                is StyleSpan -> {
                    when (span.style) {
                        Typeface.BOLD -> spans.put(spanJson("bold", start, end))
                        Typeface.ITALIC -> spans.put(spanJson("italic", start, end))
                        Typeface.BOLD_ITALIC -> {
                            spans.put(spanJson("bold", start, end))
                            spans.put(spanJson("italic", start, end))
                        }
                    }
                }
                is UnderlineSpan -> spans.put(spanJson("underline", start, end))
                is AbsoluteSizeSpan -> spans.put(
                    spanJson("size", start, end).put("value", span.size)
                )
                is AlignmentSpan -> {
                    val value = when (span.alignment) {
                        Layout.Alignment.ALIGN_CENTER -> "center"
                        Layout.Alignment.ALIGN_OPPOSITE -> "right"
                        else -> "left"
                    }
                    spans.put(spanJson("align", start, end).put("value", value))
                }
            }
        }
        root.put("spans", spans)
        return root.toString()
    }

    fun decode(json: String, fallback: String = ""): SpannableStringBuilder {
        if (json.isBlank()) return SpannableStringBuilder(fallback)
        return runCatching {
            val root = JSONObject(json)
            val text = root.optString("text", fallback)
            val out = SpannableStringBuilder(text)
            val spans = root.optJSONArray("spans") ?: JSONArray()
            for (i in 0 until spans.length()) {
                val item = spans.getJSONObject(i)
                val start = item.optInt("start").coerceIn(0, out.length)
                val end = item.optInt("end").coerceIn(start, out.length)
                if (end <= start) continue
                when (item.optString("type")) {
                    "bold" -> out.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    "italic" -> out.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    "underline" -> out.setSpan(UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    "size" -> out.setSpan(
                        AbsoluteSizeSpan(item.optInt("value", 18).coerceIn(8, 96), true),
                        start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    "align" -> {
                        val alignment = when (item.optString("value")) {
                            "center" -> Layout.Alignment.ALIGN_CENTER
                            "right" -> Layout.Alignment.ALIGN_OPPOSITE
                            else -> Layout.Alignment.ALIGN_NORMAL
                        }
                        out.setSpan(AlignmentSpan.Standard(alignment), start, end, Spannable.SPAN_PARAGRAPH)
                    }
                }
            }
            out
        }.getOrElse { SpannableStringBuilder(fallback) }
    }

    private fun spanJson(type: String, start: Int, end: Int) = JSONObject()
        .put("type", type)
        .put("start", start)
        .put("end", end)
}
