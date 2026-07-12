package com.siteforge.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.siteforge.app.server.SiteForgeWebServer
import com.siteforge.app.service.ServerForegroundService
import com.siteforge.app.ui.screens.*
import com.siteforge.app.ui.theme.*
import java.io.File

class MainActivity : ComponentActivity() {

    private var webServer: SiteForgeWebServer? = null
    private var serverRunning by mutableStateOf(false)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startServerService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        File(filesDir, "sites").mkdirs()

        // 检查是否首次启动
        val prefs = getSharedPreferences("siteforge_prefs", Context.MODE_PRIVATE)

        setContent {
            SiteForgeTheme {
                var showOnboarding by remember {
                    mutableStateOf(!prefs.getBoolean("has_seen_onboarding", false))
                }
                if (showOnboarding) {
                    OnboardingScreen(onComplete = {
                        prefs.edit().putBoolean("has_seen_onboarding", true).apply()
                        showOnboarding = false
                    })
                } else {
                    MainApp(
                        serverRunning = serverRunning,
                        onToggleServer = { toggleServer() },
                        onShowOnboarding = {
                            // 重置后重新显示
                            prefs.edit().putBoolean("has_seen_onboarding", false).apply()
                            recreate()
                        }
                    )
                }
            }
        }
    }

    private fun toggleServer() {
        if (serverRunning) {
            stopServerService()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED
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
    onToggleServer: () -> Unit,
    onShowOnboarding: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(24.dp)) },
                    label = { Text("控制台", fontSize = 11.sp) },
                    colors = navColors(selectedTab == 0)
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(24.dp)) },
                    label = { Text("站点", fontSize = 11.sp) },
                    colors = navColors(selectedTab == 1)
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Surface(
                            shape = CircleShape,
                            color = if (selectedTab == 2) Primary else Primary.copy(alpha = 0.85f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = androidx.compose.ui.graphics.Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    },
                    label = { Text("创建", fontSize = 11.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    colors = navColors(selectedTab == 2)
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(24.dp)) },
                    label = { Text("教程", fontSize = 11.sp) },
                    colors = navColors(selectedTab == 3)
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(24.dp)) },
                    label = { Text("设置", fontSize = 11.sp) },
                    colors = navColors(selectedTab == 4)
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> DashboardScreen(
                    serverRunning = serverRunning,
                    onToggleServer = onToggleServer
                )
                1 -> SitesScreen()
                2 -> CreateSiteScreen(onSiteCreated = { selectedTab = 1 })
                3 -> HelpScreen()
                4 -> SettingsScreen(onShowOnboarding = onShowOnboarding)
            }
        }
    }
}

@Composable
private fun navColors(selected: Boolean) = NavigationBarItemDefaults.colors(
    selectedIconColor = Primary,
    selectedTextColor = Primary,
    indicatorColor = PrimaryLight
)

/**
 * 系统设置页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onShowOnboarding: () -> Unit = {}
) {
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

        // 设备信息
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📱 设备信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))

                val ip = remember {
                    try {
                        val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
                        val ipInt = wifi?.connectionInfo?.ipAddress ?: 0
                        if (ipInt != 0) String.format("%d.%d.%d.%d", ipInt and 0xff, ipInt shr 8 and 0xff, ipInt shr 16 and 0xff, ipInt shr 24 and 0xff)
                        else "未连接 WiFi"
                    } catch (_: Exception) { "未知" }
                }

                InfoRow("设备 IP", ip)
                InfoRow("服务器端口", "${SiteForgeWebServer.DEFAULT_PORT}")
                InfoRow("Android 版本", "API ${Build.VERSION.SDK_INT}")
                InfoRow("CPU 架构", Build.SUPPORTED_ABIS.firstOrNull() ?: "未知")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 操作
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("⚙️ 操作", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onShowOnboarding,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("重新查看引导教程")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 关于
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("ℹ️ 关于", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))
                AboutRow("应用名称", "SiteForge")
                AboutRow("版本", "1.0.0")
                AboutRow("目标 SDK", "36 (Android 16)")
                AboutRow("最低支持", "API 24 (Android 7.0)")
                AboutRow("架构支持", "armeabi-v7a, arm64-v8a")

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "SiteForge 让你的 Android 手机变成一台 Web 服务器。在同一 WiFi 下，任何设备都能访问你搭建的网站。无需域名，无需公网 IP，即开即用。",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 14.sp)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
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
