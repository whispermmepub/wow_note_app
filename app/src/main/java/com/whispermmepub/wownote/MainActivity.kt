package com.whispermmepub.wownote

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.whispermmepub.wownote.ui.components.FloatingSurface
import com.whispermmepub.wownote.ui.theme.AppThemeController
import com.whispermmepub.wownote.ui.theme.WoWNoteTheme
import com.whispermmepub.wownote.ui.theme.WoWThemeChoice
import kotlinx.coroutines.delay
import mmcalendar.CalendarType
import mmcalendar.Config
import mmcalendar.Language

data class LaunchRequest(
    val openNoteId: String? = null,
    val quickNew: Boolean = false,
    val sharedText: String? = null
)

class MainActivity : ComponentActivity() {
    private var launchRequest by mutableStateOf(LaunchRequest())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppThemeController.initialize(this)

        runCatching {
            Config.initDefault(
                Config.Builder()
                    .setCalendarType(CalendarType.ENGLISH)
                    .setLanguage(Language.MYANMAR)
                    .build()
            )
        }

        consumeIntent(intent)
        setContent {
            WoWNoteTheme(theme = AppThemeController.theme) {
                var appReady by remember { mutableStateOf(false) }
                var showThemes by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    // Draw a branded first frame before heavier note/database UI is composed.
                    delay(650)
                    appReady = true
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    if (appReady) {
                        WoWNoteApp(
                            launchRequest = launchRequest,
                            onLaunchConsumed = { launchRequest = LaunchRequest() }
                        )

                        ThemeFloatingButton(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .statusBarsPadding()
                                .padding(top = 8.dp, end = 14.dp),
                            onClick = { showThemes = true }
                        )
                    } else {
                        StartupScreen()
                    }
                }

                if (showThemes) {
                    ThemePickerDialog(
                        selected = AppThemeController.theme,
                        onDismiss = { showThemes = false },
                        onSelect = { choice ->
                            AppThemeController.set(this@MainActivity, choice)
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeIntent(intent)
    }

    private fun consumeIntent(intent: Intent?) {
        if (intent == null) return
        val shared = if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() }
        } else null
        launchRequest = LaunchRequest(
            openNoteId = intent.getStringExtra(EXTRA_OPEN_NOTE_ID),
            quickNew = intent.getBooleanExtra(EXTRA_QUICK_NEW, false),
            sharedText = shared
        )
    }

    companion object {
        const val EXTRA_OPEN_NOTE_ID = "open_note_id"
        const val EXTRA_QUICK_NEW = "quick_new"
    }
}

@Composable
private fun StartupScreen() {
    val gradient = Brush.verticalGradient(
        listOf(
            Color(0xFF0D6EFD),
            Color(0xFF5A9CFF),
            Color(0xFFB9D7FF)
        )
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 90.dp, start = 30.dp)
                .size(130.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 100.dp, end = 22.dp)
                .size(190.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
        )

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FloatingSurface(
                modifier = Modifier.size(96.dp),
                cornerRadius = 30.dp,
                elevation = 24.dp,
                backgroundColor = Color.White.copy(alpha = 0.96f),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "W",
                        color = Color(0xFF0D6EFD),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "WoW Note",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "Write • Read • Remember",
                color = Color.White.copy(alpha = 0.86f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(25.dp),
                strokeWidth = 2.5.dp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun ThemeFloatingButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    FloatingSurface(
        modifier = modifier
            .size(42.dp)
            .clickable(onClick = onClick),
        cornerRadius = 21.dp,
        elevation = 10.dp,
        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("🎨", fontSize = 19.sp)
        }
    }
}

@Composable
private fun ThemePickerDialog(
    selected: WoWThemeChoice,
    onDismiss: () -> Unit,
    onSelect: (WoWThemeChoice) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        FloatingSurface(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 28.dp,
            elevation = 24.dp,
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentPadding = PaddingValues(18.dp)
        ) {
            Column {
                Text("App Theme", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(
                    "ရွေးထားတဲ့ theme ကို ဒီဖုန်းမှာ အလိုအလျောက်မှတ်ထားမယ်",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                )
                Spacer(Modifier.height(16.dp))

                WoWThemeChoice.entries.forEach { choice ->
                    val colors = themePreviewColors(choice)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (choice == selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            )
                            .clickable { onSelect(choice) }
                            .padding(horizontal = 13.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            colors.forEach { color ->
                                Box(
                                    Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                            }
                        }
                        Spacer(Modifier.size(11.dp))
                        Text(
                            choice.label,
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (choice == selected) {
                            Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                }

                Spacer(Modifier.height(5.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Done", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun themePreviewColors(choice: WoWThemeChoice): List<Color> = when (choice) {
    WoWThemeChoice.SKY -> listOf(Color(0xFF1677FF), Color(0xFFE8F1FF), Color.White)
    WoWThemeChoice.LIGHT -> listOf(Color(0xFF007AFF), Color(0xFFF2F2F7), Color.White)
    WoWThemeChoice.CREAM -> listOf(Color(0xFFB56A2A), Color(0xFFFFF1DA), Color(0xFFFFFDF8))
    WoWThemeChoice.ROSE -> listOf(Color(0xFFD94E7A), Color(0xFFFFE3EC), Color(0xFFFFFBFC))
    WoWThemeChoice.FOREST -> listOf(Color(0xFF327A5A), Color(0xFFDCECE3), Color(0xFFFBFEFC))
    WoWThemeChoice.DARK -> listOf(Color(0xFF6AAEFF), Color(0xFF292D35), Color(0xFF111318))
    WoWThemeChoice.AMOLED -> listOf(Color(0xFF4DA3FF), Color(0xFF171717), Color.Black)
}
