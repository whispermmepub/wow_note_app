package com.whispermmepub.wownote

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll as foundationVerticalScroll
import androidx.compose.ui.Modifier

/** Keeps scroll modifiers available to app screens without experimental APIs. */
fun Modifier.verticalScroll(state: ScrollState): Modifier = this.foundationVerticalScroll(state)
