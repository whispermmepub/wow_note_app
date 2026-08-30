package com.whispermmepub.wownote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF007AFF),
    background = Color(0xFFF2F2F7),
    surface = Color(0xFFFDFDFE),
    onPrimary = Color.White,
    onBackground = Color(0xFF111111),
    onSurface = Color(0xFF111111)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF0A84FF),
    background = Color(0xFF000000),
    surface = Color(0xFF1C1C1E),
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun WoWNoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
