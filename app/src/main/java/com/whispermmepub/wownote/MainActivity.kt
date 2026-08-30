package com.whispermmepub.wownote

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.whispermmepub.wownote.ui.theme.WoWNoteTheme
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
        Config.initDefault(
            Config.Builder()
                .setCalendarType(CalendarType.ENGLISH)
                .setLanguage(Language.MYANMAR)
                .build()
        )
        consumeIntent(intent)
        setContent {
            WoWNoteTheme {
                WoWNoteApp(
                    launchRequest = launchRequest,
                    onLaunchConsumed = { launchRequest = LaunchRequest() }
                )
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
