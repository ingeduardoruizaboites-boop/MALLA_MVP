package com.malla.mvp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun StickerFullScreenDialog() {
    val stickerUrl by StickerState.fullScreenSticker.collectAsState()
    if (stickerUrl == null) return

    val scale = remember { Animatable(0f) }
    LaunchedEffect(stickerUrl) {
        scale.animateTo(1f, animationSpec = tween(300))
        delay(1500)
        StickerState.onSendSticker?.invoke(stickerUrl!!)
        StickerState.dismissFullScreen()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = stickerUrl,
            contentDescription = "Sticker",
            modifier = Modifier
                .size(300.dp)
                .scale(scale.value),
            contentScale = ContentScale.Fit
        )
    }
}
