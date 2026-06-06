package com.malla.mvp.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@Composable
fun PageTransition(
    targetState: Any,
    modifier: Modifier = Modifier,
    content: @Composable (Any) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            (scaleIn(tween(400), initialScale = 0.92f) + fadeIn(tween(400)))
                .togetherWith(scaleOut(tween(400), targetScale = 1.08f) + fadeOut(tween(400)))
        },
        label = "page_transition"
    ) { state ->
        content(state)
    }
}
