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
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.content.getSystemService
import androidx.core.widget.doAfterTextChanged
import java.io.File

class RichEditText(context: Context) : EditText(context) {

    var onRichTextChanged: ((plain: String, richJson: String) -> Unit)? = null
    private var editMode = false
    private var suppressNextAutoFocus = false

    private val gestures = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onDoubleTap(e: MotionEvent): Boolean {
                suppressNextAutoFocus = false
                enterEditMode(showKeyboard = true)
                val offset = getOffsetForPosition(e.x, e.y).coerceIn(0, editableText.length)
                setSelection(offset)
                return true
            }
        }
    )

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
            if (!editMode) return@doAfterTextChanged
            val e = editableText ?: return@doAfterTextChanged
            onRichTextChanged?.invoke(e.toString(), RichTextCodec.encode(e))
        }

        enterReadMode()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!editMode) gestures.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    fun loadRichText(json: String, fallback: String, sizeSp: Float, fontPath: String?) {
        val restoreEditing = editMode
        editMode = false
        textSize = sizeSp
        setNoteTypeface(fontPath)
        val restored = RichTextCodec.decode(json, fallback)
        setText(restored)
        setSelection(restored.length)
        suppressNextAutoFocus = restored.isNotBlank() || json.isNotBlank()
        if (restoreEditing) enterEditMode(showKeyboard = false) else enterReadMode()
    }

    fun setNoteTypeface(fontPath: String?) {
        typeface = if (fontPath.isNullOrBlank()) {
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        } else {
            runCatching { Typeface.createFromFile(File(fontPath)) }
                .getOrElse { Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL) }
        }
    }

    fun selectedText(): String {
        val start = minOf(selectionStart.coerceAtLeast(0), selectionEnd.coerceAtLeast(0))
        val end = maxOf(selectionStart.coerceAtLeast(0), selectionEnd.coerceAtLeast(0))
        return if (end > start) editableText.substring(start, end) else ""
    }

    fun insertAtCursor(value: String) {
        suppressNextAutoFocus = false
        enterEditMode(showKeyboard = false)
        val start = minOf(selectionStart.coerceAtLeast(0), selectionEnd.coerceAtLeast(0))
        val end = maxOf(selectionStart.coerceAtLeast(0), selectionEnd.coerceAtLeast(0))
        editableText.replace(start, end, value)
        setSelection((start + value.length).coerceAtMost(editableText.length))
        emitChange()
    }

    fun replaceSelectionOrAll(value: String) {
        suppressNextAutoFocus = false
        enterEditMode(showKeyboard = false)
        val start = minOf(selectionStart.coerceAtLeast(0), selectionEnd.coerceAtLeast(0))
        val end = maxOf(selectionStart.coerceAtLeast(0), selectionEnd.coerceAtLeast(0))
        if (end > start) {
            editableText.replace(start, end, value)
            setSelection((start + value.length).coerceAtMost(editableText.length))
        } else {
            setText(value)
            setSelection(value.length)
        }
        emitChange()
    }

    fun toggleBold() { suppressNextAutoFocus = false; enterEditMode(false); toggleStyle(Typeface.BOLD) }
    fun toggleItalic() { suppressNextAutoFocus = false; enterEditMode(false); toggleStyle(Typeface.ITALIC) }

    fun toggleUnderline() {
        suppressNextAutoFocus = false
        enterEditMode(false)
        val (start, end) = selectedOrWordRange()
        if (end <= start) return
        val spans = editableText.getSpans(start, end, UnderlineSpan::class.java)
        if (spans.isNotEmpty()) spans.forEach(editableText::removeSpan)
        else editableText.setSpan(UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        emitChange()
    }

    fun setSelectionSizeSp(sizeSp: Int) {
        suppressNextAutoFocus = false
        enterEditMode(false)
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

    fun alignLeft() { suppressNextAutoFocus = false; enterEditMode(false); setParagraphAlignment(Layout.Alignment.ALIGN_NORMAL) }
    fun alignCenter() { suppressNextAutoFocus = false; enterEditMode(false); setParagraphAlignment(Layout.Alignment.ALIGN_CENTER) }
    fun alignRight() { suppressNextAutoFocus = false; enterEditMode(false); setParagraphAlignment(Layout.Alignment.ALIGN_OPPOSITE) }

    fun enterReadMode() {
        editMode = false
        isCursorVisible = false
        isFocusable = false
        isFocusableInTouchMode = false
        showSoftInputOnFocus = false
        clearFocus()
        context.getSystemService<InputMethodManager>()?.hideSoftInputFromWindow(windowToken, 0)
    }

    fun enterEditMode(showKeyboard: Boolean = true) {
        suppressNextAutoFocus = false
        editMode = true
        isFocusable = true
        isFocusableInTouchMode = true
        isCursorVisible = true
        showSoftInputOnFocus = true
        if (showKeyboard) focusAndShowKeyboard()
    }

    fun focusAndShowKeyboard() {
        if (suppressNextAutoFocus) {
            suppressNextAutoFocus = false
            enterReadMode()
            return
        }
        editMode = true
        isFocusable = true
        isFocusableInTouchMode = true
        isCursorVisible = true
        showSoftInputOnFocus = true
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
        if (matching.isNotEmpty()) matching.forEach(editableText::removeSpan)
        else editableText.setSpan(StyleSpan(style), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        emitChange()
    }

    private fun setParagraphAlignment(alignment: Layout.Alignment) {
        val (start, end) = paragraphRange()
        if (end <= start) return
        editableText.getSpans(start, end, AlignmentSpan::class.java).forEach(editableText::removeSpan)
        editableText.setSpan(AlignmentSpan.Standard(alignment), start, end, Spanned.SPAN_PARAGRAPH)
        emitChange()
    }

    private fun selectedOrWordRange(): Pair<Int, Int> {
        val rawStart = selectionStart.coerceAtLeast(0)
        val rawEnd = selectionEnd.coerceAtLeast(0)
        val start = minOf(rawStart, rawEnd)
        val end = maxOf(rawStart, rawEnd)
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
