package com.siteforge.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.siteforge.app.ui.theme.*

/**
 * 首次使用引导教程
 * 4 步傻瓜式教学，让用户 30 秒内上手
 */
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 4

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 跳过按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onComplete) {
                Text("跳过教程", color = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 步骤内容
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                fadeIn() + slideInHorizontally { it / 4 } togetherWith
                fadeOut() + slideOutHorizontally { -it / 4 }
            }
        ) { step ->
            when (step) {
                0 -> StepWelcome()
                1 -> StepHowItWorks()
                2 -> StepCreateSite()
                3 -> StepAccess()
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 步骤指示器
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            for (i in 0 until totalSteps) {
                Box(
                    modifier = Modifier
                        .size(if (i == currentStep) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (i == currentStep) Primary else CardBorder)
                )
            }
        }

        // 底部按钮
        Button(
            onClick = {
                if (currentStep < totalSteps - 1) currentStep++
                else onComplete()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                if (currentStep < totalSteps - 1) "下一步" else "开始使用 🚀",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun StepWelcome() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // 大图标
        Surface(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            color = PrimaryLight
        ) {
            Icon(
                Icons.Default.Language,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "欢迎使用 SiteForge",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "把你的 Android 手机变成\n一台真正的 Web 服务器",
            fontSize = 16.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = StatusRunningBg)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text("💡", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "不需要服务器、不需要域名、不需要公网 IP。只要一部手机，就能搭建和运行网站。",
                    fontSize = 14.sp,
                    color = TextPrimary,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun StepHowItWorks() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Default.Settings,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "三步搭建你的网站",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 步骤列表
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TutorialStep(
                number = "1",
                title = "创建站点",
                desc = "输入站点名称，选择模板\n或导入你已有的 HTML 文件夹"
            )
            TutorialStep(
                number = "2",
                title = "启动服务",
                desc = "一键启动 Web 服务器\n同一 WiFi 下的设备都能访问"
            )
            TutorialStep(
                number = "3",
                title = "分享链接",
                desc = "把地址发给别人\n他们就能在浏览器里看你的网站"
            )
        }
    }
}

@Composable
private fun StepCreateSite() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Default.AddCircle,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "两种创建方式",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 方式一
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PrimaryLight)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text("📝", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "从零开始",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Text(
                        "选择模板自动生成网站，在文件管理器中编辑内容",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 方式二
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SecondaryLight)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text("📂", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "导入已有网站",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Text(
                        "选择手机上已有的 HTML 文件夹，直接作为站点使用",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StepAccess() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Default.Wifi,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "如何访问网站？",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = StatusRunningBg)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("✅ 同一 WiFi 网络", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "手机和访问设备（电脑/平板/其他手机）\n必须连接同一个 WiFi。",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("🔗 访问地址格式", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = CardBackground,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "http://192.168.x.x:8080/站点名/",
                        modifier = Modifier.padding(12.dp),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = Primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("⚠️ 注意事项", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "• 关闭 VPN/代理后再访问\n• 部分公共 WiFi 隔离了设备间通信\n• 用手机开热点最稳定",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun TutorialStep(number: String, title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = Primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    number,
                    color = androidx.compose.ui.graphics.Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(desc, fontSize = 14.sp, color = TextSecondary, lineHeight = 20.sp)
        }
    }
}
