package com.malla.mvp
import com.malla.mvp.network.MeshMessageHandler

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.core.Animatable
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
        // Mostrar error del inicio anterior si existe
        val crashFile = java.io.File(filesDir, "crash.txt")
        if (crashFile.exists()) {
            val errorText = crashFile.readText()
            // Mostrar una actividad simple de error
            val intent = android.content.Intent(this, CrashReportActivity::class.java)
            intent.putExtra("error", errorText)
            startActivity(intent)
            finish()
            return
        }

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                } else {
                }
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            } else {
            }
        }
        }
        // ConnectivityMonitor.start(application)
        // DeviceStateMonitor.start(this)
        IdentityManager.init(this)
        insertSampleStories()

        // val appThemeState = AppThemeState.create(this)

        val prefs = try {
            getSharedPreferences("malla_prefs", Context.MODE_PRIVATE)
        } catch (e: Exception) { null }
        val isFirstLaunch = try {
            prefs?.getBoolean("first_launch", true) ?: true
        } catch (e: Exception) { true }

        val database = AppDatabase.getInstance(application)

        setContent {
            MallaTheme(colorScheme = MallaColorScheme.MALLA_DARK) {
                var showSplash by remember { mutableStateOf(true) }
                if (showSplash) {
                    SplashScreen {
                        showSplash = false
                    }
                } else {
                    // Solo mostramos ConversationsScreen para probar
                    ConversationsScreen(
                        onChatClicked = { _, _ -> },
                        onProfileClicked = {}
                    )
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
    onChatSettingsClick: () -> Unit = {},
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
    val onProfileClick = { selectedTab = 2 }
    Scaffold(
        topBar = { MainTopBar(onSettingsClick = onSettingsClick, onChatSettingsClick = onChatSettingsClick, onProfileClick = onProfileClick, isOnline = !isMeshMode) },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.height(56.dp),
                containerColor = Color(0xFF0A1B2A)  // Azul petróleo profundo
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.AutoMirrored.Filled.Chat, "Chats") },
                    label = { Text("Chats") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF4CE6FF),
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = Color(0xFF4CE6FF),
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.WifiTethering, "Pulso") },
                    label = { Text("Pulso") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF4CE6FF),
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = Color(0xFF4CE6FF),
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.Person, "Perfil") },
                    label = { Text("Perfil") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF4CE6FF),
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = Color(0xFF4CE6FF),
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )
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

@Composable
fun PremiumNavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        if (selected) {
            scale.animateTo(1.2f, animationSpec = spring(dampingRatio = 0.35f, stiffness = 500f))
            scale.animateTo(1f, animationSpec = spring(dampingRatio = 0.35f, stiffness = 500f))
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(vertical = 8.dp).clip(RoundedCornerShape(12.dp))
    ) {
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) Color(0xFF4CE6FF) else Color.White.copy(alpha = 0.4f),
                modifier = Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .offset(y = 12.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CE6FF))
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            letterSpacing = 0.8.sp,
            color = if (selected) Color(0xFF4CE6FF) else Color.White.copy(alpha = 0.4f),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
