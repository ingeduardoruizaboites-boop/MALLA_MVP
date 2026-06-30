package com.malla.mvp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.unit.dp
import com.malla.mvp.identity.IdentityManager

@Composable
fun TypingIndicator(contactName: String, contactId: String) {
    val typingAvatar by IdentityManager.avatarBitmap.collectAsState()
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (typingAvatar != null) {
            Image(
                bitmap = typingAvatar!!.asImageBitmap(),
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = contactName.take(1).uppercase(),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            repeat(3) { i ->
                val infiniteTransition = rememberInfiniteTransition(label = "typing_dot_$i")
                val dotAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(500, delayMillis = i * 200),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "typing_alpha_$i"
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .alpha(dotAlpha)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
                if (i < 2) Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}
