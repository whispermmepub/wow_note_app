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
import android.widget.OverScroller
import androidx.core.content.getSystemService
import androidx.core.widget.doAfterTextChanged
import java.io.File
import kotlin.math.roundToInt

class RichEditText(context: Context) : EditText(context) {

    var onRichTextChanged: ((plain: String, richJson: String) -> Unit)? = null
    var onEditModeChanged: ((Boolean) -> Unit)? = null

    private var editMode = false
    private val readScroller = OverScroller(context)

    private val gestures = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                if (!readScroller.isFinished) readScroller.forceFinished(true)
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (editMode) return false
                val offset = runCatching { getOffsetForPosition(e.x, e.y) }
                    .getOrDefault(0)
                    .coerceIn(0, editableText.length)
                enterEditMode(showKeyboard = false)
                setSelection(offset)
                focusAndShowKeyboard()
                return true
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (editMode) return false
                val maxY = maximumScrollY()
                if (maxY <= 0) return true
                val nextY = (scrollY + distanceY * READ_SCROLL_MULTIPLIER)
                    .roundToInt()
                    .coerceIn(0, maxY)
                scrollTo(0, nextY)
                return true
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (editMode) return false
                val maxY = maximumScrollY()
                if (maxY <= 0) return true
                readScroller.fling(
                    0,
                    scrollY,
                    0,
                    (-velocityY * READ_FLING_MULTIPLIER).roundToInt(),
                    0,
                    0,
                    0,
                    maxY
                )
                postInvalidateOnAnimation()
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
        isVerticalScrollBarEnabled = true
        scrollBarStyle = SCROLLBARS_INSIDE_OVERLAY
        overScrollMode = OVER_SCROLL_IF_CONTENT_SCROLLS
        setHorizontallyScrolling(false)

        doAfterTextChanged {
            if (!editMode) return@doAfterTextChanged
            val e = editableText ?: return@doAfterTextChanged
            onRichTextChanged?.invoke(e.toString(), RichTextCodec.encode(e))
        }

        enterReadMode()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!editMode) {
            gestures.onTouchEvent(event)
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun computeScroll() {
        if (readScroller.computeScrollOffset()) {
            scrollTo(readScroller.currX, readScroller.currY.coerceIn(0, maximumScrollY()))
            postInvalidateOnAnimation()
        }
    }

    fun loadRichText(json: String, fallback: String, sizeSp: Float, fontPath: String?) {
        editMode = false
        textSize = sizeSp
        setNoteTypeface(fontPath)
        val restored = RichTextCodec.decode(json, fallback)
        setText(restored)
        setSelection(0)
        enterReadMode()
        post {
            setSelection(0)
            scrollTo(0, 0)
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

    fun isEditing(): Boolean = editMode

    fun moveCursorToEnd() {
        val end = editableText.length
        setSelection(end)
        post { bringPointIntoView(end) }
    }

    fun beginEditingAtVisiblePosition() {
        val currentLayout = layout
        val targetOffset = if (currentLayout != null && currentLayout.lineCount > 0) {
            val vertical = (scrollY + (height * 0.30f)).roundToInt()
                .coerceIn(0, currentLayout.height.coerceAtLeast(0))
            val line = currentLayout.getLineForVertical(vertical)
            currentLayout.getLineStart(line).coerceIn(0, editableText.length)
        } else {
            0
        }
        enterEditMode(showKeyboard = false)
        setSelection(targetOffset)
        focusAndShowKeyboard()
    }

    fun selectedText(): String {
        val start = minOf(selectionStart.coerceAtLeast(0), selectionEnd.coerceAtLeast(0))
        val end = maxOf(selectionStart.coerceAtLeast(0), selectionEnd.coerceAtLeast(0))
        return if (end > start) editableText.substring(start, end) else ""
    }

    fun insertAtCursor(value: String) {
        enterEditMode(showKeyboard = false)
        val start = minOf(selectionStart.coerceAtLeast(0), selectionEnd.coerceAtLeast(0))
        val end = maxOf(selectionStart.coerceAtLeast(0), selectionEnd.coerceAtLeast(0))
        editableText.replace(start, end, value)
        setSelection((start + value.length).coerceAtMost(editableText.length))
        emitChange()
    }

    fun replaceSelectionOrAll(value: String) {
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

    fun toggleBold() {
        enterEditMode(false)
        toggleStyle(Typeface.BOLD)
    }

    fun toggleItalic() {
        enterEditMode(false)
        toggleStyle(Typeface.ITALIC)
    }

    fun toggleUnderline() {
        enterEditMode(false)
        val (start, end) = selectedOrWordRange()
        if (end <= start) return
        val spans = editableText.getSpans(start, end, UnderlineSpan::class.java)
        if (spans.isNotEmpty()) spans.forEach(editableText::removeSpan)
        else editableText.setSpan(UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        emitChange()
    }

    fun setSelectionSizeSp(sizeSp: Int) {
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

    fun alignLeft() {
        enterEditMode(false)
        setParagraphAlignment(Layout.Alignment.ALIGN_NORMAL)
    }

    fun alignCenter() {
        enterEditMode(false)
        setParagraphAlignment(Layout.Alignment.ALIGN_CENTER)
    }

    fun alignRight() {
        enterEditMode(false)
        setParagraphAlignment(Layout.Alignment.ALIGN_OPPOSITE)
    }

    fun enterReadMode() {
        val changed = editMode
        editMode = false
        isCursorVisible = false
        isFocusable = false
        isFocusableInTouchMode = false
        showSoftInputOnFocus = false
        clearFocus()
        context.getSystemService<InputMethodManager>()?.hideSoftInputFromWindow(windowToken, 0)
        if (changed) onEditModeChanged?.invoke(false)
    }

    fun enterEditMode(showKeyboard: Boolean = true) {
        val changed = !editMode
        editMode = true
        isFocusable = true
        isFocusableInTouchMode = true
        isCursorVisible = true
        showSoftInputOnFocus = true
        if (changed) onEditModeChanged?.invoke(true)
        if (showKeyboard) focusAndShowKeyboard()
    }

    fun focusAndShowKeyboard() {
        enterEditMode(showKeyboard = false)
        requestFocus()
        post {
            context.getSystemService<InputMethodManager>()
                ?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun maximumScrollY(): Int {
        val currentLayout = layout ?: return 0
        val viewport = (height - compoundPaddingTop - compoundPaddingBottom).coerceAtLeast(0)
        return (currentLayout.height - viewport).coerceAtLeast(0)
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
        editableText.setSpan(
            AlignmentSpan.Standard(alignment),
            start,
            end,
            Spanned.SPAN_PARAGRAPH
        )
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

    companion object {
        private const val READ_SCROLL_MULTIPLIER = 2.35f
        private const val READ_FLING_MULTIPLIER = 1.45f
    }
}
