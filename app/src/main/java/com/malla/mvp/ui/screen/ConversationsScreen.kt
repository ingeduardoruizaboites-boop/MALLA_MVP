package com.malla.mvp.ui.screen

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border
import com.malla.mvp.data.AppDatabase
import com.malla.mvp.identity.IdentityManager
import com.malla.mvp.data.dao.StoryDao
import com.malla.mvp.data.entity.ConversationEntity
import com.malla.mvp.data.entity.StoryEntity
import com.malla.mvp.ui.components.ConversationCard
import kotlinx.coroutines.launch
import java.util.UUID

private val sampleConversations = listOf(
    ConversationEntity(id = "sim_alicia", title = "Alicia", lastMessage = "¿Todo bien?", timestamp = System.currentTimeMillis() - 60000, lastMessageStatus = 2, unreadCount = 0),
    ConversationEntity(id = "sim_carlos", title = "Carlos", lastMessage = "Conéctate a la mesh", timestamp = System.currentTimeMillis() - 3600000, lastMessageStatus = 0, unreadCount = 1),
    ConversationEntity(id = "sim_oficina", title = "Oficina", lastMessage = "Recordatorio: reunión mesh", timestamp = System.currentTimeMillis() - 86400000, lastMessageStatus = 1, unreadCount = 0)
)

@Composable
fun ConversationsScreen(
    onChatClicked: (String, String) -> Unit,
    onProfileClicked: (String) -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val conversationDao = remember { db?.conversationDao() }
    var conversations by remember { mutableStateOf<List<ConversationEntity>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    var showStoryViewer by remember { mutableStateOf(false) }
    var currentStoryUri by remember { mutableStateOf("") }
    var customTabs by remember { mutableStateOf(listOf<String>()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(conversationDao) {
        if (conversationDao != null) {
            conversationDao.getAllVisibleConversations().collect { list ->
                conversations = list
            }
        } else {
            conversations = emptyList()
        }
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

    Box(modifier = Modifier.fillMaxSize()) {
    if (showStoryViewer) {
        StoryViewerScreen(imageUri = currentStoryUri, onFinished = { showStoryViewer = false })
    }
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).height(40.dp),
                placeholder = { Text("") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Buscar") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            Text("Historias", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            val storyDao = remember { db?.storyDao() }
            var stories by remember { mutableStateOf<List<StoryEntity>>(emptyList()) }
            LaunchedEffect(storyDao) {
                storyDao?.getAllStories()?.collect { stories = it }
            }
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            modifier = Modifier.size(60.dp).clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            onClick = {
                                // Añadir nueva historia (placeholder)
                                scope.launch {
                                    storyDao?.insertStory(StoryEntity(id = UUID.randomUUID().toString(), userId = "self", imageUri = "#FFFF00", timestamp = System.currentTimeMillis()))
                                }
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Add, "Nueva historia") }
                        }
                        Text("Nueva", style = MaterialTheme.typography.labelSmall)
                    }
                }
                items(stories) { story ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val haloColor = if (!story.seen) MaterialTheme.colorScheme.primary else Color.Transparent
                        Box(modifier = Modifier.size(64.dp).clip(CircleShape).border(2.dp, haloColor, CircleShape)) {
                            Surface(
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                onClick = {
                                    // Mostrar visor de historia
                                    showStoryViewer = true
                                    currentStoryUri = story.imageUri
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(story.userId.take(1).uppercase(), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        Text(story.userId.take(8), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Etiquetas estilo Telegram
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allTabs.size) { index ->
                    FilterChip(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = { Text(allTabs[index]) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                item {
                    IconButton(onClick = {
                        // Añadir etiqueta personalizada (placeholder)
                        val newLabel = "Nuevo ${customTabs.size + 1}"
                        customTabs = customTabs + newLabel
                        selectedTab = allTabs.size - 1
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Añadir etiqueta", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (tabFiltered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay conversaciones", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
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

        var showFabMenu by remember { mutableStateOf(false) }
        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            FloatingActionButton(
                onClick = { showFabMenu = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) { Icon(Icons.Filled.Add, "Nuevo") }
            DropdownMenu(expanded = showFabMenu, onDismissRequest = { showFabMenu = false }) {
                DropdownMenuItem(text = { Text("Agregar usuario") }, onClick = { showFabMenu = false })
                DropdownMenuItem(text = { Text("Nueva historia") }, onClick = { showFabMenu = false })
                DropdownMenuItem(text = { Text("Nuevo grupo") }, onClick = {
                    showFabMenu = false
                    // Crear grupo simulado
                    scope.launch {
                        val groupId = UUID.randomUUID().toString()
                        val group = ConversationEntity(
                            id = groupId,
                            title = "Grupo nuevo",
                            isGroup = true,
                            timestamp = System.currentTimeMillis()
                        )
                        conversationDao?.insertConversation(group)
                        onChatClicked(groupId, "Grupo nuevo")
                    }
                })
            }
        }
    }
}
