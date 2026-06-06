package com.malla.mvp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malla.mvp.data.AppDatabase
import com.malla.mvp.identity.IdentityManager
import com.malla.mvp.data.entity.ConversationEntity
import com.malla.mvp.network.ConnectivityMonitor
import com.malla.mvp.network.MeshMessageHandler
import com.malla.mvp.core.engine.DeviceStateMonitor
import com.malla.mvp.network.NetworkService
import com.malla.mvp.service.MeshChatService
import com.malla.mvp.ui.components.MainTopBar
import com.malla.mvp.ui.components.ConnectivityStatusBar
import com.malla.mvp.ui.components.TutorialOverlay
import com.malla.mvp.ui.screen.*
import com.malla.mvp.ui.settings.AccessibilitySettings
import com.malla.mvp.ui.theme.MallaColorScheme
import com.malla.mvp.ui.theme.MallaTheme
import com.malla.mvp.viewmodel.AppThemeState
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppState { Splash, Onboarding, Main }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ConnectivityMonitor.start(application)
        DeviceStateMonitor.start(this)
        IdentityManager.init(this)
        insertSampleStories()

        // Inicializar el estado del tema
        val appThemeState = AppThemeState.create(this)

        val prefs = try {
            getSharedPreferences("malla_prefs", Context.MODE_PRIVATE)
        } catch (e: Exception) { null }
        val isFirstLaunch = try {
            prefs?.getBoolean("first_launch", true) ?: true
        } catch (e: Exception) { true }

        // Obtener instancia de la base de datos
        val database = AppDatabase.getInstance(application)

        setContent {
            var appState by remember { mutableStateOf(AppState.Splash) }
            var showQrScanner by remember { mutableStateOf(false) }
            var currentConversationId by remember { mutableStateOf<String?>(null) }
            var selectedContact by remember { mutableStateOf<String?>(null) }
            var showSettings by remember { mutableStateOf(false) }
            var showTutorial by remember { mutableStateOf(false) } 
            val context = LocalContext.current

            // Tema efectivo (cambia automáticamente a OLED en modo mesh)
            val effectiveScheme by appThemeState.currentTheme.collectAsState()

            val isOnline by ConnectivityMonitor.isOnline.collectAsState()
            LaunchedEffect(isOnline) {
                try {
                    if (!isOnline) {
                        context.startService(Intent(context, MeshChatService::class.java))
                        NetworkService.startServer()
                        MeshMessageHandler.start(application)
                    } else {
                        context.stopService(Intent(context, MeshChatService::class.java))
                        NetworkService.stopServer()
                        MeshMessageHandler.stop()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MallaMesh", "Error gestionando mesh (ignorado)", e)
                }
            }

            LaunchedEffect(appState) {
                if (appState == AppState.Main && !isFirstLaunch) {
                    val tutorialPrefs = try {
                        getSharedPreferences("tutorial", Context.MODE_PRIVATE)
                    } catch (e: Exception) { null }
                    val tutorialShown = try {
                        tutorialPrefs?.getBoolean("shown", false) ?: false
                    } catch (e: Exception) { false }
                    if (!tutorialShown) {
                        showTutorial = true
                    }
                }
            }

            MallaTheme(colorScheme = effectiveScheme, fontScale = AccessibilitySettings.fontScale.floatValue) {
                when (appState) {
                    AppState.Splash -> SplashContent {
                        if (isFirstLaunch) {
                            try { prefs?.edit()?.putBoolean("first_launch", false)?.apply() } catch (_: Exception) {}
                            appState = AppState.Onboarding
                        } else appState = AppState.Main
                    }
                    AppState.Onboarding -> OnboardingScreen {
                        appState = AppState.Main
                    }
                    AppState.Main -> {
                        if (showTutorial) {
                            TutorialOverlay(
                                onDismiss = {
                                    showTutorial = false
                                    try {
                                        getSharedPreferences("tutorial", Context.MODE_PRIVATE)
                                            ?.edit()?.putBoolean("shown", true)?.apply()
                                    } catch (_: Exception) {}
                                }
                            )
                        } else if (showQrScanner) {
                            QrScanScreen(
                                onQrScanned = { ip ->
                                    showQrScanner = false
                                    connectToPeerAndCreateConversation(ip) { convId -> currentConversationId = convId }
                                },
                                onBack = { showQrScanner = false }
                            )
                        } else if (showSettings) {
                            SettingsScreenWrapper(
                                currentScheme = effectiveScheme,
                                onSchemeSelected = { scheme -> appThemeState.selectScheme(scheme) },
                                onBack = { showSettings = false }
                            )
                        } else if (selectedContact != null) {
                            ContactProfileScreen(contactName = selectedContact!!, onBack = { selectedContact = null })
                        } else {
                            MainApp(
                                isMeshMode = !isOnline,
                                currentConversationId = currentConversationId,
                                onConversationChanged = { convId -> currentConversationId = convId },
                                onSettingsClick = { showSettings = true },
                                onProfileClicked = { contactName -> selectedContact = contactName },
                                onNavigateToQrScanner = { showQrScanner = true },
                                onConnectToPeer = { ip ->
                                    connectToPeerAndCreateConversation(ip) { convId -> currentConversationId = convId }
                                },
                                db = database
                            )
                        }
                    }
                }
            }
        }
    }

    private fun insertSampleStories() {
        MainScope().launch {
            val db = AppDatabase.getInstance(application) ?: return@launch
            val storyDao = db.storyDao()
            storyDao.insertStory(com.malla.mvp.data.entity.StoryEntity(id = "story1", userId = "sim_alicia", imageUri = "#FF5733", timestamp = System.currentTimeMillis() - 3600000))
            storyDao.insertStory(com.malla.mvp.data.entity.StoryEntity(id = "story2", userId = "sim_carlos", imageUri = "#33FF57", timestamp = System.currentTimeMillis() - 7200000))
        }
    }

    private fun connectToPeerAndCreateConversation(ip: String, onCreated: (String) -> Unit) {
        try {
            NetworkService.connectToPeer(ip)
        } catch (_: Exception) {}
        val db = AppDatabase.getInstance(application)
        val conversationId = UUID.randomUUID().toString()
        val conv = ConversationEntity(id = conversationId, title = "Peer ${ip.take(8)}", timestamp = System.currentTimeMillis())
        MainScope().launch {
            try {
                db?.conversationDao()?.insertConversation(conv)
                Toast.makeText(this@MainActivity, "Conectado a $ip", Toast.LENGTH_SHORT).show()
                onCreated(conversationId)
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error al crear conversación", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenWrapper(
    currentScheme: MallaColorScheme,
    onSchemeSelected: (MallaColorScheme) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, titleContentColor = MaterialTheme.colorScheme.onSurface)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            SettingsScreen(
                currentScheme = currentScheme,
                onSchemeSelected = onSchemeSelected
            )
        }
    }
}

@Composable
fun SplashContent(onFinished: () -> Unit) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = tween(800, easing = FastOutSlowInEasing))
        alpha.animateTo(1f, animationSpec = tween(600))
        delay(400)
        subtitleAlpha.animateTo(1f, animationSpec = tween(500))
        delay(1000)
        onFinished()
    }
    Box(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("M A L L A", style = MaterialTheme.typography.headlineLarge.copy(letterSpacing = 10.sp, fontSize = 36.sp), color = MaterialTheme.colorScheme.primary, modifier = Modifier.scale(scale.value).alpha(alpha.value))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Conéctate sin límites", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = subtitleAlpha.value), modifier = Modifier.alpha(subtitleAlpha.value))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    isMeshMode: Boolean,
    currentConversationId: String?,
    onConversationChanged: (String?) -> Unit,
    onSettingsClick: () -> Unit,
    onProfileClicked: (String) -> Unit,
    onNavigateToQrScanner: () -> Unit,
    onConnectToPeer: (String) -> Unit,
    db: AppDatabase?
) {
    var selectedTab by remember { mutableStateOf(0) }
    var currentContactName by remember { mutableStateOf("Chat") }

    if (currentConversationId != null) {
        ChatScreen(
            conversationId = currentConversationId,
            contactName = currentContactName,
            onBack = { onConversationChanged(null) },
            isMeshMode = isMeshMode
        )
        return
    }
    Scaffold(
        topBar = { MainTopBar(onSettingsClick = onSettingsClick, isOnline = !isMeshMode) },
        bottomBar = {
            NavigationBar(modifier = Modifier.height(56.dp)) {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.AutoMirrored.Filled.Chat, "Chats") }, label = { Text("Chats") })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Filled.WifiTethering, "Pulso") }, label = { Text("Pulso") })
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Filled.Person, "Perfil") }, label = { Text("Perfil") })
            }
        }
    ) { padding ->
        ConnectivityStatusBar()
        Spacer(modifier = Modifier.height(1.dp))
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> ConversationsScreen(
                    onChatClicked = { convId, name -> currentContactName = name; onConversationChanged(convId) },
                    onProfileClicked = onProfileClicked
                )
                1 -> PulsoScreen(onNavigateToQrScanner = onNavigateToQrScanner, onConnectToPeer = onConnectToPeer)
                2 -> PerfilScreen()
            }
        }
    }
}
