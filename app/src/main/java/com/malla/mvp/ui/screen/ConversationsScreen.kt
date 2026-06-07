package com.malla.mvp.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Bitmap
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import com.malla.mvp.data.AppDatabase
import com.malla.mvp.identity.IdentityManager
import com.malla.mvp.data.entity.ConversationEntity
import com.malla.mvp.data.entity.StoryEntity
import com.malla.mvp.ui.components.ConversationCard
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun ConversationsScreen(
    onChatClicked: (String, String) -> Unit,
    onProfileClicked: (String) -> Unit, onNavigateToQrScanner: () -> Unit = {}
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val conversationDao = remember { db?.conversationDao() }
    val storyDao = remember { db?.storyDao() }
    var conversations by remember { mutableStateOf<List<ConversationEntity>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    var showStoryViewer by remember { mutableStateOf(false) }
    var currentStoryUri by remember { mutableStateOf("") }
    var customTabs by remember { mutableStateOf(listOf<String>()) }
    var showFabMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(conversationDao) {
        conversationDao?.getAllVisibleConversations()?.collect { list ->
            conversations = list
        } ?: run { conversations = emptyList() }
    }

    var stories by remember { mutableStateOf<List<StoryEntity>>(emptyList()) }
    LaunchedEffect(storyDao) {
        storyDao?.getAllStories()?.collect { stories = it }
    }

    val allTabs = listOf("Todos", "No leídos", "Favoritos") + customTabs

    val filtered = remember(conversations, searchQuery) {
        if (searchQuery.isBlank()) conversations
        else conversations.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    val tabFiltered = remember(filtered, selectedTab) {
        when (selectedTab) {
            0 -> filtered
            1 -> filtered.filter { it.unreadCount > 0 }
            2 -> filtered.filter { it.id.startsWith("sim") }
            3 -> filtered.filter { it.isGroup }
            else -> {
                val customLabel = allTabs.getOrElse(selectedTab) { "" }
                filtered.filter { it.title.contains(customLabel, ignoreCase = true) }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(
        Brush.verticalGradient(listOf(Color(0xFF0A1B2A), Color(0xFF0A1118)))
    )) {
        if (showStoryViewer) {
            StoryViewerScreen(imageUri = currentStoryUri, onFinished = { showStoryViewer = false })
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Barra de búsqueda mejorada
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Search, "Buscar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    androidx.compose.foundation.text.BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text("Buscar conversaciones...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Filled.Clear, "Limpiar", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            // Historias
            Text(
                "Historias",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            modifier = Modifier.size(60.dp).clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            onClick = {
                                scope.launch {
                                    storyDao?.insertStory(StoryEntity(id = UUID.randomUUID().toString(), userId = "self", imageUri = "#FFFF00", timestamp = System.currentTimeMillis()))
                                }
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Add, "Nueva historia", tint = MaterialTheme.colorScheme.primary) }
                        }
                        Text("Nueva", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                items(stories) { story ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val haloColor = if (!story.seen) MaterialTheme.colorScheme.primary else Color.Transparent
                        Box(modifier = Modifier.size(64.dp).clip(CircleShape).border(2.dp, haloColor, CircleShape)) {
                            Surface(
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                onClick = { showStoryViewer = true; currentStoryUri = story.imageUri }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(story.userId.take(1).uppercase(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                        Text(story.userId.take(8), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Etiquetas
            LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(allTabs.size) { index ->
                    FilterChip(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = { Text(allTabs[index]) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                item {
                    IconButton(onClick = {
                        val newLabel = "Nuevo ${customTabs.size + 1}"
                        customTabs = customTabs + newLabel
                        selectedTab = allTabs.size - 1
                    }) {
                        Icon(Icons.Filled.Add, "Añadir etiqueta", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (tabFiltered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.ChatBubbleOutline, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No hay conversaciones", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
                    items(tabFiltered, key = { it.id }) { conversation ->
                        val avatarBitmap: Bitmap? = if (conversation.id == "sim_alicia") IdentityManager.loadAvatar(context) else null
                        ConversationCard(
                            conversation = conversation,
                            onClick = { onChatClicked(conversation.id, conversation.title) },
                            avatarBitmap = avatarBitmap,
                            onProfile = { onProfileClicked(conversation.title) },
                            onStories = { Toast.makeText(context, "Historias de ${conversation.title}", Toast.LENGTH_SHORT).show() },
                            onHide = { scope.launch { conversationDao?.hideConversation(conversation.id) } },
                            onDelete = { scope.launch { conversationDao?.deleteConversation(conversation) } },
                            onArchive = { scope.launch { conversationDao?.hideConversation(conversation.id) } }
                        )
                    }
                }
            }
        }

        // FAB
        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            FloatingActionButton(
                onClick = { showFabMenu = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) { Icon(Icons.Filled.Add, "Nuevo") }
            DropdownMenu(expanded = showFabMenu, onDismissRequest = { showFabMenu = false }) {
                DropdownMenuItem(text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.PersonAdd, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Agregar usuario") } }, onClick = { showFabMenu = false; onNavigateToQrScanner() })
                DropdownMenuItem(text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.AddCircle, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Nueva historia") } }, onClick = { showFabMenu = false; scope.launch { storyDao?.insertStory(StoryEntity(id = UUID.randomUUID().toString(), userId = "self", imageUri = "#FF00FF", timestamp = System.currentTimeMillis())) } })
                DropdownMenuItem(text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Group, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Nuevo grupo") } }, onClick = {
                    showFabMenu = false
                    scope.launch {
                        val groupId = UUID.randomUUID().toString()
                        val group = ConversationEntity(id = groupId, title = "Grupo nuevo", isGroup = true, timestamp = System.currentTimeMillis())
                        conversationDao?.insertConversation(group)
                        onChatClicked(groupId, "Grupo nuevo")
                    }
                })
            }
        }
    }
}
