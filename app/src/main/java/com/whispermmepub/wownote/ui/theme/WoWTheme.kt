package com.whispermmepub.wownote.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Sky = lightColorScheme(
    primary = Color(0xFF1677FF),
    secondary = Color(0xFF66A6FF),
    background = Color(0xFFF3F7FF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE8F1FF),
    onPrimary = Color.White,
    onBackground = Color(0xFF111827),
    onSurface = Color(0xFF111827)
)

private val Light = lightColorScheme(
    primary = Color(0xFF007AFF),
    secondary = Color(0xFF5856D6),
    background = Color(0xFFF2F2F7),
    surface = Color(0xFFFDFDFE),
    surfaceVariant = Color(0xFFE9E9EF),
    onPrimary = Color.White,
    onBackground = Color(0xFF111111),
    onSurface = Color(0xFF111111)
)

private val Cream = lightColorScheme(
    primary = Color(0xFFB56A2A),
    secondary = Color(0xFFD09A62),
    background = Color(0xFFFFF8EC),
    surface = Color(0xFFFFFDF8),
    surfaceVariant = Color(0xFFF5E7D2),
    onPrimary = Color.White,
    onBackground = Color(0xFF30271F),
    onSurface = Color(0xFF30271F)
)

private val Rose = lightColorScheme(
    primary = Color(0xFFD94E7A),
    secondary = Color(0xFFF08FAE),
    background = Color(0xFFFFF3F7),
    surface = Color(0xFFFFFBFC),
    surfaceVariant = Color(0xFFFFE3EC),
    onPrimary = Color.White,
    onBackground = Color(0xFF351B24),
    onSurface = Color(0xFF351B24)
)

private val Forest = lightColorScheme(
    primary = Color(0xFF327A5A),
    secondary = Color(0xFF68A989),
    background = Color(0xFFF1F7F3),
    surface = Color(0xFFFBFEFC),
    surfaceVariant = Color(0xFFDCECE3),
    onPrimary = Color.White,
    onBackground = Color(0xFF13271E),
    onSurface = Color(0xFF13271E)
)

private val Dark = darkColorScheme(
    primary = Color(0xFF6AAEFF),
    secondary = Color(0xFFA7C8FF),
    background = Color(0xFF111318),
    surface = Color(0xFF1B1E24),
    surfaceVariant = Color(0xFF292D35),
    onPrimary = Color(0xFF061B33),
    onBackground = Color(0xFFF4F5F7),
    onSurface = Color(0xFFF4F5F7)
)

private val Amoled = darkColorScheme(
    primary = Color(0xFF4DA3FF),
    secondary = Color(0xFF7AB8FF),
    background = Color.Black,
    surface = Color(0xFF080808),
    surfaceVariant = Color(0xFF171717),
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun WoWNoteTheme(
    theme: WoWThemeChoice = WoWThemeChoice.SKY,
    content: @Composable () -> Unit
) {
    val colors = when (theme) {
        WoWThemeChoice.SKY -> Sky
        WoWThemeChoice.LIGHT -> Light
        WoWThemeChoice.CREAM -> Cream
        WoWThemeChoice.ROSE -> Rose
        WoWThemeChoice.FOREST -> Forest
        WoWThemeChoice.DARK -> Dark
        WoWThemeChoice.AMOLED -> Amoled
    }

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
