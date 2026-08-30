from pathlib import Path

path = Path("app/src/main/java/com/whispermmepub/wownote/WoWNoteApp.kt")
text = path.read_text(encoding="utf-8")

if "import androidx.compose.foundation.lazy.grid.GridCells" not in text:
    text = text.replace(
        "import androidx.compose.foundation.lazy.items\n",
        "import androidx.compose.foundation.lazy.items\n"
        "import androidx.compose.foundation.lazy.grid.GridCells\n"
        "import androidx.compose.foundation.lazy.grid.LazyVerticalGrid\n"
        "import androidx.compose.foundation.lazy.grid.items as gridItems\n"
    )

text = text.replace(
    "private enum class NoteSection { NOTES, ARCHIVE, TRASH }\n",
    "private enum class NoteSection { NOTES, ARCHIVE, TRASH }\n"
    "private enum class HomeLayout { CARD, LIST, GRID }\n"
    "private enum class SocialStyle { CLEAN, PHOTO, FEED, MICRO }\n"
)

notes_start = text.index("@Composable\nprivate fun NotesScreen(")
notes_end = text.index("@Composable\nprivate fun SectionPicker(", notes_start)

new_notes = r'''@Composable
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

'''

text = text[:notes_start] + new_notes + text[notes_end:]

card_start = text.index("@Composable\nprivate fun NoteCard(")
card_end = text.index("@Composable\nprivate fun SmallCircle(", card_start)

new_card = r'''@Composable
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

'''

text = text[:card_start] + new_card + text[card_end:]

path.write_text(text, encoding="utf-8")
print("Home UI patch applied")
