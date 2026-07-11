package com.siteforge.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.siteforge.app.server.SiteForgeWebServer
import com.siteforge.app.service.ServerForegroundService
import com.siteforge.app.ui.screens.*
import com.siteforge.app.ui.theme.*
import kotlinx.coroutines.*
import java.io.File

class MainActivity : ComponentActivity() {

    private var webServer: SiteForgeWebServer? = null
    private var serverRunning by mutableStateOf(false)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startServerService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 确保站点目录存在
        File(filesDir, "sites").mkdirs()

        setContent {
            SiteForgeTheme {
                MainApp(serverRunning = serverRunning, onToggleServer = { toggleServer() })
            }
        }
    }

    private fun toggleServer() {
        if (serverRunning) {
            stopServerService()
        } else {
            // Android 13+ 需要通知权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        this, Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    startServerService()
                } else {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                startServerService()
            }
        }
    }

    private fun startServerService() {
        val intent = Intent(this, ServerForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        serverRunning = true
    }

    private fun stopServerService() {
        val intent = Intent(this, ServerForegroundService::class.java).apply {
            action = ServerForegroundService.ACTION_STOP
        }
        startService(intent)
        webServer?.stop()
        webServer = null
        serverRunning = false
    }

    override fun onDestroy() {
        super.onDestroy()
        webServer?.stop()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    serverRunning: Boolean,
    onToggleServer: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        Triple("控制台", Icons.Default.Dashboard, 0),
        Triple("站点", Icons.Default.Language, 1),
        Triple("创建", Icons.Default.AddCircle, 2),
        Triple("设置", Icons.Default.Settings, 3),
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                tabs.forEach { (label, icon, index) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                icon,
                                contentDescription = label,
                                modifier = Modifier.size(if (index == 2) 28.dp else 24.dp)
                            )
                        },
                        label = {
                            Text(
                                label,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Primary,
                            selectedTextColor = Primary,
                            indicatorColor = PrimaryLight
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> DashboardScreen(serverRunning = serverRunning, onToggleServer = onToggleServer)
                1 -> SitesScreen()
                2 -> CreateSiteScreen(onSiteCreated = { selectedTab = 1 })
                3 -> SettingsScreen()
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "系统设置",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("关于", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))

                AboutRow("应用名称", "SiteForge")
                AboutRow("版本", "1.0.0")
                AboutRow("平台", "Android ${Build.VERSION.SDK_INT}")
                AboutRow("架构", "ARM64")
                AboutRow("目标 SDK", "36 (Android 16)")
                AboutRow("最低支持", "API 24 (Android 7.0)")

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "SiteForge 是一款运行在 Android 设备上的自动建站平台，",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Text(
                    "内置轻量级 Web 服务器，支持在同一 Wi-Fi 网络下让其他设备访问你的网站。",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 14.sp)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
