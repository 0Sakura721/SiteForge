package com.siteforge.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as SiteForgeApplication
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("static") }
    var description by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isCreating by remember { mutableStateOf(false) }

    // 表单
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "创建站点",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                // 站点名称
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.lowercase().replace(Regex("[^a-z0-9_-]"), "") },
                    label = { Text("站点名称 *") },
                    placeholder = { Text("my-website") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = error != null,
                    supportingText = {
                        if (error != null) Text(error!!, color = StatusStopped)
                        else Text("仅支持小写字母、数字、连字符和下划线，2-32个字符")
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 站点类型
                Text("站点类型", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TypeChip(
                        selected = type == "static",
                        label = "静态站点",
                        subtitle = "HTML/CSS/JS",
                        onClick = { type = "static" }
                    )
                    TypeChip(
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
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 创建按钮
                Button(
                    onClick = {
                        if (name.length < 2) {
                            error = "名称至少2个字符"
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
                                    val site = Site(name = name, type = type, description = description)
                                    app.repository.insert(site)

                                    // 创建站点目录和默认文件
                                    val siteDir = File(app.filesDir, "sites/$name")
                                    siteDir.mkdirs()
                                    generateDefaultSite(siteDir, type)
                                }
                                name = ""
                                description = ""
                                error = null
                                onSiteCreated()
                            } catch (e: Exception) {
                                error = "创建失败: ${e.message}"
                            } finally {
                                isCreating = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreating && name.length >= 2,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isCreating) "创建中..." else "🚀 立即创建")
                }
            }
        }
    }
}

@Composable
private fun TypeChip(
    selected: Boolean,
    label: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .weight(1f),
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) PrimaryLight else CardBackground,
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, Primary)
        else androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Language,
                contentDescription = null,
                tint = if (selected) Primary else TextSecondary,
                modifier = Modifier.size(28.dp)
            )
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
    <title>欢迎</title>
    <style>
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif;background:linear-gradient(135deg,#667eea,#764ba2);min-height:100vh;display:flex;align-items:center;justify-content:center}
.container{color:#fff;text-align:center;padding:2rem}
h1{font-size:2.5rem;margin-bottom:1rem}
p{font-size:1.1rem;opacity:.9}
    </style>
</head>
<body>
    <div class="container">
        <h1>🚀 站点搭建成功！</h1>
        <p>由 SiteForge 自动生成 — 运行在 Android 设备上</p>
    </div>
</body>
</html>"""
    File(siteDir, "index.html").writeText(indexContent)
}
