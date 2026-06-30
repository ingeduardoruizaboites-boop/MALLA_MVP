package com.malla.mvp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.malla.mvp.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val scale = remember { Animatable(0.85f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = tween(1000, easing = FastOutSlowInEasing))
        delay(1200)
        onFinished()
    }
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0A1118)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher),
            contentDescription = "Logo",
            modifier = Modifier.size(120.dp).scale(scale.value),
            contentScale = ContentScale.Fit
        )
    }
}
