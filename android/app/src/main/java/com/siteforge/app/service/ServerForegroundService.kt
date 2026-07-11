package com.siteforge.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.siteforge.app.MainActivity
import com.siteforge.app.R
import com.siteforge.app.SiteForgeApplication
import com.siteforge.app.server.SiteForgeWebServer
import kotlinx.coroutines.*

/**
 * 前台服务：保持 Web 服务器在后台持续运行
 * 适配 Android 8+ 的前台服务限制和 Android 14+ 的 foregroundServiceType
 */
class ServerForegroundService : Service() {

    companion object {
        private const val TAG = "ServerForeground"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.siteforge.action.STOP_SERVER"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var webServer: SiteForgeWebServer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "前台服务创建")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopServer()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIFICATION_ID, createNotification())
                startServer()
            }
        }
        return START_STICKY
    }

    private fun startServer() {
        serviceScope.launch {
            try {
                val port = 8080
                webServer = SiteForgeWebServer(applicationContext, port)
                webServer?.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
                Log.i(TAG, "Web 服务器已启动，端口: $port")

                // 获取 Wi-Fi 锁，防止设备休眠
                acquireWakeLock()

                updateNotification("运行中 :$port")
            } catch (e: Exception) {
                Log.e(TAG, "服务器启动失败", e)
                stopSelf()
            }
        }
    }

    private fun stopServer() {
        try {
            webServer?.stop()
            webServer = null
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
            Log.i(TAG, "Web 服务器已停止")
        } catch (e: Exception) {
            Log.e(TAG, "停止服务器异常", e)
        }
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SiteForge:ServerWakeLock"
        ).apply {
            acquire(10 * 60 * 1000L) // 10分钟超时
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, ServerForegroundService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, SiteForgeApplication.CHANNEL_SERVER)
            .setContentTitle("SiteForge")
            .setContentText("服务器运行中")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "停止服务", stopIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = NotificationCompat.Builder(this, SiteForgeApplication.CHANNEL_SERVER)
            .setContentTitle("SiteForge")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopServer()
        serviceScope.cancel()
        super.onDestroy()
    }
}

// 用于 start() 调用的 NanoHTTPD 引用
object NanoHTTPD {
    const val SOCKET_READ_TIMEOUT = 5000
}
