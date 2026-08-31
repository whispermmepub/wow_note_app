from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise SystemExit(f"Patch target not found: {label}")
    return text.replace(old, new, 1)

# ---------------- MainActivity: remove global floating theme overlay ----------------
main_path = Path("app/src/main/java/com/whispermmepub/wownote/MainActivity.kt")
main = main_path.read_text(encoding="utf-8")
main = replace_once(
    main,
    '''                        WoWNoteApp(
                            launchRequest = launchRequest,
                            onLaunchConsumed = { launchRequest = LaunchRequest() }
                        )

                        ThemeFloatingButton(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .statusBarsPadding()
                                .padding(top = 8.dp, end = 14.dp),
                            onClick = { showThemes = true }
                        )''',
    '''                        WoWNoteApp(
                            launchRequest = launchRequest,
                            onLaunchConsumed = { launchRequest = LaunchRequest() },
                            onThemeClick = { showThemes = true }
                        )''',
    "MainActivity global theme overlay"
)
main_path.write_text(main, encoding="utf-8")

# ---------------- WoWNoteApp: theme placement, read/edit UX, calendar scroll ----------------
app_path = Path("app/src/main/java/com/whispermmepub/wownote/WoWNoteApp.kt")
app = app_path.read_text(encoding="utf-8")

app = replace_once(
    app,
    'import androidx.activity.compose.rememberLauncherForActivityResult',
    'import androidx.activity.compose.BackHandler\nimport androidx.activity.compose.rememberLauncherForActivityResult',
    "BackHandler import"
)
app = replace_once(
    app,
    'import androidx.compose.ui.platform.LocalContext',
    'import androidx.compose.ui.platform.LocalContext\nimport androidx.compose.ui.platform.LocalFocusManager',
    "LocalFocusManager import"
)

app = replace_once(
    app,
    '''fun WoWNoteApp(
    launchRequest: LaunchRequest,
    onLaunchConsumed: () -> Unit
) {''',
    '''fun WoWNoteApp(
    launchRequest: LaunchRequest,
    onLaunchConsumed: () -> Unit,
    onThemeClick: () -> Unit
) {''',
    "WoWNoteApp signature"
)

app = replace_once(
    app,
    '''                onOpen = { editingId = it.id },
                onNew = { showCreate = true },
                onSave = ::save,''',
    '''                onOpen = { editingId = it.id },
                onNew = { showCreate = true },
                onThemeClick = onThemeClick,
                onSave = ::save,''',
    "NotesScreen theme callback"
)

app = replace_once(
    app,
    '''private fun NotesScreen(
    notes: List<Note>,
    onOpen: (Note) -> Unit,
    onNew: () -> Unit,
    onSave: (Note) -> Unit,
    onDeleteForever: (Note) -> Unit
) {''',
    '''private fun NotesScreen(
    notes: List<Note>,
    onOpen: (Note) -> Unit,
    onNew: () -> Unit,
    onThemeClick: () -> Unit,
    onSave: (Note) -> Unit,
    onDeleteForever: (Note) -> Unit
) {''',
    "NotesScreen signature"
)

app = replace_once(
    app,
    '''            Spacer(Modifier.height(9.dp))
            Text("WoW Note", fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(13.dp))''',
    '''            Spacer(Modifier.height(9.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "WoW Note",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Your notes, your space",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.52f)
                    )
                }
                FloatingCircleButton(
                    onClick = onThemeClick,
                    modifier = Modifier.size(42.dp),
                    elevation = 8.dp,
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Icon(
                        Icons.Rounded.Palette,
                        "App theme",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Center).size(20.dp)
                    )
                }
            }
            Spacer(Modifier.height(13.dp))''',
    "Notes home theme button"
)

app = replace_once(
    app,
    '''    val context = LocalContext.current
    var draft by remember(original.id) { mutableStateOf(original) }
    var editor by remember(original.id) { mutableStateOf<RichEditText?>(null) }
    var showSize by remember { mutableStateOf(false) }
    var showBackground by remember { mutableStateOf(false) }
    var showAi by remember { mutableStateOf(false) }
    var aiSelection by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }

    fun saveChanged(next: Note) {''',
    '''    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var draft by remember(original.id) { mutableStateOf(original) }
    var editor by remember(original.id) { mutableStateOf<RichEditText?>(null) }
    var isEditing by remember(original.id) {
        mutableStateOf(original.content.isBlank() || original.content == "[ ] ")
    }
    var showSize by remember { mutableStateOf(false) }
    var showBackground by remember { mutableStateOf(false) }
    var showAi by remember { mutableStateOf(false) }
    var aiSelection by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }

    fun leaveEditMode() {
        isEditing = false
        editor?.enterReadMode()
        focusManager.clearFocus(force = true)
    }

    fun handleEditorBack() {
        if (isEditing) leaveEditMode() else onBack()
    }

    BackHandler(enabled = true) { handleEditorBack() }

    fun saveChanged(next: Note) {''',
    "Editor read mode state and back handling"
)

app = replace_once(
    app,
    '''    val docxExport = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    ) { uri ->
        if (uri != null) {
            runCatching { DocxExporter.export(context, uri, draft) }
                .onSuccess { status = "DOCX export ပြီးပြီ" }
                .onFailure { status = "DOCX export မအောင်မြင်ပါ" }
        }
    }

    val voiceLauncher''',
    '''    val docxExport = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    ) { uri ->
        if (uri != null) {
            runCatching { DocxExporter.export(context, uri, draft) }
                .onSuccess { status = "DOCX export ပြီးပြီ" }
                .onFailure { status = "DOCX export မအောင်မြင်ပါ" }
        }
    }

    fun launchDocxExport() {
        val safe = draft.title
            .ifBlank { "WoW-Note" }
            .replace(Regex("[^A-Za-z0-9က-အ]+"), "-")
            .trim('-')
            .take(50)
            .ifBlank { "WoW-Note" }
        docxExport.launch("$safe.docx")
    }

    val voiceLauncher''',
    "DOCX launcher helper"
)

old_header = '''            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FloatingCircleButton(
                    onClick = onBack,
                    modifier = Modifier.size(42.dp),
                    elevation = 10.dp
                ) {
                    Icon(Icons.Rounded.ArrowBack, "Back", modifier = Modifier.align(Alignment.Center).size(21.dp))
                }
                Spacer(Modifier.width(11.dp))
                BasicTextField(
                    value = draft.title,
                    onValueChange = { saveChanged(draft.copy(title = it)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF171719)),
                    decorationBox = { inner ->
                        Box {
                            if (draft.title.isBlank()) Text("Title", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E8E93))
                            inner()
                        }
                    }
                )
                FloatingCircleButton(
                    onClick = {
                        val safe = draft.title.ifBlank { "WoW-Note" }.replace(Regex("[^A-Za-z0-9က-အ]+"), "-").take(50)
                        docxExport.launch("$safe.docx")
                    },
                    modifier = Modifier.size(42.dp),
                    elevation = 10.dp
                ) {
                    Icon(Icons.Rounded.FileDownload, "Export DOCX", modifier = Modifier.align(Alignment.Center).size(20.dp))
                }
            }'''

new_header = '''            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FloatingCircleButton(
                    onClick = { handleEditorBack() },
                    modifier = Modifier.size(40.dp),
                    elevation = 8.dp,
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Icon(
                        Icons.Rounded.ArrowBack,
                        "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Center).size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                if (isEditing) {
                    BasicTextField(
                        value = draft.title,
                        onValueChange = { saveChanged(draft.copy(title = it)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        decorationBox = { inner ->
                            Box {
                                if (draft.title.isBlank()) {
                                    Text(
                                        "Title",
                                        fontSize = 21.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                    )
                                }
                                inner()
                            }
                        }
                    )
                } else {
                    Text(
                        draft.title.ifBlank { if (draft.type == NoteType.CHECKLIST) "Checklist" else "Untitled" },
                        modifier = Modifier.weight(1f),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.width(7.dp))
                FloatingCircleButton(
                    onClick = {
                        if (isEditing) {
                            leaveEditMode()
                        } else {
                            isEditing = true
                            editor?.beginEditingAtVisiblePosition()
                        }
                    },
                    modifier = Modifier.size(40.dp),
                    elevation = 8.dp,
                    backgroundColor = if (isEditing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                ) {
                    Icon(
                        if (isEditing) Icons.Rounded.Check else Icons.Rounded.Edit,
                        if (isEditing) "Done editing" else "Edit note",
                        tint = if (isEditing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Center).size(19.dp)
                    )
                }
                Spacer(Modifier.width(7.dp))
                FloatingCircleButton(
                    onClick = { launchDocxExport() },
                    modifier = Modifier.size(40.dp),
                    elevation = 8.dp,
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Icon(
                        Icons.Rounded.FileDownload,
                        "Export DOCX",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Center).size(19.dp)
                    )
                }
            }'''
app = replace_once(app, old_header, new_header, "Editor top bar")

app = replace_once(
    app,
    '''                .statusBarsPadding()
                .imePadding()
                .padding(horizontal = 15.dp)''',
    '''                .statusBarsPadding()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 15.dp)''',
    "Editor navigation bar padding"
)

app = replace_once(
    app,
    '''                backgroundColor = Color.White.copy(alpha = if (draft.background.type == NoteBackgroundType.IMAGE) 0.82f else 0.93f),''',
    '''                backgroundColor = MaterialTheme.colorScheme.surface.copy(
                    alpha = if (draft.background.type == NoteBackgroundType.IMAGE) 0.84f else 0.96f
                ),''',
    "Editor content surface theme"
)

old_android_view = '''                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        RichEditText(ctx).also { view ->
                            editor = view
                            view.loadRichText(draft.richTextJson, draft.content, draft.defaultFontSizeSp, draft.customFontPath)
                            view.onRichTextChanged = { plain, rich ->
                                val next = draft.copy(content = plain, richTextJson = rich, updatedAt = System.currentTimeMillis())
                                draft = next
                                onSave(next)
                            }
                            view.focusAndShowKeyboard()
                        }
                    },
                    update = { view ->
                        editor = view
                        view.setNoteTypeface(draft.customFontPath)
                    }
                )'''

new_android_view = '''                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        RichEditText(ctx).also { view ->
                            editor = view
                            view.loadRichText(
                                draft.richTextJson,
                                draft.content,
                                draft.defaultFontSizeSp,
                                draft.customFontPath
                            )
                            view.onRichTextChanged = { plain, rich ->
                                val next = draft.copy(
                                    content = plain,
                                    richTextJson = rich,
                                    updatedAt = System.currentTimeMillis()
                                )
                                draft = next
                                onSave(next)
                            }
                            view.onEditModeChanged = { active -> isEditing = active }
                            if (isEditing) {
                                view.enterEditMode(showKeyboard = false)
                                view.moveCursorToEnd()
                                view.post { view.focusAndShowKeyboard() }
                            }
                        }
                    },
                    update = { view ->
                        editor = view
                        view.setNoteTypeface(draft.customFontPath)
                        if (!isEditing && view.isEditing()) view.enterReadMode()
                    }
                )'''
app = replace_once(app, old_android_view, new_android_view, "Editor AndroidView")

old_toolbar = '''            FloatingSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp),
                cornerRadius = 24.dp,
                elevation = 18.dp,
                backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 7.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ToolButton(Icons.Rounded.FormatBold, "Bold") { editor?.toggleBold() }
                    ToolButton(Icons.Rounded.FormatItalic, "Italic") { editor?.toggleItalic() }
                    ToolButton(Icons.Rounded.FormatUnderlined, "Underline") { editor?.toggleUnderline() }
                    ToolButton(Icons.Rounded.TextFields, "Size") { showSize = true }
                    ToolButton(Icons.Rounded.FormatAlignLeft, "Left") { editor?.alignLeft() }
                    ToolButton(Icons.Rounded.FormatAlignCenter, "Center") { editor?.alignCenter() }
                    ToolButton(Icons.Rounded.FormatAlignRight, "Right") { editor?.alignRight() }
                    ToolButton(Icons.Rounded.FontDownload, "Font") {
                        fontPicker.launch(arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/x-font-opentype"))
                    }
                    ToolButton(Icons.Rounded.Palette, "Background") { showBackground = true }
                    ToolButton(Icons.Rounded.Mic, "Myanmar voice") { launchVoice() }
                    ToolButton(Icons.Rounded.NotificationsActive, "Reminder") { pickReminder() }
                    ToolButton(Icons.Rounded.AutoAwesome, "WoW AI") {
                        aiSelection = editor?.selectedText().orEmpty()
                        showAi = true
                    }
                }
            }'''

new_toolbar = '''            if (isEditing) {
                FloatingSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    cornerRadius = 24.dp,
                    elevation = 18.dp,
                    backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 7.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ToolButton(Icons.Rounded.FormatBold, "Bold") { editor?.toggleBold() }
                        ToolButton(Icons.Rounded.FormatItalic, "Italic") { editor?.toggleItalic() }
                        ToolButton(Icons.Rounded.FormatUnderlined, "Underline") { editor?.toggleUnderline() }
                        ToolButton(Icons.Rounded.TextFields, "Size") { showSize = true }
                        ToolButton(Icons.Rounded.FormatAlignLeft, "Left") { editor?.alignLeft() }
                        ToolButton(Icons.Rounded.FormatAlignCenter, "Center") { editor?.alignCenter() }
                        ToolButton(Icons.Rounded.FormatAlignRight, "Right") { editor?.alignRight() }
                        ToolButton(Icons.Rounded.FontDownload, "Font") {
                            fontPicker.launch(arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/x-font-opentype"))
                        }
                        ToolButton(Icons.Rounded.Palette, "Background") { showBackground = true }
                        ToolButton(Icons.Rounded.Mic, "Myanmar voice") { launchVoice() }
                        ToolButton(Icons.Rounded.NotificationsActive, "Reminder") { pickReminder() }
                        ToolButton(Icons.Rounded.AutoAwesome, "WoW AI") {
                            aiSelection = editor?.selectedText().orEmpty()
                            showAi = true
                        }
                    }
                }
            }'''
app = replace_once(app, old_toolbar, new_toolbar, "Hide formatting toolbar in read mode")

app = replace_once(
    app,
    '''    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 13.dp)
    ) {''',
    '''    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 13.dp)
            .padding(bottom = 118.dp)
    ) {''',
    "Scrollable calendar"
)

app = replace_once(
    app,
    '''            modifier = Modifier.fillMaxWidth().padding(bottom = 92.dp),''',
    '''            modifier = Modifier.fillMaxWidth(),''',
    "Calendar today card bottom overlap"
)

# Theme-aware calendar cards/text, while keeping today's visual emphasis.
app = app.replace(
    'backgroundColor = if (isToday) Color(0xFFE6F2FF) else Color.White.copy(alpha = 0.94f),',
    'backgroundColor = if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),'
    1
)
app = app.replace(
    'color = if (isToday) Color(0xFF007AFF) else Color(0xFF171719)',
    'color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface',
    1
)

app_path.write_text(app, encoding="utf-8")

# ---------------- RichEditText: top-on-open + accelerated read scrolling/fling ----------------
rich_path = Path("app/src/main/java/com/whispermmepub/wownote/editor/RichEditText.kt")
rich_path.write_text(r'''package com.whispermmepub.wownote.editor

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
''', encoding="utf-8")

# ---------------- Version bump ----------------
gradle_path = Path("app/build.gradle.kts")
gradle = gradle_path.read_text(encoding="utf-8")
gradle = replace_once(gradle, 'versionCode = 2', 'versionCode = 3', "versionCode")
gradle = replace_once(gradle, 'versionName = "1.0.1"', 'versionName = "1.0.2"', "versionName")
gradle_path.write_text(gradle, encoding="utf-8")

print("UX cleanup patch applied successfully")
