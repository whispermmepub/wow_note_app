package com.whispermmepub.wownote

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.FormatAlignCenter
import androidx.compose.material.icons.rounded.FormatAlignLeft
import androidx.compose.material.icons.rounded.FormatAlignRight
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.FormatUnderlined
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.whispermmepub.wownote.calendar.MyanmarCalendarUtil
import com.whispermmepub.wownote.data.NoteStore
import com.whispermmepub.wownote.editor.RichEditText
import com.whispermmepub.wownote.io.DocxExporter
import com.whispermmepub.wownote.io.LocalAssetManager
import com.whispermmepub.wownote.model.Note
import com.whispermmepub.wownote.model.NoteBackground
import com.whispermmepub.wownote.model.NoteBackgroundType
import com.whispermmepub.wownote.model.NoteType
import com.whispermmepub.wownote.ui.components.FloatingCircleButton
import com.whispermmepub.wownote.ui.components.FloatingPressable
import com.whispermmepub.wownote.ui.components.FloatingSurface
import com.whispermmepub.wownote.ui.theme.WoWNoteTheme
import mmcalendar.CalendarType
import mmcalendar.Config
import mmcalendar.Language
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Config.initDefault(
            Config.Builder()
                .setCalendarType(CalendarType.ENGLISH)
                .setLanguage(Language.MYANMAR)
                .build()
        )
        setContent {
            WoWNoteTheme { WoWNoteApp() }
        }
    }
}

private enum class AppTab { NOTES, CALENDAR }
private enum class NoteSection { NOTES, ARCHIVE, TRASH }

@Composable
private fun WoWNoteApp() {
    val context = LocalContext.current
    val store = remember { NoteStore(context) }
    var notes by remember { mutableStateOf(store.all()) }
    var tab by rememberSaveable { mutableStateOf(AppTab.NOTES) }
    var section by rememberSaveable { mutableStateOf(NoteSection.NOTES) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var showCreate by remember { mutableStateOf(false) }

    fun refresh() { notes = store.all() }
    fun save(note: Note) {
        store.upsert(note)
        refresh()
    }

    val editing = editingId?.let { id -> notes.firstOrNull { it.id == id } ?: store.get(id) }
    if (editing != null) {
        EditorScreen(
            original = editing,
            onSave = ::save,
            onBack = {
                refresh()
                editingId = null
            }
        )
        return
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (tab) {
            AppTab.NOTES -> NotesHome(
                notes = notes,
                section = section,
                onSection = { section = it },
                onOpen = { editingId = it.id },
                onPin = { save(it.copy(pinned = !it.pinned, updatedAt = System.currentTimeMillis())) },
                onArchive = {
                    save(it.copy(archived = !it.archived, deleted = false, updatedAt = System.currentTimeMillis()))
                },
                onTrash = {
                    if (section == NoteSection.TRASH) {
                        store.deleteForever(it.id)
                        refresh()
                    } else {
                        save(it.copy(deleted = true, archived = false, updatedAt = System.currentTimeMillis()))
                    }
                }
            )
            AppTab.CALENDAR -> MyanmarCalendarScreen(notes)
        }

        FloatingBottomTabs(
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        )

        if (tab == AppTab.NOTES) {
            FloatingCircleButton(
                onClick = { showCreate = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 20.dp, bottom = 88.dp)
                    .size(58.dp),
                backgroundColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = "New note",
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.Center).size(30.dp)
                )
            }
        }
    }

    if (showCreate) {
        ChoiceDialog(title = "New Note", onDismiss = { showCreate = false }) {
            ChoiceRow(Icons.Rounded.Notes, "Text Note") {
                val note = Note(type = NoteType.TEXT)
                save(note)
                editingId = note.id
                showCreate = false
            }
            Spacer(Modifier.height(10.dp))
            ChoiceRow(Icons.Rounded.CheckBox, "Checklist") {
                val note = Note(type = NoteType.CHECKLIST, content = "[ ] ")
                save(note)
                editingId = note.id
                showCreate = false
            }
        }
    }
}

@Composable
private fun NotesHome(
    notes: List<Note>,
    section: NoteSection,
    onSection: (NoteSection) -> Unit,
    onOpen: (Note) -> Unit,
    onPin: (Note) -> Unit,
    onArchive: (Note) -> Unit,
    onTrash: (Note) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = notes.filter {
        when (section) {
            NoteSection.NOTES -> !it.deleted && !it.archived
            NoteSection.ARCHIVE -> !it.deleted && it.archived
            NoteSection.TRASH -> it.deleted
        }
    }.filter {
        query.isBlank() || it.title.contains(query, true) || it.content.contains(query, true)
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        Text(
            "WoW Note",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(14.dp))
        FloatingSurface(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 18.dp,
            elevation = 10.dp,
            backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 11.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Search, null, tint = Color(0xFF8E8E93), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(9.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (query.isBlank()) Text("Search", color = Color(0xFF8E8E93), fontSize = 17.sp)
                        inner()
                    }
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        FloatingSegmented(section, onSection)
        Spacer(Modifier.height(14.dp))

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(bottom = 110.dp), contentAlignment = Alignment.Center) {
                Text(
                    when (section) {
                        NoteSection.NOTES -> "No notes yet"
                        NoteSection.ARCHIVE -> "Archive is empty"
                        NoteSection.TRASH -> "Trash is empty"
                    },
                    color = Color(0xFF8E8E93),
                    fontSize = 18.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 128.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(filtered, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        permanentDelete = section == NoteSection.TRASH,
                        onOpen = { onOpen(note) },
                        onPin = { onPin(note) },
                        onArchive = { onArchive(note) },
                        onTrash = { onTrash(note) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingSegmented(selected: NoteSection, onSelect: (NoteSection) -> Unit) {
    FloatingSurface(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 17.dp,
        elevation = 8.dp,
        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        contentPadding = PaddingValues(4.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            listOf(
                NoteSection.NOTES to "Notes",
                NoteSection.ARCHIVE to "Archive",
                NoteSection.TRASH to "Trash"
            ).forEach { (value, label) ->
                val active = selected == value
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(13.dp))
                        .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .combinedClickable(onClick = { onSelect(value) })
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = if (active) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteCard(
    note: Note,
    permanentDelete: Boolean,
    onOpen: () -> Unit,
    onPin: () -> Unit,
    onArchive: () -> Unit,
    onTrash: () -> Unit
) {
    FloatingSurface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onOpen, onLongClick = onPin),
        cornerRadius = 24.dp,
        elevation = 13.dp,
        backgroundColor = Color.Transparent
    ) {
        Box(Modifier.fillMaxWidth().height(150.dp)) {
            NoteBackgroundLayer(note.background, note.colorArgb)
            Column(Modifier.fillMaxSize().padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        note.title.ifBlank { if (note.type == NoteType.CHECKLIST) "Checklist" else "Untitled" },
                        modifier = Modifier.weight(1f),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color(0xFF151517)
                    )
                    if (note.pinned) Icon(Icons.Rounded.PushPin, null, tint = Color(0xFF007AFF), modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    note.content.replace("[ ] ", "☐ ").replace("[x] ", "☑ "),
                    modifier = Modifier.weight(1f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    color = Color(0xFF3A3A3C)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                    MiniAction(Icons.Rounded.PushPin, onPin)
                    MiniAction(Icons.Rounded.Archive, onArchive)
                    MiniAction(Icons.Rounded.Delete, onTrash)
                    Spacer(Modifier.weight(1f))
                    Text(relativeTime(note.updatedAt), fontSize = 12.sp, color = Color(0xFF8E8E93))
                }
            }
        }
    }
}

@Composable
private fun MiniAction(icon: ImageVector, onClick: () -> Unit) {
    FloatingCircleButton(
        onClick = onClick,
        modifier = Modifier.size(30.dp),
        elevation = 5.dp,
        backgroundColor = Color.White.copy(alpha = 0.84f)
    ) {
        Icon(icon, null, tint = Color(0xFF505054), modifier = Modifier.align(Alignment.Center).size(15.dp))
    }
}

@Composable
private fun FloatingBottomTabs(selected: AppTab, onSelect: (AppTab) -> Unit, modifier: Modifier = Modifier) {
    FloatingSurface(
        modifier = modifier.width(210.dp),
        cornerRadius = 30.dp,
        elevation = 18.dp,
        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        contentPadding = PaddingValues(5.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            BottomTab(Icons.Rounded.Notes, "Notes", selected == AppTab.NOTES, Modifier.weight(1f)) { onSelect(AppTab.NOTES) }
            BottomTab(Icons.Rounded.CalendarMonth, "Calendar", selected == AppTab.CALENDAR, Modifier.weight(1f)) { onSelect(AppTab.CALENDAR) }
        }
    }
}

@Composable
private fun BottomTab(icon: ImageVector, label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    FloatingPressable(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shapeRadius = 24.dp,
        elevation = if (active) 6.dp else 0.dp,
        backgroundColor = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
        haptics = true
    ) {
        Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (active) Color.White else Color(0xFF8E8E93), modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (active) Color.White else Color(0xFF6E6E73))
        }
    }
}

@Composable
private fun EditorScreen(original: Note, onSave: (Note) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var draft by remember(original.id) { mutableStateOf(original) }
    var showSizes by remember { mutableStateOf(false) }
    var showBackground by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val editorRef = remember(original.id) { mutableStateOf<RichEditText?>(null) }

    fun save(next: Note) {
        draft = next
        onSave(next)
    }

    val fontLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { LocalAssetManager.importFont(context, uri) }
                .onSuccess { asset ->
                    val next = draft.copy(
                        customFontPath = asset.path,
                        customFontName = asset.displayName,
                        updatedAt = System.currentTimeMillis()
                    )
                    editorRef.value?.setNoteTypeface(asset.path)
                    save(next)
                    message = "Font: ${asset.displayName}"
                }
                .onFailure { message = "This font could not be opened" }
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { LocalAssetManager.importBackgroundImage(context, uri) }
                .onSuccess { asset ->
                    save(
                        draft.copy(
                            background = draft.background.copy(type = NoteBackgroundType.IMAGE, imageUri = asset.path),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    showBackground = false
                }
                .onFailure { message = "This image could not be opened" }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    ) { uri ->
        if (uri != null) {
            runCatching { DocxExporter.export(context, uri, draft) }
                .onSuccess { message = "DOCX exported" }
                .onFailure { message = "DOCX export failed" }
        }
    }

    Box(Modifier.fillMaxSize()) {
        NoteBackgroundLayer(draft.background, draft.colorArgb)
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FloatingCircleButton(
                    onClick = onBack,
                    modifier = Modifier.size(44.dp),
                    backgroundColor = Color.White.copy(alpha = 0.92f)
                ) {
                    Icon(Icons.Rounded.ArrowBack, "Back", tint = Color(0xFF111113), modifier = Modifier.align(Alignment.Center))
                }
                Spacer(Modifier.weight(1f))
                FloatingCircleButton(
                    onClick = {
                        save(draft.copy(pinned = !draft.pinned, updatedAt = System.currentTimeMillis()))
                    },
                    modifier = Modifier.size(44.dp),
                    backgroundColor = Color.White.copy(alpha = 0.92f)
                ) {
                    Icon(Icons.Rounded.PushPin, "Pin", tint = if (draft.pinned) Color(0xFF007AFF) else Color(0xFF111113), modifier = Modifier.align(Alignment.Center))
                }
                Spacer(Modifier.width(10.dp))
                FloatingCircleButton(
                    onClick = {
                        val base = draft.title.ifBlank { "WoW Note" }.replace(Regex("[^\\p{L}\\p{N} _-]"), "_")
                        exportLauncher.launch("$base.docx")
                    },
                    modifier = Modifier.size(44.dp),
                    backgroundColor = Color.White.copy(alpha = 0.92f)
                ) {
                    Icon(Icons.Rounded.FileDownload, "Export DOCX", tint = Color(0xFF007AFF), modifier = Modifier.align(Alignment.Center))
                }
            }

            FloatingSurface(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 22.dp,
                elevation = 10.dp,
                backgroundColor = Color.White.copy(alpha = 0.82f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 13.dp)
            ) {
                BasicTextField(
                    value = draft.title,
                    onValueChange = { save(draft.copy(title = it, updatedAt = System.currentTimeMillis())) },
                    textStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111113)),
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (draft.title.isBlank()) Text("Title", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E8E93))
                        inner()
                    }
                )
            }
            Spacer(Modifier.height(12.dp))

            FloatingSurface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                cornerRadius = 26.dp,
                elevation = 13.dp,
                backgroundColor = Color.White.copy(alpha = 0.80f),
                contentPadding = PaddingValues(18.dp)
            ) {
                if (draft.type == NoteType.CHECKLIST) {
                    ChecklistEditor(draft) { save(it) }
                } else {
                    AndroidView(
                        factory = { ctx ->
                            RichEditText(ctx).also { editor ->
                                editor.loadRichText(draft.richTextJson, draft.content, draft.defaultFontSizeSp, draft.customFontPath)
                                editor.onRichTextChanged = { plain, rich ->
                                    val next = draft.copy(
                                        content = plain,
                                        richTextJson = rich,
                                        updatedAt = System.currentTimeMillis()
                                    )
                                    draft = next
                                    onSave(next)
                                }
                                editorRef.value = editor
                            }
                        },
                        update = { editor ->
                            editor.setNoteTypeface(draft.customFontPath)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            if (draft.type == NoteType.TEXT) {
                FloatingSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(bottom = 10.dp),
                    cornerRadius = 23.dp,
                    elevation = 18.dp,
                    backgroundColor = Color.White.copy(alpha = 0.94f),
                    contentPadding = PaddingValues(horizontal = 7.dp, vertical = 7.dp)
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        item { FormatAction(Icons.Rounded.FormatBold, "Bold") { editorRef.value?.toggleBold() } }
                        item { FormatAction(Icons.Rounded.FormatItalic, "Italic") { editorRef.value?.toggleItalic() } }
                        item { FormatAction(Icons.Rounded.FormatUnderlined, "Underline") { editorRef.value?.toggleUnderline() } }
                        item { FormatAction(Icons.Rounded.TextFields, "Size") { showSizes = true } }
                        item { FormatAction(Icons.Rounded.FormatAlignLeft, "Left") { editorRef.value?.alignLeft() } }
                        item { FormatAction(Icons.Rounded.FormatAlignCenter, "Center") { editorRef.value?.alignCenter() } }
                        item { FormatAction(Icons.Rounded.FormatAlignRight, "Right") { editorRef.value?.alignRight() } }
                        item { FormatAction(Icons.Rounded.FontDownload, "Font") { fontLauncher.launch(arrayOf("font/*", "application/x-font-ttf", "application/x-font-opentype", "*/*")) } }
                        item { FormatAction(Icons.Rounded.Palette, "Background") { showBackground = true } }
                    }
                }
            } else {
                Spacer(Modifier.navigationBarsPadding().height(12.dp))
            }
        }

        message?.let { text ->
            FloatingSurface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 70.dp),
                cornerRadius = 18.dp,
                elevation = 16.dp,
                backgroundColor = Color(0xEE1C1C1E),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) { Text(text, color = Color.White, fontSize = 14.sp) }
        }
    }

    if (showSizes) {
        FontSizeDialog(
            onDismiss = { showSizes = false },
            onSize = { size ->
                val editor = editorRef.value
                if (editor != null && editor.selectionStart == editor.selectionEnd) {
                    editor.textSize = size.toFloat()
                    save(draft.copy(defaultFontSizeSp = size.toFloat(), updatedAt = System.currentTimeMillis()))
                } else {
                    editor?.setSelectionSizeSp(size)
                }
                showSizes = false
            }
        )
    }

    if (showBackground) {
        BackgroundDialog(
            background = draft.background,
            onDismiss = { showBackground = false },
            onChange = { bg -> save(draft.copy(background = bg, updatedAt = System.currentTimeMillis())) },
            onPickImage = { imageLauncher.launch(arrayOf("image/*")) }
        )
    }
}

@Composable
private fun FormatAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    FloatingPressable(
        onClick = onClick,
        modifier = Modifier.height(42.dp),
        shapeRadius = 16.dp,
        elevation = 5.dp,
        backgroundColor = Color(0xFFF2F2F7)
    ) {
        Row(Modifier.align(Alignment.Center).padding(horizontal = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, label, tint = Color(0xFF222224), modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(5.dp))
            Text(label, fontSize = 12.sp, color = Color(0xFF3A3A3C))
        }
    }
}

private data class CheckItem(var checked: Boolean, var text: String)

@Composable
private fun ChecklistEditor(note: Note, onSave: (Note) -> Unit) {
    val initial = note.content.lines().filter { it.isNotEmpty() }.map {
        when {
            it.startsWith("[x] ") -> CheckItem(true, it.removePrefix("[x] "))
            it.startsWith("[ ] ") -> CheckItem(false, it.removePrefix("[ ] "))
            else -> CheckItem(false, it)
        }
    }.ifEmpty { listOf(CheckItem(false, "")) }
    val items = remember(note.id) { mutableStateListOf<CheckItem>().apply { addAll(initial) } }

    fun persist() {
        val content = items.joinToString("\n") { (if (it.checked) "[x] " else "[ ] ") + it.text }
        onSave(note.copy(content = content, updatedAt = System.currentTimeMillis()))
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items.size) { index ->
                val item = items[index]
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = item.checked,
                        onCheckedChange = {
                            items[index] = item.copy(checked = it)
                            persist()
                        }
                    )
                    BasicTextField(
                        value = item.text,
                        onValueChange = {
                            items[index] = item.copy(text = it)
                            persist()
                        },
                        textStyle = TextStyle(fontSize = 18.sp, color = Color(0xFF171719)),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        FloatingPressable(
            onClick = {
                items += CheckItem(false, "")
                persist()
            },
            modifier = Modifier.height(44.dp),
            shapeRadius = 18.dp,
            backgroundColor = Color(0xFFF2F2F7),
            elevation = 6.dp
        ) {
            Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Add, null, tint = Color(0xFF007AFF), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add item", color = Color(0xFF007AFF), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun FontSizeDialog(onDismiss: () -> Unit, onSize: (Int) -> Unit) {
    ChoiceDialog("Font Size", onDismiss) {
        val sizes = listOf(12, 14, 16, 18, 20, 22, 24, 28, 32, 36, 42, 48)
        sizes.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { size ->
                    FloatingPressable(
                        onClick = { onSize(size) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shapeRadius = 15.dp,
                        elevation = 5.dp,
                        backgroundColor = Color(0xFFF2F2F7)
                    ) {
                        Text("$size", modifier = Modifier.align(Alignment.Center), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun BackgroundDialog(
    background: NoteBackground,
    onDismiss: () -> Unit,
    onChange: (NoteBackground) -> Unit,
    onPickImage: () -> Unit
) {
    ChoiceDialog("Note Background", onDismiss) {
        Text("Solid", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF6E6E73))
        Spacer(Modifier.height(8.dp))
        val solids = listOf(0xFFFFFFFF, 0xFFFFF4CC, 0xFFE9F7EF, 0xFFE7F1FF, 0xFFF6E8FF, 0xFFFFE8E8)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            solids.forEach { argb ->
                FloatingCircleButton(
                    onClick = { onChange(NoteBackground(type = NoteBackgroundType.SOLID, solidArgb = argb)) },
                    modifier = Modifier.size(42.dp),
                    elevation = 7.dp,
                    backgroundColor = Color(argb.toULong())
                ) {}
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("Gradient", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF6E6E73))
        Spacer(Modifier.height(8.dp))
        val gradients = listOf(
            0xFFFFF3C4 to 0xFFFFE0E8,
            0xFFE5F4FF to 0xFFEDE5FF,
            0xFFE5FFE9 to 0xFFFFF4D8,
            0xFFFFE6F1 to 0xFFE8ECFF
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            gradients.forEach { (a, b) ->
                FloatingPressable(
                    onClick = { onChange(NoteBackground(type = NoteBackgroundType.GRADIENT, gradientStartArgb = a, gradientEndArgb = b)) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shapeRadius = 15.dp,
                    elevation = 7.dp,
                    backgroundColor = Color.Transparent
                ) {
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.linearGradient(listOf(Color(a.toULong()), Color(b.toULong())))
                        )
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        ChoiceRow(Icons.Rounded.Palette, "Choose Photo") { onPickImage() }
        if (background.type == NoteBackgroundType.IMAGE) {
            Spacer(Modifier.height(12.dp))
            Text("Photo opacity", fontSize = 13.sp, color = Color(0xFF6E6E73))
            Slider(
                value = background.imageOpacity,
                onValueChange = { onChange(background.copy(imageOpacity = it)) },
                valueRange = 0.2f..1f
            )
            Text("Blur", fontSize = 13.sp, color = Color(0xFF6E6E73))
            Slider(
                value = background.imageBlurDp,
                onValueChange = { onChange(background.copy(imageBlurDp = it)) },
                valueRange = 0f..24f
            )
        }
        Spacer(Modifier.height(10.dp))
        ChoiceRow(Icons.Rounded.Delete, "Use Default Background") {
            onChange(NoteBackground())
            onDismiss()
        }
    }
}

@Composable
private fun ChoiceDialog(title: String, onDismiss: () -> Unit, content: @Composable Column.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        FloatingSurface(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 28.dp,
            elevation = 24.dp,
            backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            contentPadding = PaddingValues(18.dp)
        ) {
            Column {
                Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                content()
            }
        }
    }
}

@Composable
private fun ChoiceRow(icon: ImageVector, text: String, onClick: () -> Unit) {
    FloatingPressable(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shapeRadius = 17.dp,
        elevation = 6.dp,
        backgroundColor = Color(0xFFF2F2F7)
    ) {
        Row(Modifier.align(Alignment.CenterStart).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color(0xFF007AFF), modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun NoteBackgroundLayer(background: NoteBackground, fallbackArgb: Long) {
    val default = Color(fallbackArgb.toULong())
    when (background.type) {
        NoteBackgroundType.DEFAULT -> Box(Modifier.fillMaxSize().background(if (fallbackArgb == 0xFFFFFFFF) Color(0xFFFDFDFE) else default))
        NoteBackgroundType.SOLID -> Box(Modifier.fillMaxSize().background(Color((background.solidArgb ?: fallbackArgb).toULong())))
        NoteBackgroundType.GRADIENT -> {
            val a = Color((background.gradientStartArgb ?: 0xFFFFFFFF).toULong())
            val b = Color((background.gradientEndArgb ?: 0xFFF2F2F7).toULong())
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(a, b))))
        }
        NoteBackgroundType.IMAGE -> {
            val bitmap = remember(background.imageUri) {
                background.imageUri?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
            }
            if (bitmap == null) {
                Box(Modifier.fillMaxSize().background(default))
            } else {
                Box(Modifier.fillMaxSize()) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().blur(background.imageBlurDp.dp).alpha(background.imageOpacity)
                    )
                    Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.16f)))
                }
            }
        }
    }
}

@Composable
private fun MyanmarCalendarScreen(notes: List<Note>) {
    var month by rememberSaveable { mutableStateOf(YearMonth.now()) }
    var selected by rememberSaveable { mutableStateOf(LocalDate.now()) }
    val days = remember(month) { MyanmarCalendarUtil.month(month.year, month.monthValue) }
    val selectedInfo = remember(selected) { MyanmarCalendarUtil.info(selected) }
    val leading = month.atDay(1).dayOfWeek.value % 7 // Sunday first
    val week = listOf("နွေ", "လာ", "ဂါ", "ဟူး", "တေး", "ကြာ", "နေ")
    val noteDates = remember(notes) {
        notes.filter { !it.deleted }.groupBy {
            Instant.ofEpochMilli(it.updatedAt).atZone(ZoneId.systemDefault()).toLocalDate()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        Text("မြန်မာ ပြက္ခဒိန်", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        FloatingSurface(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 22.dp,
            elevation = 12.dp,
            backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            contentPadding = PaddingValues(12.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FloatingCircleButton(onClick = { month = month.minusMonths(1) }, modifier = Modifier.size(38.dp), elevation = 6.dp) {
                        Icon(Icons.Rounded.ChevronLeft, null, modifier = Modifier.align(Alignment.Center))
                    }
                    Text(
                        "${month.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${month.year}",
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    FloatingCircleButton(onClick = { month = month.plusMonths(1) }, modifier = Modifier.size(38.dp), elevation = 6.dp) {
                        Icon(Icons.Rounded.ChevronRight, null, modifier = Modifier.align(Alignment.Center))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { Text(it, Modifier.weight(1f), fontSize = 12.sp, color = Color(0xFF8E8E93), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
                }
                Spacer(Modifier.height(7.dp))
                val cells = leading + days.size
                val rows = (cells + 6) / 7
                repeat(rows) { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(7) { col ->
                            val index = row * 7 + col - leading
                            if (index !in days.indices) {
                                Spacer(Modifier.weight(1f).aspectRatio(0.82f))
                            } else {
                                val info = days[index]
                                val active = info.westernDate == selected
                                val hasNote = noteDates.containsKey(info.westernDate)
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .aspectRatio(0.82f)
                                        .clip(RoundedCornerShape(13.dp))
                                        .background(if (active) Color(0xFF007AFF) else Color(0xFFF2F2F7))
                                        .combinedClickable(onClick = { selected = info.westernDate })
                                        .padding(4.dp)
                                ) {
                                    Text(
                                        info.westernDate.dayOfMonth.toString(),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) Color.White else Color(0xFF171719),
                                        modifier = Modifier.align(Alignment.TopStart)
                                    )
                                    Text(
                                        buildString {
                                            append(info.moonPhase)
                                            if (info.fortnightDay.isNotBlank()) append(" ").append(info.fortnightDay)
                                        },
                                        fontSize = 8.sp,
                                        lineHeight = 9.sp,
                                        maxLines = 2,
                                        color = if (active) Color.White.copy(alpha = 0.9f) else Color(0xFF6E6E73),
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                    if (hasNote) Box(Modifier.align(Alignment.BottomCenter).size(4.dp).clip(CircleShape).background(if (active) Color.White else Color(0xFF007AFF)))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        FloatingSurface(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp,
            elevation = 13.dp,
            backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            contentPadding = PaddingValues(18.dp)
        ) {
            Column {
                Text(selected.toString(), fontSize = 14.sp, color = Color(0xFF8E8E93))
                Spacer(Modifier.height(5.dp))
                Text("မြန်မာနှစ် ${selectedInfo.myanmarYear} ခု", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(selectedInfo.compactLunar, fontSize = 18.sp)
                Text("${selectedInfo.weekDay}နေ့", fontSize = 15.sp, color = Color(0xFF6E6E73))
                if (selectedInfo.sabbath.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(selectedInfo.sabbath, color = Color(0xFF007AFF), fontWeight = FontWeight.Bold)
                }
                selectedInfo.holidays.forEach { holiday ->
                    Text(holiday, color = Color(0xFFD33A2C), fontWeight = FontWeight.SemiBold)
                }
                val count = noteDates[selected]?.size ?: 0
                if (count > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text("WoW Note: $count note${if (count == 1) "" else "s"}", fontSize = 14.sp, color = Color(0xFF6E6E73))
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.navigationBarsPadding().height(86.dp))
    }
}

private fun relativeTime(epochMillis: Long): String {
    val minutes = ((System.currentTimeMillis() - epochMillis).coerceAtLeast(0) / 60_000)
    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m"
        minutes < 1_440 -> "${minutes / 60}h"
        else -> "${minutes / 1_440}d"
    }
}
