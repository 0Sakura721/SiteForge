package com.siteforge.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.siteforge.app.data.AppDatabase
import com.siteforge.app.data.SiteRepository

class SiteForgeApplication : Application() {

    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy { SiteRepository(database.siteDao()) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_SERVER,
                getString(R.string.notification_channel_server),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "SiteForge 服务器运行状态"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_SERVER = "siteforge_server"

        lateinit var instance: SiteForgeApplication
            private set
    }
}
