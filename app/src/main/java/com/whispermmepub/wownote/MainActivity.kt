package com.whispermmepub.wownote

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whispermmepub.wownote.ui.theme.WoWNoteTheme
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

        // Calendar initialization must never be able to block the whole app UI.
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
            WoWNoteTheme {
                var appReady by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    // Let Android draw a real first frame instead of showing a white window
                    // while SQLite/Compose are preparing the home screen.
                    delay(80)
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
                    } else {
                        StartupScreen()
                    }
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
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(74.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF007AFF)),
            contentAlignment = Alignment.Center
        ) {
            Text("W", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black)
        }
        Text(
            "WoW Note",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.5.dp,
            color = Color(0xFF007AFF)
        )
    }
}
