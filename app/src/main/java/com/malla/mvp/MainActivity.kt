package com.malla.mvp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malla.mvp.core.transport.FlashlightTransport
import com.malla.mvp.data.AppDatabase
import com.malla.mvp.data.entity.ConversationEntity
import com.malla.mvp.identity.IdentityManager
import com.malla.mvp.network.ConnectivityMonitor
import com.malla.mvp.util.RadioManager
import com.malla.mvp.service.MeshChatService
import com.malla.mvp.network.MeshMessageHandler
import com.malla.mvp.core.engine.DeviceStateMonitor
import com.malla.mvp.core.engine.LogBuffer
import com.malla.mvp.network.NetworkService
import com.malla.mvp.ui.components.MainTopBar
import com.malla.mvp.ui.components.StickerPickerDialog
import com.malla.mvp.ui.components.StickerFullScreenDialog
import com.malla.mvp.ui.components.SplashScreen
import com.malla.mvp.ui.components.StickerState
import com.malla.mvp.ui.components.ConnectivityStatusBar
import com.malla.mvp.ui.components.TutorialOverlay
import com.malla.mvp.ui.screen.*
import com.malla.mvp.ui.settings.AccessibilitySettings
import com.malla.mvp.ui.theme.MallaColorScheme
import com.malla.mvp.R
import com.malla.mvp.ui.theme.MallaTheme
import com.malla.mvp.viewmodel.AppThemeState
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppState { Splash, Onboarding, Main }

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val requiredPermissions = arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_ADVERTISE,
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.POST_NOTIFICATIONS,
                android.Manifest.permission.READ_CONTACTS,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
            val ungranted = requiredPermissions.filter {
                checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED
            }.toTypedArray()
            if (ungranted.isNotEmpty()) requestPermissions(ungranted, 1001)
        RadioManager.enableBluetooth(this)
        RadioManager.enableWifi(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, MeshChatService::class.java))
        } else {
            startService(Intent(this, MeshChatService::class.java))
        }
        }
        ConnectivityMonitor.start(application)
        DeviceStateMonitor.start(this)
        IdentityManager.init(this)
        insertSampleStories()

        val appThemeState = AppThemeState.create(this)

        val prefs = try {
            getSharedPreferences("malla_prefs", Context.MODE_PRIVATE)
        } catch (e: Exception) { null }
        val isFirstLaunch = try {
            prefs?.getBoolean("first_launch", true) ?: true
        } catch (e: Exception) { true }

        val database = AppDatabase.getInstance(application)

        setContent {
            var appState by remember { mutableStateOf(AppState.Splash) }
            var showQrScanner by remember { mutableStateOf(false) }
            var currentConversationId by remember { mutableStateOf<String?>(null) }
            var selectedContact by remember { mutableStateOf<String?>(null) }
            var showSettings by remember { mutableStateOf(false) }
            var showCall by remember { mutableStateOf(false) }
            var callContact by remember { mutableStateOf("") }
            var callType by remember { mutableStateOf("voice") }
            var showTutorial by remember { mutableStateOf(false) }
            val context = LocalContext.current
            val flashlight = remember { FlashlightTransport(context) }

            val effectiveScheme by appThemeState.currentTheme.collectAsState()

            val isOnline by ConnectivityMonitor.isOnline.collectAsState()
            LaunchedEffect(isOnline) {
                try {
                    if (!isOnline) {
                        // MeshChatService eliminado temporalmente
                        LogBuffer.add("MAIN", "Iniciando servicio mesh")
                        NetworkService.startServer()
                        LogBuffer.add("MAIN", "NetworkService iniciado")
                        MeshMessageHandler.start(application)
                        LogBuffer.add("MAIN", "MeshMessageHandler iniciado")
                    } else {
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
                        if (!isFirstLaunch) {
                        showTutorial = true
                        }
                    }
                }
            }

            MallaTheme(colorScheme = effectiveScheme, fontScale = AccessibilitySettings.fontScale.floatValue) {
                AnimatedContent(
                    targetState = appState,
                    transitionSpec = {
                        (slideInHorizontally { width -> width } + fadeIn(tween(300))) togetherWith
                                (slideOutHorizontally { width -> -width } + fadeOut(tween(300)))
                    },
                    label = "app_state_transition"
                ) { state ->
                    when (state) {
                        AppState.Splash -> SplashScreen {
                            if (isFirstLaunch) {
                                try { prefs?.edit()?.putBoolean("first_launch", false)?.apply() } catch (_: Exception) {}
                                appState = AppState.Onboarding
                            } else appState = AppState.Main
                        }
                        AppState.Onboarding -> IdentityOnboardingScreen {
                            appState = AppState.Main
                            val tutorialPrefs = getSharedPreferences("tutorial", MODE_PRIVATE)
                            val alreadyShown = tutorialPrefs.getBoolean("shown", false)
                            if (!alreadyShown) {
                                showTutorial = true
                            }
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
                                BackHandler { showQrScanner = false }
                                QrScanScreen(
                                    onQrScanned = { ip ->
                                        showQrScanner = false
                                        connectToPeerAndCreateConversation(ip) { convId -> currentConversationId = convId }
                                    },
                                    onBack = { showQrScanner = false }
                                )
                            } else if (showSettings) {
                                BackHandler { showSettings = false }
                                SettingsScreenWrapper(
                                    currentScheme = effectiveScheme,
                                    onSchemeSelected = { scheme -> appThemeState.selectScheme(scheme) },
                                    onBack = { showSettings = false }
                                )
                            } else if (selectedContact != null) {
                                BackHandler { selectedContact = null }
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
                                    onVoiceCallClick = { showCall = true; callContact = "Contacto"; callType = "voice" },
                                    onVideoCallClick = { showCall = true; callContact = "Contacto"; callType = "video" },
                                    db = database
                                )
                                if (showCall) {
                                    CallScreen(
                                        contactName = callContact,
                                        callType = callType,
                                        onEndCall = { showCall = false }
                                    )
                                }
                            }
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
    override fun onDestroy() {
        RadioManager.restoreStates(this)
        super.onDestroy()
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



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    onVoiceCallClick: () -> Unit = {},
    onVideoCallClick: () -> Unit = {},
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
    var lastBackPressTime by remember { mutableStateOf(0L) }
    val backContext = LocalContext.current

    BackHandler {
        if (currentConversationId != null) {
            onConversationChanged(null)
            return@BackHandler
        }
        if (selectedTab != 0) {
            selectedTab = 0
            return@BackHandler
        }
        if (lastBackPressTime + 2000 > System.currentTimeMillis()) {
            (backContext as? android.app.Activity)?.finish()
        } else {
            Toast.makeText(backContext, "Presiona de nuevo para salir", Toast.LENGTH_SHORT).show()
        }
        lastBackPressTime = System.currentTimeMillis()
    }

    if (currentConversationId != null) {
        ChatScreen(
            conversationId = currentConversationId,
            contactName = currentContactName,
            onBack = { onConversationChanged(null) },
            isMeshMode = isMeshMode,
            onVoiceCallClick = onVoiceCallClick,
            onVideoCallClick = onVideoCallClick
        )
        return
    }
    Scaffold(
        topBar = { MainTopBar(onSettingsClick = onSettingsClick, isOnline = !isMeshMode) },
        bottomBar = {
            NavigationBar(modifier = Modifier.height(56.dp), containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { BadgedBox(badge = {}) { Icon(Icons.AutoMirrored.Filled.Chat, "Chats") } }, label = { Text("Chats") })
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
    StickerPickerDialog()
    StickerFullScreenDialog()
}
