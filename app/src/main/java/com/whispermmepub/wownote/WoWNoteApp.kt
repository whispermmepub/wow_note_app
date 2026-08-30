package com.whispermmepub.wownote

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.os.Build
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.whispermmepub.wownote.ai.AiAssistantDialog
import com.whispermmepub.wownote.browser.BrowserScreen
import com.whispermmepub.wownote.calendar.MyanmarCalendarUtil
import com.whispermmepub.wownote.data.NoteStore
import com.whispermmepub.wownote.editor.RichEditText
import com.whispermmepub.wownote.io.DocxExporter
import com.whispermmepub.wownote.io.LocalAssetManager
import com.whispermmepub.wownote.model.Note
import com.whispermmepub.wownote.model.NoteBackground
import com.whispermmepub.wownote.model.NoteBackgroundType
import com.whispermmepub.wownote.model.NoteType
import com.whispermmepub.wownote.reminder.ReminderScheduler
import com.whispermmepub.wownote.ui.components.FloatingCircleButton
import com.whispermmepub.wownote.ui.components.FloatingPressable
import com.whispermmepub.wownote.ui.components.FloatingSurface
import com.whispermmepub.wownote.widget.WoWNoteWidget
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

enum class AppTab { NOTES, CALENDAR, BROWSER }
private enum class NoteSection { NOTES, ARCHIVE, TRASH }
private enum class HomeLayout { CARD, LIST, GRID }
private enum class SocialStyle { CLEAN, PHOTO, FEED, MICRO }

@Composable
fun WoWNoteApp(
    launchRequest: LaunchRequest,
    onLaunchConsumed: () -> Unit
) {
    val context = LocalContext.current
    val store = remember { NoteStore(context.applicationContext) }
    var notes by remember { mutableStateOf(store.all()) }
    var tab by rememberSaveable { mutableStateOf(AppTab.NOTES) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var showCreate by remember { mutableStateOf(false) }

    fun refresh() {
        notes = store.all()
    }

    fun save(note: Note) {
        store.upsert(note)
        ReminderScheduler.schedule(context, note)
        WoWNoteWidget.refresh(context)
        refresh()
    }

    fun create(type: NoteType = NoteType.TEXT, content: String = "", title: String = "", sourceUrl: String? = null): Note {
        val initial = if (type == NoteType.CHECKLIST && content.isBlank()) "[ ] " else content
        val note = Note(type = type, title = title, content = initial, sourceUrl = sourceUrl)
        save(note)
        editingId = note.id
        tab = AppTab.NOTES
        return note
    }

    LaunchedEffect(launchRequest) {
        when {
            launchRequest.openNoteId != null -> {
                if (store.get(launchRequest.openNoteId) != null) {
                    editingId = launchRequest.openNoteId
                    tab = AppTab.NOTES
                }
            }
            launchRequest.quickNew -> create()
            !launchRequest.sharedText.isNullOrBlank() -> {
                val shared = launchRequest.sharedText.trim()
                val url = shared.lineSequence().firstOrNull()?.takeIf {
                    it.startsWith("http://") || it.startsWith("https://")
                }
                create(
                    title = if (url != null) "Web Note" else "Shared Note",
                    content = shared,
                    sourceUrl = url
                )
            }
        }
        if (launchRequest != LaunchRequest()) onLaunchConsumed()
    }

    val editing = editingId?.let { store.get(it) }
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
            AppTab.NOTES -> NotesScreen(
                notes = notes,
                onOpen = { editingId = it.id },
                onNew = { showCreate = true },
                onSave = ::save,
                onDeleteForever = {
                    ReminderScheduler.cancel(context, it.id)
                    store.deleteForever(it.id)
                    WoWNoteWidget.refresh(context)
                    refresh()
                }
            )
            AppTab.CALENDAR -> CalendarScreen(
                notes = notes.filter { !it.deleted },
                onOpenNote = { editingId = it.id }
            )
            AppTab.BROWSER -> BrowserScreen { title, text, url ->
                create(
                    title = title.take(120),
                    content = text,
                    sourceUrl = url
                )
            }
        }

        AppBottomBar(
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        )
    }

    if (showCreate) {
        NewNoteDialog(
            onDismiss = { showCreate = false },
            onText = {
                showCreate = false
                create(NoteType.TEXT)
            },
            onChecklist = {
                showCreate = false
                create(NoteType.CHECKLIST)
            }
        )
    }
}

@Composable
private fun NotesScreen(
    notes: List<Note>,
    onOpen: (Note) -> Unit,
    onNew: () -> Unit,
    onSave: (Note) -> Unit,
    onDeleteForever: (Note) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("wow_home_ui", android.content.Context.MODE_PRIVATE) }
    var query by rememberSaveable { mutableStateOf("") }
    var section by rememberSaveable { mutableStateOf(NoteSection.NOTES) }
    var layoutKey by rememberSaveable {
        mutableStateOf(prefs.getString("layout", HomeLayout.CARD.name) ?: HomeLayout.CARD.name)
    }
    var styleKey by rememberSaveable {
        mutableStateOf(prefs.getString("social_style", SocialStyle.CLEAN.name) ?: SocialStyle.CLEAN.name)
    }
    val layout = runCatching { HomeLayout.valueOf(layoutKey) }.getOrDefault(HomeLayout.CARD)
    val socialStyle = runCatching { SocialStyle.valueOf(styleKey) }.getOrDefault(SocialStyle.CLEAN)

    fun selectLayout(value: HomeLayout) {
        layoutKey = value.name
        prefs.edit().putString("layout", value.name).apply()
    }

    fun selectStyle(value: SocialStyle) {
        styleKey = value.name
        prefs.edit().putString("social_style", value.name).apply()
    }

    val filtered = notes.filter {
        when (section) {
            NoteSection.NOTES -> !it.deleted && !it.archived
            NoteSection.ARCHIVE -> !it.deleted && it.archived
            NoteSection.TRASH -> it.deleted
        }
    }.filter {
        query.isBlank() || it.title.contains(query, true) || it.content.contains(query, true) || it.sourceUrl.orEmpty().contains(query, true)
    }

    fun pin(note: Note) = onSave(note.copy(pinned = !note.pinned, updatedAt = System.currentTimeMillis()))
    fun archive(note: Note) = onSave(note.copy(archived = !note.archived, deleted = false, updatedAt = System.currentTimeMillis()))
    fun delete(note: Note) {
        if (section == NoteSection.TRASH) onDeleteForever(note)
        else onSave(note.copy(deleted = true, archived = false, updatedAt = System.currentTimeMillis()))
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 18.dp)
        ) {
            Spacer(Modifier.height(9.dp))
            Text("WoW Note", fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(13.dp))
            FloatingSurface(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 19.dp,
                elevation = 11.dp,
                backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 11.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(9.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            Box {
                                if (query.isBlank()) Text("Search notes", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f), fontSize = 16.sp)
                                inner()
                            }
                        }
                    )
                }
            }
            Spacer(Modifier.height(11.dp))
            SectionPicker(section) { section = it }
            Spacer(Modifier.height(10.dp))
            HomeViewControls(
                layout = layout,
                socialStyle = socialStyle,
                onLayout = ::selectLayout,
                onStyle = ::selectStyle
            )
            Spacer(Modifier.height(12.dp))

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(bottom = 110.dp), contentAlignment = Alignment.Center) {
                    Text(
                        when (section) {
                            NoteSection.NOTES -> "No notes yet"
                            NoteSection.ARCHIVE -> "Archive is empty"
                            NoteSection.TRASH -> "Trash is empty"
                        },
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.52f),
                        fontSize = 17.sp
                    )
                }
            } else if (layout == HomeLayout.GRID) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                    verticalArrangement = Arrangement.spacedBy(11.dp),
                    contentPadding = PaddingValues(bottom = 130.dp)
                ) {
                    gridItems(filtered, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            layout = layout,
                            socialStyle = socialStyle,
                            onOpen = { onOpen(note) },
                            onPin = { pin(note) },
                            onArchive = { archive(note) },
                            onDelete = { delete(note) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(if (layout == HomeLayout.LIST) 9.dp else 13.dp),
                    contentPadding = PaddingValues(bottom = 130.dp)
                ) {
                    items(filtered, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            layout = layout,
                            socialStyle = socialStyle,
                            onOpen = { onOpen(note) },
                            onPin = { pin(note) },
                            onArchive = { archive(note) },
                            onDelete = { delete(note) }
                        )
                    }
                }
            }
        }

        FloatingCircleButton(
            onClick = onNew,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 87.dp)
                .size(58.dp),
            backgroundColor = MaterialTheme.colorScheme.primary,
            elevation = 18.dp
        ) {
            Icon(Icons.Rounded.Add, "New note", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.align(Alignment.Center).size(30.dp))
        }
    }
}

@Composable
private fun HomeViewControls(
    layout: HomeLayout,
    socialStyle: SocialStyle,
    onLayout: (HomeLayout) -> Unit,
    onStyle: (SocialStyle) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            HomeChoicePill("Card", layout == HomeLayout.CARD) { onLayout(HomeLayout.CARD) }
            HomeChoicePill("List", layout == HomeLayout.LIST) { onLayout(HomeLayout.LIST) }
            HomeChoicePill("Grid", layout == HomeLayout.GRID) { onLayout(HomeLayout.GRID) }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            HomeChoicePill("Clean", socialStyle == SocialStyle.CLEAN) { onStyle(SocialStyle.CLEAN) }
            HomeChoicePill("Photo Social", socialStyle == SocialStyle.PHOTO) { onStyle(SocialStyle.PHOTO) }
            HomeChoicePill("Feed", socialStyle == SocialStyle.FEED) { onStyle(SocialStyle.FEED) }
            HomeChoicePill("Micro", socialStyle == SocialStyle.MICRO) { onStyle(SocialStyle.MICRO) }
        }
    }
}

@Composable
private fun HomeChoicePill(label: String, active: Boolean, onClick: () -> Unit) {
    FloatingPressable(
        onClick = onClick,
        modifier = Modifier.height(37.dp).widthIn(min = 76.dp),
        shapeRadius = 18.dp,
        elevation = if (active) 6.dp else 2.dp,
        backgroundColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    ) {
        Text(
            label,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 12.dp),
            color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SectionPicker(selected: NoteSection, onSelect: (NoteSection) -> Unit) {
    FloatingSurface(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        elevation = 8.dp,
        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        contentPadding = PaddingValues(4.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            listOf(
                NoteSection.NOTES to "Notes",
                NoteSection.ARCHIVE to "Archive",
                NoteSection.TRASH to "Trash"
            ).forEach { (value, label) ->
                val active = value == selected
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onSelect(value) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: Note,
    layout: HomeLayout,
    socialStyle: SocialStyle,
    onOpen: () -> Unit,
    onPin: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    val cardHeight = when (layout) {
        HomeLayout.CARD -> if (socialStyle == SocialStyle.PHOTO) 202.dp else 154.dp
        HomeLayout.LIST -> 94.dp
        HomeLayout.GRID -> if (socialStyle == SocialStyle.PHOTO) 206.dp else 184.dp
    }
    val radius = when (layout) {
        HomeLayout.LIST -> 19.dp
        else -> 24.dp
    }
    val maxLines = when (layout) {
        HomeLayout.LIST -> 1
        HomeLayout.GRID -> 4
        HomeLayout.CARD -> if (socialStyle == SocialStyle.PHOTO) 5 else 3
    }
    val veil = when (socialStyle) {
        SocialStyle.PHOTO -> 0.25f
        SocialStyle.FEED -> 0.72f
        SocialStyle.MICRO -> 0.82f
        SocialStyle.CLEAN -> 0.48f
    }

    FloatingSurface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        cornerRadius = radius,
        elevation = if (layout == HomeLayout.LIST) 8.dp else 13.dp,
        backgroundColor = Color.Transparent
    ) {
        Box(Modifier.fillMaxWidth().height(cardHeight)) {
            NoteBackgroundLayer(note)
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface.copy(alpha = veil)))

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(if (layout == HomeLayout.LIST) 13.dp else 16.dp)
            ) {
                when (socialStyle) {
                    SocialStyle.FEED -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(if (layout == HomeLayout.GRID) 26.dp else 30.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("W", color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text("WoW Note", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(relativeTime(note.updatedAt), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                            }
                            if (note.pinned) Icon(Icons.Rounded.PushPin, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    SocialStyle.MICRO -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("@wow_note", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text(relativeTime(note.updatedAt), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        Spacer(Modifier.height(5.dp))
                    }
                    SocialStyle.PHOTO -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FloatingSurface(
                                cornerRadius = 12.dp,
                                elevation = 2.dp,
                                backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.90f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("PHOTO NOTE", color = MaterialTheme.colorScheme.onPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.weight(1f))
                            if (note.pinned) Icon(Icons.Rounded.PushPin, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    SocialStyle.CLEAN -> Unit
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        note.title.ifBlank { if (note.type == NoteType.CHECKLIST) "Checklist" else "Untitled" },
                        modifier = Modifier.weight(1f),
                        fontSize = when (layout) {
                            HomeLayout.LIST -> 16.sp
                            HomeLayout.GRID -> 16.sp
                            HomeLayout.CARD -> 19.sp
                        },
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (socialStyle == SocialStyle.CLEAN && note.pinned) {
                        Icon(Icons.Rounded.PushPin, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }

                if (layout != HomeLayout.LIST || note.content.isNotBlank()) {
                    Spacer(Modifier.height(if (layout == HomeLayout.LIST) 3.dp else 7.dp))
                    Text(
                        note.content.replace("[ ] ", "☐ ").replace("[x] ", "☑ "),
                        modifier = Modifier.weight(1f),
                        maxLines = maxLines,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                        fontSize = if (layout == HomeLayout.LIST) 13.sp else 14.sp,
                        lineHeight = if (layout == HomeLayout.LIST) 17.sp else 19.sp
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    if (layout != HomeLayout.GRID || socialStyle != SocialStyle.PHOTO) {
                        SmallCircle(Icons.Rounded.PushPin, onPin)
                        SmallCircle(Icons.Rounded.Archive, onArchive)
                        SmallCircle(Icons.Rounded.Delete, onDelete)
                    }
                    note.reminderAt?.let {
                        Icon(Icons.Rounded.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    if (socialStyle != SocialStyle.FEED && socialStyle != SocialStyle.MICRO) {
                        Text(relativeTime(note.updatedAt), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallCircle(icon: ImageVector, onClick: () -> Unit) {
    FloatingCircleButton(
        onClick = onClick,
        modifier = Modifier.size(30.dp),
        elevation = 5.dp,
        backgroundColor = Color.White.copy(alpha = 0.85f)
    ) {
        Icon(icon, null, tint = Color(0xFF55555A), modifier = Modifier.align(Alignment.Center).size(15.dp))
    }
}

@Composable
private fun NoteBackgroundLayer(note: Note) {
    val bg = note.background
    when (bg.type) {
        NoteBackgroundType.DEFAULT -> Box(Modifier.fillMaxSize().background(Color(note.colorArgb.toInt())))
        NoteBackgroundType.SOLID -> Box(
            Modifier.fillMaxSize().background(Color((bg.solidArgb ?: note.colorArgb).toInt()))
        )
        NoteBackgroundType.GRADIENT -> Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(
                    listOf(
                        Color((bg.gradientStartArgb ?: 0xFFFFF4D6).toInt()),
                        Color((bg.gradientEndArgb ?: 0xFFE8F3FF).toInt())
                    )
                )
            )
        )
        NoteBackgroundType.IMAGE -> {
            val path = bg.imageUri
            val bitmap = remember(path) { path?.let { BitmapFactory.decodeFile(it) } }
            Box(Modifier.fillMaxSize().background(Color(0xFFF2F2F7))) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(bg.imageBlurDp.coerceIn(0f, 30f).dp)
                            .alpha(bg.imageOpacity.coerceIn(0.08f, 1f)),
                        contentScale = ContentScale.Crop
                    )
                    Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.14f)))
                }
            }
        }
    }
}

@Composable
private fun EditorScreen(
    original: Note,
    onSave: (Note) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var draft by remember(original.id) { mutableStateOf(original) }
    var editor by remember(original.id) { mutableStateOf<RichEditText?>(null) }
    var showSize by remember { mutableStateOf(false) }
    var showBackground by remember { mutableStateOf(false) }
    var showAi by remember { mutableStateOf(false) }
    var aiSelection by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }

    fun saveChanged(next: Note) {
        val stamped = next.copy(updatedAt = System.currentTimeMillis())
        draft = stamped
        onSave(stamped)
    }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    val fontPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { LocalAssetManager.importFont(context, uri) }
                .onSuccess { asset ->
                    editor?.setNoteTypeface(asset.path)
                    saveChanged(draft.copy(customFontPath = asset.path, customFontName = asset.displayName))
                    status = "Font: ${asset.displayName}"
                }
                .onFailure { status = "Font file မမှန်ပါ" }
        }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching { LocalAssetManager.importBackgroundImage(context, uri) }
                .onSuccess { asset ->
                    saveChanged(
                        draft.copy(
                            background = NoteBackground(
                                type = NoteBackgroundType.IMAGE,
                                imageUri = asset.path,
                                imageOpacity = 0.55f,
                                imageBlurDp = 0f
                            )
                        )
                    )
                    status = "Background photo ထည့်ပြီးပြီ"
                }
                .onFailure { status = "Background image မဖတ်နိုင်ပါ" }
        }
    }

    val docxExport = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    ) { uri ->
        if (uri != null) {
            runCatching { DocxExporter.export(context, uri, draft) }
                .onSuccess { status = "DOCX export ပြီးပြီ" }
                .onFailure { status = "DOCX export မအောင်မြင်ပါ" }
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!text.isNullOrBlank()) {
            editor?.insertAtCursor(text)
            status = "မြန်မာအသံကို စာသားပြောင်းပြီးပြီ"
        }
    }

    fun launchVoice() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "my-MM")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "my-MM")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "မြန်မာလို ပြောပါ")
        }
        runCatching { voiceLauncher.launch(intent) }
            .onFailure { status = "ဖုန်းမှာ Speech Recognition service မတွေ့ပါ" }
    }

    fun pickReminder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val base = Calendar.getInstance().apply {
            draft.reminderAt?.let { timeInMillis = it }
        }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        val picked = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, day)
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        if (picked > System.currentTimeMillis()) {
                            saveChanged(draft.copy(reminderAt = picked))
                            status = "Reminder သတ်မှတ်ပြီးပြီ"
                        } else status = "အနာဂတ်ရက်/အချိန်ကို ရွေးပါ"
                    },
                    base.get(Calendar.HOUR_OF_DAY),
                    base.get(Calendar.MINUTE),
                    false
                ).show()
            },
            base.get(Calendar.YEAR),
            base.get(Calendar.MONTH),
            base.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Box(Modifier.fillMaxSize()) {
        NoteBackgroundLayer(draft)
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .padding(horizontal = 15.dp)
        ) {
            Spacer(Modifier.height(7.dp))
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
            }

            draft.reminderAt?.let { millis ->
                Spacer(Modifier.height(8.dp))
                FloatingSurface(
                    cornerRadius = 15.dp,
                    elevation = 6.dp,
                    backgroundColor = Color.White.copy(alpha = 0.84f),
                    contentPadding = PaddingValues(horizontal = 11.dp, vertical = 7.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.NotificationsActive, null, tint = Color(0xFF007AFF), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(formatReminder(millis), fontSize = 12.sp, color = Color(0xFF3A3A3C))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Clear",
                            color = Color(0xFFD33A2C),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {
                                saveChanged(draft.copy(reminderAt = null))
                                ReminderScheduler.cancel(context, draft.id)
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(11.dp))
            FloatingSurface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                cornerRadius = 26.dp,
                elevation = 13.dp,
                backgroundColor = Color.White.copy(alpha = if (draft.background.type == NoteBackgroundType.IMAGE) 0.82f else 0.93f),
                contentPadding = PaddingValues(18.dp)
            ) {
                AndroidView(
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
                )
            }
            Spacer(Modifier.height(10.dp))

            FloatingSurface(
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
            }
        }

        status?.let { message ->
            FloatingSurface(
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 64.dp),
                cornerRadius = 17.dp,
                elevation = 18.dp,
                backgroundColor = Color(0xEE1C1C1E),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Text(message, color = Color.White, fontSize = 12.sp, modifier = Modifier.clickable { status = null })
            }
        }
    }

    if (showSize) {
        SizeDialog(
            onDismiss = { showSize = false },
            onSelect = { size ->
                editor?.setSelectionSizeSp(size)
                saveChanged(draft.copy(defaultFontSizeSp = size.toFloat()))
                showSize = false
            }
        )
    }

    if (showBackground) {
        BackgroundDialog(
            current = draft.background,
            onDismiss = { showBackground = false },
            onApply = {
                saveChanged(draft.copy(background = it))
                showBackground = false
            },
            onPhoto = {
                showBackground = false
                photoPicker.launch("image/*")
            }
        )
    }

    if (showAi) {
        AiAssistantDialog(
            noteText = draft.content,
            selectedText = aiSelection,
            onDismiss = { showAi = false },
            onInsert = { editor?.insertAtCursor(it) },
            onReplace = { editor?.replaceSelectionOrAll(it) }
        )
    }
}

@Composable
private fun ToolButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    FloatingCircleButton(
        onClick = onClick,
        modifier = Modifier.size(43.dp),
        elevation = 6.dp,
        backgroundColor = Color(0xFFF2F2F7)
    ) {
        Icon(icon, label, tint = Color(0xFF2C2C2E), modifier = Modifier.align(Alignment.Center).size(20.dp))
    }
}

@Composable
private fun SizeDialog(onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        FloatingSurface(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 28.dp,
            elevation = 24.dp,
            contentPadding = PaddingValues(18.dp)
        ) {
            Column {
                Text("Font Size", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                listOf(12, 14, 16, 18, 20, 24, 30, 36, 48, 60).chunked(5).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        row.forEach { size ->
                            FloatingPressable(
                                onClick = { onSelect(size) },
                                modifier = Modifier.weight(1f).height(46.dp),
                                shapeRadius = 15.dp,
                                elevation = 5.dp,
                                backgroundColor = Color(0xFFF2F2F7)
                            ) {
                                Text("$size", modifier = Modifier.align(Alignment.Center), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                DialogDone(onDismiss)
            }
        }
    }
}

@Composable
private fun BackgroundDialog(
    current: NoteBackground,
    onDismiss: () -> Unit,
    onApply: (NoteBackground) -> Unit,
    onPhoto: () -> Unit
) {
    var solid by remember(current) { mutableStateOf(argbToHex(current.solidArgb ?: 0xFFFFF4D6)) }
    var start by remember(current) { mutableStateOf(argbToHex(current.gradientStartArgb ?: 0xFFFFE8D6)) }
    var end by remember(current) { mutableStateOf(argbToHex(current.gradientEndArgb ?: 0xFFDDEEFF)) }
    var opacity by remember(current) { mutableStateOf(current.imageOpacity.coerceIn(0.08f, 1f)) }
    var blur by remember(current) { mutableStateOf(current.imageBlurDp.coerceIn(0f, 30f)) }

    Dialog(onDismissRequest = onDismiss) {
        FloatingSurface(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 28.dp,
            elevation = 24.dp,
            contentPadding = PaddingValues(18.dp)
        ) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Note Background", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("ဒီ note တစ်ခုအတွက်ပဲ သီးသန့်သိမ်းမယ်", color = Color(0xFF8E8E93), fontSize = 12.sp)
                Spacer(Modifier.height(14.dp))

                DialogAction("Default", Icons.Rounded.Restore) { onApply(NoteBackground()) }
                Spacer(Modifier.height(8.dp))
                Text("Solid color (HEX)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                HexField(solid) { solid = it }
                Spacer(Modifier.height(7.dp))
                DialogAction("Apply Solid", Icons.Rounded.FormatColorFill) {
                    parseArgb(solid)?.let { onApply(NoteBackground(type = NoteBackgroundType.SOLID, solidArgb = it)) }
                }
                Spacer(Modifier.height(12.dp))

                Text("Gradient start / end", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { HexField(start) { start = it } }
                    Box(Modifier.weight(1f)) { HexField(end) { end = it } }
                }
                Spacer(Modifier.height(7.dp))
                DialogAction("Apply Gradient", Icons.Rounded.Gradient) {
                    val a = parseArgb(start)
                    val b = parseArgb(end)
                    if (a != null && b != null) onApply(
                        NoteBackground(
                            type = NoteBackgroundType.GRADIENT,
                            gradientStartArgb = a,
                            gradientEndArgb = b
                        )
                    )
                }
                Spacer(Modifier.height(12.dp))
                DialogAction("Choose Photo", Icons.Rounded.Image) { onPhoto() }

                if (current.type == NoteBackgroundType.IMAGE && !current.imageUri.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text("Photo opacity ${String.format("%.0f", opacity * 100)}%", fontSize = 12.sp)
                    Slider(value = opacity, onValueChange = { opacity = it }, valueRange = 0.08f..1f)
                    Text("Blur ${String.format("%.0f", blur)}", fontSize = 12.sp)
                    Slider(value = blur, onValueChange = { blur = it }, valueRange = 0f..30f)
                    DialogAction("Apply Photo Settings", Icons.Rounded.Tune) {
                        onApply(current.copy(imageOpacity = opacity, imageBlurDp = blur))
                    }
                }
                Spacer(Modifier.height(10.dp))
                DialogDone(onDismiss)
            }
        }
    }
}

@Composable
private fun HexField(value: String, onChange: (String) -> Unit) {
    FloatingSurface(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 15.dp,
        elevation = 4.dp,
        backgroundColor = Color(0xFFF2F2F7),
        contentPadding = PaddingValues(horizontal = 11.dp, vertical = 10.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = { onChange(it.take(9)) },
            singleLine = true,
            textStyle = TextStyle(fontSize = 14.sp, color = Color(0xFF171719)),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DialogAction(label: String, icon: ImageVector, onClick: () -> Unit) {
    FloatingPressable(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(47.dp),
        shapeRadius = 16.dp,
        elevation = 6.dp,
        backgroundColor = Color(0xFFF2F2F7)
    ) {
        Row(Modifier.align(Alignment.CenterStart).padding(horizontal = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(19.dp), tint = Color(0xFF007AFF))
            Spacer(Modifier.width(9.dp))
            Text(label, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DialogDone(onClick: () -> Unit) {
    FloatingPressable(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(44.dp),
        shapeRadius = 16.dp,
        elevation = 5.dp,
        backgroundColor = Color(0xFF007AFF)
    ) {
        Text("Done", modifier = Modifier.align(Alignment.Center), color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CalendarScreen(notes: List<Note>, onOpenNote: (Note) -> Unit) {
    var month by rememberSaveable { mutableStateOf(YearMonth.now()) }
    val first = month.atDay(1)
    val offset = first.dayOfWeek.value - 1
    val dayCells = buildList<LocalDate?> {
        repeat(offset) { add(null) }
        for (day in 1..month.lengthOfMonth()) add(month.atDay(day))
        while (size % 7 != 0) add(null)
    }
    val today = LocalDate.now()

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 13.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("မြန်မာ ပြက္ခဒိန်", fontSize = 31.sp, fontWeight = FontWeight.Bold)
                Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), color = Color(0xFF8E8E93), fontSize = 13.sp)
            }
            FloatingCircleButton(onClick = { month = month.minusMonths(1) }, modifier = Modifier.size(40.dp), elevation = 8.dp) {
                Icon(Icons.Rounded.ChevronLeft, "Previous", modifier = Modifier.align(Alignment.Center))
            }
            Spacer(Modifier.width(7.dp))
            FloatingCircleButton(onClick = { month = month.plusMonths(1) }, modifier = Modifier.size(40.dp), elevation = 8.dp) {
                Icon(Icons.Rounded.ChevronRight, "Next", modifier = Modifier.align(Alignment.Center))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            listOf("တနင်္လာ", "အင်္ဂါ", "ဗုဒ္ဓ", "ကြာသပတေး", "သောကြာ", "စနေ", "တနင်္ဂနွေ").forEach {
                Text(it.take(2), modifier = Modifier.weight(1f), fontSize = 10.sp, color = Color(0xFF8E8E93), fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(5.dp))
        dayCells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { date ->
                    if (date == null) Spacer(Modifier.weight(1f).height(82.dp))
                    else {
                        val info = remember(date) { MyanmarCalendarUtil.info(date) }
                        val dayNotes = notes.filter { note ->
                            note.reminderAt?.let { millis ->
                                Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate() == date
                            } == true
                        }
                        val isToday = date == today
                        FloatingSurface(
                            modifier = Modifier
                                .weight(1f)
                                .height(82.dp)
                                .clickable(enabled = dayNotes.isNotEmpty()) { onOpenNote(dayNotes.first()) },
                            cornerRadius = 13.dp,
                            elevation = if (isToday) 8.dp else 3.dp,
                            backgroundColor = if (isToday) Color(0xFFE6F2FF) else Color.White.copy(alpha = 0.94f),
                            contentPadding = PaddingValues(5.dp)
                        ) {
                            Column(Modifier.fillMaxSize()) {
                                Text(
                                    date.dayOfMonth.toString(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isToday) Color(0xFF007AFF) else Color(0xFF171719)
                                )
                                Text(info.monthName, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color(0xFF636366))
                                Text(
                                    buildString {
                                        append(info.moonPhase)
                                        if (info.fortnightDay.isNotBlank()) append(" ${info.fortnightDay}")
                                    },
                                    fontSize = 7.sp,
                                    maxLines = 2,
                                    color = Color(0xFF636366)
                                )
                                Spacer(Modifier.weight(1f))
                                if (info.sabbath.isNotBlank()) Text(info.sabbath, fontSize = 7.sp, color = Color(0xFFD15A00), maxLines = 1)
                                if (dayNotes.isNotEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.NotificationsActive, null, tint = Color(0xFF007AFF), modifier = Modifier.size(9.dp))
                                        Text("${dayNotes.size}", fontSize = 7.sp, color = Color(0xFF007AFF))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        val todayInfo = remember(today) { MyanmarCalendarUtil.info(today) }
        Spacer(Modifier.height(6.dp))
        FloatingSurface(
            modifier = Modifier.fillMaxWidth().padding(bottom = 92.dp),
            cornerRadius = 22.dp,
            elevation = 10.dp,
            backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            contentPadding = PaddingValues(14.dp)
        ) {
            Column {
                Text("ယနေ့", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    "မြန်မာနှစ် ${todayInfo.myanmarYear} • ${todayInfo.compactLunar} • ${todayInfo.weekDay}",
                    fontSize = 13.sp,
                    color = Color(0xFF3A3A3C)
                )
                if (todayInfo.sabbath.isNotBlank()) Text(todayInfo.sabbath, color = Color(0xFFD15A00), fontSize = 12.sp)
                todayInfo.holidays.takeIf { it.isNotEmpty() }?.let { Text(it.joinToString(" • "), color = Color(0xFF007AFF), fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun AppBottomBar(selected: AppTab, onSelect: (AppTab) -> Unit, modifier: Modifier = Modifier) {
    FloatingSurface(
        modifier = modifier.width(302.dp),
        cornerRadius = 31.dp,
        elevation = 20.dp,
        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        contentPadding = PaddingValues(5.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            BottomItem(Icons.Rounded.Notes, "Notes", selected == AppTab.NOTES, Modifier.weight(1f)) { onSelect(AppTab.NOTES) }
            BottomItem(Icons.Rounded.CalendarMonth, "Calendar", selected == AppTab.CALENDAR, Modifier.weight(1f)) { onSelect(AppTab.CALENDAR) }
            BottomItem(Icons.Rounded.Language, "Browser", selected == AppTab.BROWSER, Modifier.weight(1f)) { onSelect(AppTab.BROWSER) }
        }
    }
}

@Composable
private fun BottomItem(icon: ImageVector, label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    FloatingPressable(
        onClick = onClick,
        modifier = modifier.height(49.dp),
        shapeRadius = 24.dp,
        elevation = if (active) 7.dp else 0.dp,
        backgroundColor = if (active) MaterialTheme.colorScheme.primary else Color.Transparent
    ) {
        Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, label, modifier = Modifier.size(18.dp), tint = if (active) Color.White else Color(0xFF636366))
            Spacer(Modifier.width(5.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (active) Color.White else Color(0xFF636366))
        }
    }
}

@Composable
private fun NewNoteDialog(onDismiss: () -> Unit, onText: () -> Unit, onChecklist: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        FloatingSurface(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 28.dp,
            elevation = 24.dp,
            contentPadding = PaddingValues(18.dp)
        ) {
            Column {
                Text("New Note", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                DialogAction("Text Note", Icons.Rounded.Notes, onText)
                Spacer(Modifier.height(9.dp))
                DialogAction("Checklist", Icons.Rounded.CheckBox, onChecklist)
                Spacer(Modifier.height(10.dp))
                DialogDone(onDismiss)
            }
        }
    }
}

private fun relativeTime(time: Long): String {
    val minutes = ((System.currentTimeMillis() - time).coerceAtLeast(0) / 60_000L)
    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m"
        minutes < 1_440 -> "${minutes / 60}h"
        else -> "${minutes / 1_440}d"
    }
}

private fun formatReminder(millis: Long): String = Instant.ofEpochMilli(millis)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("dd MMM yyyy • h:mm a"))

private fun argbToHex(value: Long): String = String.format("#%08X", value.toInt())

private fun parseArgb(raw: String): Long? = runCatching {
    val normalized = raw.trim().let {
        when {
            it.startsWith("#") -> it
            else -> "#$it"
        }
    }
    AndroidColor.parseColor(normalized).toLong() and 0xFFFFFFFFL
}.getOrNull()
