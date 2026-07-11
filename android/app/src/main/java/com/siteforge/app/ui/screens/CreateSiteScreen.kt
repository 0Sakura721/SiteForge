package com.siteforge.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import com.siteforge.app.SiteForgeApplication
import com.siteforge.app.data.model.Site
import com.siteforge.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun CreateSiteScreen(
    onSiteCreated: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as SiteForgeApplication
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("static") }
    var description by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isCreating by remember { mutableStateOf(false) }
    var useCustomDir by remember { mutableStateOf(false) }
    var selectedDirUri by remember { mutableStateOf<Uri?>(null) }
    var selectedDirName by remember { mutableStateOf("") }

    // SAF 目录选择器
    val dirPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            // 持久化权限
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(it, flags)
            selectedDirUri = it
            // 从 URI 提取目录名
            selectedDirName = it.lastPathSegment ?: "已选文件夹"
            // 自动填充站点名
            if (name.isEmpty()) {
                name = selectedDirName.replace(Regex("[^a-zA-Z0-9_-]"), "-").lowercase()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "创建站点",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        // ---- 创建方式选择 ----
        Text("创建方式", fontWeight = FontWeight.Medium, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OptionChip(
                modifier = Modifier.weight(1f),
                selected = !useCustomDir,
                label = "使用模板",
                subtitle = "自动生成网站",
                icon = Icons.Default.AutoAwesome,
                onClick = { useCustomDir = false }
            )
            OptionChip(
                modifier = Modifier.weight(1f),
                selected = useCustomDir,
                label = "导入文件夹",
                subtitle = "选择已有文件",
                icon = Icons.Default.FolderOpen,
                onClick = { useCustomDir = true }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ---- 配置表单 ----
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {

                // 站点名称
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.lowercase().replace(Regex("[^a-z0-9_-]"), "") },
                    label = { Text("站点名称 *") },
                    placeholder = { Text("my-website") },
                    leadingIcon = { Icon(Icons.Default.Label, null, modifier = Modifier.size(20.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = error != null,
                    supportingText = {
                        if (error != null) Text(error!!, color = StatusStopped)
                        else Text("仅支持小写字母、数字、连字符和下划线，2-32个字符")
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 自定义目录：选择文件夹按钮
                if (useCustomDir) {
                    OutlinedButton(
                        onClick = { dirPickerLauncher.launch(null) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                        border = BorderStroke(1.dp, Primary)
                    ) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (selectedDirName.isEmpty()) "📁 选择 HTML 文件夹"
                            else "已选: $selectedDirName"
                        )
                    }

                    if (selectedDirName.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "将直接使用此文件夹中的文件作为网站内容",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    // 模板模式：显示类型和描述
                    Text("站点类型", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TypeChip(
                            modifier = Modifier.weight(1f),
                            selected = type == "static",
                            label = "静态站点",
                            subtitle = "HTML/CSS/JS",
                            onClick = { type = "static" }
                        )
                        TypeChip(
                            modifier = Modifier.weight(1f),
                            selected = type == "single-page",
                            label = "单页应用",
                            subtitle = "SPA",
                            onClick = { type = "single-page" }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 描述
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("描述（可选）") },
                        placeholder = { Text("简要描述此站点的用途") },
                        leadingIcon = { Icon(Icons.Default.Description, null, modifier = Modifier.size(20.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 创建按钮
                Button(
                    onClick = {
                        if (name.length < 2) {
                            error = "名称至少2个字符"
                            return@Button
                        }
                        if (useCustomDir && selectedDirUri == null) {
                            error = "请先选择一个文件夹"
                            return@Button
                        }
                        error = null
                        isCreating = true
                        scope.launch {
                            try {
                                val exists = withContext(Dispatchers.IO) {
                                    app.repository.isNameTaken(name)
                                }
                                if (exists) {
                                    error = "站点「$name」已存在"
                                    isCreating = false
                                    return@launch
                                }

                                withContext(Dispatchers.IO) {
                                    val customPath = if (useCustomDir && selectedDirUri != null) {
                                        selectedDirUri.toString()
                                    } else ""

                                    val site = Site(
                                        name = name,
                                        type = type,
                                        description = if (useCustomDir) "导入: $selectedDirName" else description,
                                        customPath = customPath
                                    )
                                    app.repository.insert(site)

                                    // 模板模式：创建目录和默认文件
                                    if (!useCustomDir) {
                                        val siteDir = File(app.filesDir, "sites/$name")
                                        siteDir.mkdirs()
                                        generateDefaultSite(siteDir, type)
                                    }
                                }
                                name = ""
                                description = ""
                                selectedDirUri = null
                                selectedDirName = ""
                                error = null
                                onSiteCreated()
                            } catch (e: Exception) {
                                error = "创建失败: ${e.message}"
                            } finally {
                                isCreating = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !isCreating && name.length >= 2 && (!useCustomDir || selectedDirUri != null),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(Icons.Default.RocketLaunch, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isCreating) "创建中..." else "立即创建", fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun OptionChip(
    modifier: Modifier,
    selected: Boolean,
    label: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) PrimaryLight else CardBackground,
        border = if (selected) BorderStroke(2.dp, Primary) else BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = if (selected) Primary else TextSecondary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, fontSize = 11.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun TypeChip(
    modifier: Modifier = Modifier,
    selected: Boolean,
    label: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) PrimaryLight else CardBackground,
        border = if (selected) BorderStroke(2.dp, Primary) else BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Language, null, tint = if (selected) Primary else TextSecondary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(label, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(subtitle, fontSize = 11.sp, color = TextSecondary)
        }
    }
}

private fun generateDefaultSite(siteDir: File, type: String) {
    val indexContent = """<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>欢迎来到我的站点</title>
    <style>
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,"PingFang SC","Microsoft YaHei",sans-serif;background:linear-gradient(135deg,#667eea,#764ba2);min-height:100vh;display:flex;align-items:center;justify-content:center}
.container{max-width:680px;margin:2rem;text-align:center;color:#fff}
h1{font-size:2.4rem;margin-bottom:1rem}
p{font-size:1.1rem;opacity:.9;line-height:1.8}
.card{background:rgba(255,255,255,.15);border-radius:16px;padding:1.5rem;margin:1.5rem 0;backdrop-filter:blur(10px)}
.card h2{font-size:1.2rem;margin-bottom:.5rem}
.highlight{background:rgba(255,255,255,.2);padding:.5rem 1rem;border-radius:8px;font-family:monospace;font-size:.9rem;word-break:break-all}
a{color:#ffd54f;text-decoration:none}
    </style>
</head>
<body>
    <div class="container">
        <h1>🚀 站点搭建成功！</h1>
        <p>由 <strong>SiteForge</strong> 自动生成<br>运行在你的 Android 设备上</p>
        <div class="card">
            <h2>开始定制你的网站</h2>
            <p>编辑 <code>index.html</code> 和 <code>css/style.css</code> 来修改此页面</p>
            <p>通过 SiteForge 的 Web 管理面板即可在线编辑</p>
        </div>
        <div class="card">
            <h2>访问地址</h2>
            <p>同一 WiFi 下，其他设备访问：</p>
            <p class="highlight">http://设备IP:8080/站点名/</p>
        </div>
        <p style="margin-top:2rem;opacity:.6;font-size:.85rem">&copy; 2026 SiteForge · Android 自动建站平台</p>
    </div>
</body>
</html>"""
    File(siteDir, "index.html").writeText(indexContent)
    val cssDir = File(siteDir, "css")
    cssDir.mkdirs()
    File(cssDir, "style.css").writeText("/* 在此添加你的自定义样式 */\n")
}
