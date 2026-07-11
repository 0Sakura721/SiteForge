package com.siteforge.app.ui.screens

import android.net.wifi.WifiManager
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siteforge.app.SiteForgeApplication
import com.siteforge.app.ui.theme.*
import com.siteforge.app.server.SiteForgeWebServer

@Composable
fun DashboardScreen(
    serverRunning: Boolean,
    onToggleServer: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as SiteForgeApplication
    val sites by app.repository.allSites.collectAsStateWithLifecycle(initialValue = emptyList())

    val runningSites = sites.count { it.status == "running" }
    val stoppedSites = sites.size - runningSites

    val deviceIp = remember {
        try {
            val wifiManager = context.applicationContext.getSystemService(WifiManager::class.java)
            val ip = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (ip != 0) {
                String.format("%d.%d.%d.%d", ip and 0xff, ip shr 8 and 0xff, ip shr 16 and 0xff, ip shr 24 and 0xff)
            } else "127.0.0.1"
        } catch (e: Exception) { "127.0.0.1" }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "控制台",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 服务器状态卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (serverRunning) StatusRunningBg else StatusStoppedBg
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (serverRunning) Icons.Default.CloudDone else Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = if (serverRunning) StatusRunning else StatusStopped,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (serverRunning) "服务器运行中" else "服务器已停止",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (serverRunning) {
                        Text(
                            "http://$deviceIp:${SiteForgeWebServer.DEFAULT_PORT}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                Button(
                    onClick = onToggleServer,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (serverRunning) StatusStopped else StatusRunning
                    )
                ) {
                    Text(if (serverRunning) "停止" else "启动")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 统计卡片
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "站点总数",
                value = sites.size.toString(),
                icon = Icons.Default.Language,
                color = Primary
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "运行中",
                value = runningSites.toString(),
                icon = Icons.Default.PlayArrow,
                color = StatusRunning
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "已停止",
                value = stoppedSites.toString(),
                icon = Icons.Default.Stop,
                color = StatusStopped
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 设备信息
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "设备信息",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                InfoRow("设备 IP", deviceIp)
                InfoRow("服务器端口", SiteForgeWebServer.DEFAULT_PORT.toString())
                InfoRow("Android 版本", "API ${Build.VERSION.SDK_INT}")
                InfoRow("CPU 架构", Build.SUPPORTED_ABIS.joinToString(", "))
                InfoRow("应用版本", "1.0.0")
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                label,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 14.sp)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
