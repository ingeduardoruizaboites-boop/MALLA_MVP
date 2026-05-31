package com.malla.mvp.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.malla.mvp.data.entity.ConversationEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun ConversationCard(
    conversation: ConversationEntity,
    onClick: () -> Unit,
    onArchive: () -> Unit = {},
    onDelete: () -> Unit = {},
    onHide: () -> Unit = {},
    onProfile: () -> Unit = {},
    onStories: () -> Unit = {},
    avatarBitmap: Bitmap? = null
) {
    var showAvatarDialog by remember { mutableStateOf(false) }

    val leftActionWidth = 120.dp
    val rightActionWidth = 160.dp
    val density = LocalDensity.current
    val leftActionWidthPx = with(density) { leftActionWidth.toPx() }
    val rightActionWidthPx = with(density) { rightActionWidth.toPx() }
    val dismissThreshold = 0.4f
    val scope = rememberCoroutineScope()

    val offsetX = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isPeeked by remember { mutableStateOf(false) }
    val peekDirection = remember { mutableStateOf(0) }

    fun closeActions() {
        scope.launch {
            isPeeked = false
            peekDirection.value = 0
            offsetX.animateTo(0f, animationSpec = spring(dampingRatio = 0.3f, stiffness = 300f))
        }
    }

    suspend fun snapToTarget(dragOffset: Float) {
        if (dragOffset > leftActionWidthPx * dismissThreshold) {
            isPeeked = true
            peekDirection.value = 1
            offsetX.animateTo(leftActionWidthPx, animationSpec = spring(dampingRatio = 0.3f, stiffness = 300f))
        } else if (dragOffset < -rightActionWidthPx * dismissThreshold) {
            isPeeked = true
            peekDirection.value = -1
            offsetX.animateTo(-rightActionWidthPx, animationSpec = spring(dampingRatio = 0.3f, stiffness = 300f))
        } else {
            closeActions()
        }
    }

    val leftActions = listOf(
        SwipeActionData(Icons.Filled.Person, "Perfil", Color(0xFF2196F3), onProfile),
        SwipeActionData(Icons.Filled.Collections, "Historias", Color(0xFF9C27B0), onStories)
    )
    val rightActions = listOf(
        SwipeActionData(Icons.Filled.VisibilityOff, "Ocultar", Color(0xFF607D8B), onHide),
        SwipeActionData(Icons.Filled.Delete, "Eliminar", Color(0xFFF44336), onDelete),
        SwipeActionData(Icons.Filled.Archive, "Archivar", Color(0xFF4CAF50), onArchive)
    )

    // Colores del avatar
    val defaultAvatarColor = remember(conversation.title) {
        val hue = (conversation.title.hashCode() % 360).toFloat()
        Color.hsl(hue = hue, saturation = 0.6f, lightness = 0.5f, alpha = 0.3f)
    }
    val textColor = remember(conversation.title) {
        val hue = (conversation.title.hashCode() % 360).toFloat()
        Color.hsl(hue = hue, saturation = 0.8f, lightness = 0.4f, alpha = 1f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        scope.launch { snapToTarget(offsetX.value) }
                    },
                    onDragCancel = {
                        isDragging = false
                        scope.launch {
                            offsetX.animateTo(0f)
                            isPeeked = false
                            peekDirection.value = 0
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        scope.launch {
                            val newValue = (offsetX.value + dragAmount)
                                .coerceIn(-rightActionWidthPx, leftActionWidthPx)
                            offsetX.snapTo(newValue)
                            isPeeked = false
                            peekDirection.value = if (newValue > 0) 1 else if (newValue < 0) -1 else 0
                        }
                    }
                )
            }
    ) {
        val alphaLeft = (offsetX.value / leftActionWidthPx).coerceIn(0f, 1f)
        val alphaRight = ((-offsetX.value) / rightActionWidthPx).coerceIn(0f, 1f)

        if (leftActions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .alpha(alphaLeft)
                    .padding(start = 4.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                leftActions.forEach { action ->
                    SwipeIconButton(action = action, scale = 1f, onTap = { action.onTrigger(); closeActions() })
                }
            }
        }

        if (rightActions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .alpha(alphaRight)
                    .padding(end = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                rightActions.forEach { action ->
                    SwipeIconButton(action = action, scale = 1f, onTap = { action.onTrigger(); closeActions() })
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(x = offsetX.value.roundToInt(), y = 0) }
        ) {
            Surface(
                onClick = {
                    if (isPeeked) closeActions() else onClick()
                },
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(52.dp)) {
                        Surface(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .clickable { showAvatarDialog = true },
                            color = if (avatarBitmap != null) Color.Transparent else defaultAvatarColor
                        ) {
                            if (avatarBitmap != null) {
                                Image(
                                    bitmap = avatarBitmap.asImageBitmap(),
                                    contentDescription = "Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(conversation.title.take(1).uppercase(), style = MaterialTheme.typography.titleLarge, color = textColor)
                                }
                            }
                        }
                        if (conversation.id == "sim_alicia") {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50))
                                    .align(Alignment.BottomEnd)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(conversation.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                            Text(formatTimestamp(conversation.timestamp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                if (conversation.lastMessageStatus >= 0 && conversation.lastMessage?.isNotBlank() == true) {
                                    val check = when (conversation.lastMessageStatus) { 0 -> "✓" 1 -> "✓✓" 2 -> "✓✓" else -> "" }
                                    val checkColor = if (conversation.lastMessageStatus == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    Text(check, style = MaterialTheme.typography.labelSmall, color = checkColor)
                                    Spacer(modifier = Modifier.width(2.dp))
                                }
                                Text(conversation.lastMessage ?: "Sin mensajes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (conversation.unreadCount > 0) {
                                Badge(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                                    Text(conversation.unreadCount.toString(), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAvatarDialog) {
        Dialog(onDismissRequest = { showAvatarDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(120.dp).clip(CircleShape),
                        color = if (avatarBitmap != null) Color.Transparent else defaultAvatarColor
                    ) {
                        if (avatarBitmap != null) {
                            Image(
                                bitmap = avatarBitmap.asImageBitmap(),
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(conversation.title.take(1).uppercase(), style = MaterialTheme.typography.headlineLarge, color = textColor)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(conversation.title, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(conversation.lastMessage ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { showAvatarDialog = false }) { Text("Cerrar") }
                        Button(onClick = {
                            showAvatarDialog = false
                            onProfile()
                        }) { Text("Ver perfil") }
                    }
                }
            }
        }
    }

    HorizontalDivider(
        modifier = Modifier.padding(start = 72.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    )
}

private data class SwipeActionData(
    val icon: ImageVector,
    val label: String,
    val color: Color,
    val onTrigger: () -> Unit
)

@Composable
private fun SwipeIconButton(action: SwipeActionData, scale: Float, onTap: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.size(60.dp)
    ) {
        IconButton(onClick = onTap, modifier = Modifier.size(40.dp)) {
            Icon(imageVector = action.icon, contentDescription = action.label, tint = action.color, modifier = Modifier.size(24.dp * scale))
        }
        Text(text = action.label, style = MaterialTheme.typography.labelSmall, color = action.color.copy(alpha = 0.8f), maxLines = 1)
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "Ahora"
        diff < 3600_000 -> "${diff / 60_000} min"
        diff < 86_400_000 -> "${diff / 3_600_000} h"
        else -> SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(timestamp))
    }
}
