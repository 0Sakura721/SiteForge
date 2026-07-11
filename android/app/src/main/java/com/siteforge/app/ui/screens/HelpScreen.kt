package com.siteforge.app.ui.screens

import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.siteforge.app.server.SiteForgeWebServer
import com.siteforge.app.ui.theme.*

/**
 * 帮助与教程页面
 * 随时可查阅的详细操作指南
 */
@Composable
fun HelpScreen() {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    val deviceIp = remember {
        try {
            val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
            val ip = wifi?.connectionInfo?.ipAddress ?: 0
            if (ip != 0) String.format("%d.%d.%d.%d", ip and 0xff, ip shr 8 and 0xff, ip shr 16 and 0xff, ip shr 24 and 0xff)
            else "127.0.0.1"
        } catch (_: Exception) { "127.0.0.1" }
    }

    val accessUrl = "http://$deviceIp:${SiteForgeWebServer.DEFAULT_PORT}/"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            "📖 使用教程",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        // ---- 服务器地址 ----
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PrimaryLight)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🌐 你的服务器地址", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = CardBackground,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            clipboard.setText(AnnotatedString(accessUrl))
                            Toast.makeText(context, "地址已复制到剪贴板", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "复制",
                            tint = Primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            accessUrl,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Primary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("👆 点击复制地址", fontSize = 12.sp, color = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---- 快速入门 ----
        SectionHeader("🚀 快速入门（3分钟）")

        HelpStep(
            num = "1",
            title = "启动服务器",
            desc = "在「控制台」页面，点击「启动」按钮。首次使用需要授予通知权限。"
        )
        HelpStep(
            num = "2",
            title = "创建站点",
            desc = "切换到「创建」标签，输入站点名称（如 my-blog），选择类型后创建。应用会自动生成一个示例网站。"
        )
        HelpStep(
            num = "3",
            title = "访问网站",
            desc = "确保手机和电脑在同一 WiFi。在电脑浏览器输入上面的地址 + 站点名，如：http://IP:8080/my-blog/"
        )
        HelpStep(
            num = "4",
            title = "编辑内容",
            desc = "使用文件管理器（通过 Web 管理面板访问）修改 index.html 和 css/style.css 来定制你的网站。"
        )

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        // ---- 导入已有网站 ----
        SectionHeader("📂 如何导入已有网站")

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                BulletPoint("创建站点时，选择「导入已有文件夹」选项")
                BulletPoint("点击「选择文件夹」浏览手机存储")
                BulletPoint("选择包含 index.html 的文件夹")
                BulletPoint("文件夹中的所有文件都会被作为网站内容")
                BulletPoint("注：导入不会复制文件，直接使用原始路径")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ---- 常见问题 ----
        SectionHeader("❓ 常见问题")

        FaqItem(
            q = "为什么电脑访问不了？",
            a = "1. 确认手机和电脑在同一个 WiFi\n2. 确认服务器已经启动（控制台显示绿色）\n3. 关闭 VPN 和代理软件\n4. 检查防火墙是否拦截了 8080 端口"
        )
        FaqItem(
            q = "可以放 PHP 文件吗？",
            a = "目前 SiteForge 支持静态网站（HTML/CSS/JS）和单页应用。PHP 需要额外的服务器环境，暂不支持。"
        )
        FaqItem(
            q = "手机连了热点能用吗？",
            a = "可以！手机开热点是最稳定的方式。用其他设备连上手机热点，然后用手机显示的 IP 地址访问。"
        )
        FaqItem(
            q = "能同时运行多个网站吗？",
            a = "可以。创建多个站点，它们共享同一个 Web 服务器（8080 端口），通过不同的路径访问：/站点名1/ 和 /站点名2/"
        )
        FaqItem(
            q = "如何让别人通过外网访问？",
            a = "需要做端口映射（内网穿透）。可以使用 frp、ngrok 等工具将手机的 8080 端口暴露到公网。这不属于 SiteForge 的功能范围。"
        )
        FaqItem(
            q = "网站文件存在哪里？",
            a = "默认存储在应用私有目录。如果使用「导入文件夹」功能，使用的是你选择的原始文件夹路径。"
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun HelpStep(num: String, title: String, desc: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Primary,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        num,
                        color = androidx.compose.ui.graphics.Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(desc, fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("•", color = Primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 13.sp, color = TextPrimary, lineHeight = 18.sp)
    }
}

@Composable
private fun FaqItem(q: String, a: String) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Q:", color = Primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(q, fontWeight = FontWeight.Medium, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(a, fontSize = 13.sp, color = TextSecondary, lineHeight = 20.sp)
            }
        }
    }
}
