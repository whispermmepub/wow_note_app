package com.whispermmepub.wownote.editor

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Layout
import android.text.Spannable
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.AlignmentSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.content.getSystemService
import androidx.core.widget.doAfterTextChanged
import java.io.File

class RichEditText(context: Context) : EditText(context) {

    var onRichTextChanged: ((plain: String, richJson: String) -> Unit)? = null

    init {
        setTextColor(Color.rgb(20, 20, 22))
        setHintTextColor(Color.rgb(142, 142, 147))
        setBackgroundColor(Color.TRANSPARENT)
        gravity = Gravity.TOP or Gravity.START
        setPadding(0, 0, 0, 0)
        includeFontPadding = false
        setLineSpacing(0f, 1.28f)
        textSize = 18f
        hint = "Start writing…"
        isVerticalScrollBarEnabled = false
        doAfterTextChanged {
            val e = editableText ?: return@doAfterTextChanged
            onRichTextChanged?.invoke(e.toString(), RichTextCodec.encode(e))
        }
    }

    fun loadRichText(json: String, fallback: String, sizeSp: Float, fontPath: String?) {
        textSize = sizeSp
        if (!fontPath.isNullOrBlank()) {
            runCatching { typeface = Typeface.createFromFile(File(fontPath)) }
        } else {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        if (editableText.toString() != fallback || json.isNotBlank()) {
            val restored = RichTextCodec.decode(json, fallback)
            setText(restored)
            setSelection(restored.length)
        }
    }

    fun setNoteTypeface(fontPath: String?) {
        typeface = if (fontPath.isNullOrBlank()) {
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        } else {
            runCatching { Typeface.createFromFile(File(fontPath)) }
                .getOrElse { Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL) }
        }
    }

    fun toggleBold() = toggleStyle(Typeface.BOLD)
    fun toggleItalic() = toggleStyle(Typeface.ITALIC)

    fun toggleUnderline() {
        val (start, end) = selectedOrWordRange()
        if (end <= start) return
        val spans = editableText.getSpans(start, end, UnderlineSpan::class.java)
        if (spans.isNotEmpty()) spans.forEach(editableText::removeSpan)
        else editableText.setSpan(UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        emitChange()
    }

    fun setSelectionSizeSp(sizeSp: Int) {
        val (start, end) = selectedOrWordRange()
        if (end <= start) {
            textSize = sizeSp.toFloat()
            return
        }
        editableText.getSpans(start, end, AbsoluteSizeSpan::class.java).forEach(editableText::removeSpan)
        editableText.setSpan(
            AbsoluteSizeSpan(sizeSp.coerceIn(8, 96), true),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        emitChange()
    }

    fun alignLeft() = setParagraphAlignment(Layout.Alignment.ALIGN_NORMAL)
    fun alignCenter() = setParagraphAlignment(Layout.Alignment.ALIGN_CENTER)
    fun alignRight() = setParagraphAlignment(Layout.Alignment.ALIGN_OPPOSITE)

    fun focusAndShowKeyboard() {
        requestFocus()
        post {
            context.getSystemService<InputMethodManager>()
                ?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun toggleStyle(style: Int) {
        val (start, end) = selectedOrWordRange()
        if (end <= start) return
        val matching = editableText.getSpans(start, end, StyleSpan::class.java)
            .filter { it.style == style || it.style == Typeface.BOLD_ITALIC }
        if (matching.isNotEmpty()) {
            matching.forEach(editableText::removeSpan)
        } else {
            editableText.setSpan(StyleSpan(style), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        emitChange()
    }

    private fun setParagraphAlignment(alignment: Layout.Alignment) {
        val (start, end) = paragraphRange()
        if (end <= start) return
        editableText.getSpans(start, end, AlignmentSpan::class.java).forEach(editableText::removeSpan)
        editableText.setSpan(
            AlignmentSpan.Standard(alignment),
            start,
            end,
            Spanned.SPAN_PARAGRAPH
        )
        emitChange()
    }

    private fun selectedOrWordRange(): Pair<Int, Int> {
        var start = selectionStart.coerceAtLeast(0)
        var end = selectionEnd.coerceAtLeast(0)
        if (start > end) start = end.also { end = start }
        if (start != end) return start to end

        val text = editableText
        var left = start
        var right = start
        while (left > 0 && !text[left - 1].isWhitespace()) left--
        while (right < text.length && !text[right].isWhitespace()) right++
        return left to right
    }

    private fun paragraphRange(): Pair<Int, Int> {
        val text = editableText
        var start = minOf(selectionStart.coerceAtLeast(0), selectionEnd.coerceAtLeast(0))
        var end = maxOf(selectionStart.coerceAtLeast(0), selectionEnd.coerceAtLeast(0))
        while (start > 0 && text[start - 1] != '\n') start--
        while (end < text.length && text[end] != '\n') end++
        if (end < text.length) end++
        return start to end
    }

    private fun emitChange() {
        val e = editableText ?: return
        onRichTextChanged?.invoke(e.toString(), RichTextCodec.encode(e))
        invalidate()
    }
}
