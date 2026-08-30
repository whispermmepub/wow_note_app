package com.whispermmepub.wownote.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun FloatingSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    elevation: Dp = 14.dp,
    backgroundColor: Color = Color.White.copy(alpha = 0.96f),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .shadow(elevation = elevation, shape = shape, clip = false)
            .clip(shape)
            .background(backgroundColor)
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun FloatingPressable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shapeRadius: Dp = 20.dp,
    backgroundColor: Color = Color.White.copy(alpha = 0.96f),
    elevation: Dp = 12.dp,
    haptics: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val animatedElevation by animateDpAsState(if (pressed) 3.dp else elevation, label = "buttonElevation")
    val animatedScale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "buttonScale")
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(shapeRadius)

    Box(
        modifier = modifier
            .scale(animatedScale)
            .shadow(animatedElevation, shape, clip = false)
            .clip(shape)
            .background(backgroundColor)
            .clickable(interactionSource = interaction, indication = null) {
                if (haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        content = content
    )
}

@Composable
fun FloatingCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White.copy(alpha = 0.97f),
    elevation: Dp = 14.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val animatedElevation by animateDpAsState(if (pressed) 3.dp else elevation, label = "circleElevation")
    val animatedScale by animateFloatAsState(if (pressed) 0.94f else 1f, label = "circleScale")
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .scale(animatedScale)
            .shadow(animatedElevation, CircleShape, clip = false)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(interactionSource = interaction, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        content = content
    )
}
