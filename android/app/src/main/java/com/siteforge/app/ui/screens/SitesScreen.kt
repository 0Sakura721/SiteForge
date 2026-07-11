package com.siteforge.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siteforge.app.SiteForgeApplication
import com.siteforge.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SitesScreen(onSiteSelected: (Long) -> Unit = {}) {
    val context = LocalContext.current
    val app = context.applicationContext as SiteForgeApplication
    val sites by app.repository.allSites.collectAsStateWithLifecycle(initialValue = emptyList())
    var showDeleteDialog by remember { mutableStateOf<com.siteforge.app.data.model.Site?>(null) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "站点管理",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (sites.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("还没有任何站点", color = TextSecondary, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("点击底部「创建」来新建站点\n或导入已有的 HTML 文件夹", color = TextSecondary, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(sites, key = { it.id }) { site ->
                    SiteListItem(
                        site = site,
                        onClick = { onSiteSelected(site.id) },
                        onDelete = { showDeleteDialog = site }
                    )
                }
            }
        }
    }

    showDeleteDialog?.let { site ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("删除站点") },
            text = {
                Text(
                    "确定要删除「${site.name}」吗？" +
                    if (site.customPath.isNotEmpty()) "\n\n⚠️ 导入的原始文件夹不会被删除。" else "\n\n所有站点文件将被永久删除，不可恢复。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) { app.repository.delete(site) }
                            showDeleteDialog = null
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = StatusStopped)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SiteListItem(
    site: com.siteforge.app.data.model.Site,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 状态图标
                Icon(
                    imageVector = if (site.status == "running") Icons.Default.PlayCircle else Icons.Default.StopCircle,
                    contentDescription = null,
                    tint = if (site.status == "running") StatusRunning else StatusStopped,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))

                // 站点信息
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            site.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = if (site.status == "running") StatusRunningBg else StatusStoppedBg,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                if (site.status == "running") "运行中" else "已停止",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 11.sp,
                                color = if (site.status == "running") StatusRunning else StatusStopped,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (site.description.isNotEmpty()) {
                        Text(
                            site.description,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 类型和路径信息
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            site.type.let { if (it == "static") "静态" else "SPA" },
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        if (site.customPath.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("外部目录", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }

                // 删除按钮
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = StatusStopped,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
