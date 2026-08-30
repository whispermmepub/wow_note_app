package com.whispermmepub.wownote.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class WoWThemeChoice(val label: String) {
    SKY("Sky Light"),
    LIGHT("Clean Light"),
    CREAM("Warm Cream"),
    ROSE("Soft Rose"),
    FOREST("Forest"),
    DARK("Midnight"),
    AMOLED("AMOLED Black")
}

object AppThemeController {
    private const val PREFS = "wow_note_ui"
    private const val KEY_THEME = "app_theme"

    var theme by mutableStateOf(WoWThemeChoice.SKY)
        private set

    fun initialize(context: Context) {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_THEME, WoWThemeChoice.SKY.name)
        theme = runCatching { WoWThemeChoice.valueOf(saved ?: WoWThemeChoice.SKY.name) }
            .getOrDefault(WoWThemeChoice.SKY)
    }

    fun set(context: Context, choice: WoWThemeChoice) {
        theme = choice
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, choice.name)
            .apply()
    }
}
