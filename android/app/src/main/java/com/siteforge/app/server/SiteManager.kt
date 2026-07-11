package com.siteforge.app.server

import android.content.Context
import com.siteforge.app.data.model.Site
import java.io.File

/**
 * 站点管理器
 * 负责站点的创建、文件部署、状态管理。
 */
class SiteManager(private val context: Context) {

    private val sitesDir: File
        get() = File(context.filesDir, "sites").also { it.mkdirs() }

    /** 内存中的站点缓存（用于服务器快速查询） */
    private val siteCache = mutableMapOf<Long, Site>()

    fun getAllSites(): List<Site> {
        if (siteCache.isEmpty()) {
            loadFromDisk()
        }
        return siteCache.values.toList().sortedByDescending { it.id }
    }

    fun getSiteById(id: Long): Site? {
        if (siteCache.isEmpty()) loadFromDisk()
        return siteCache[id]
    }

    fun getSiteByName(name: String): Site? {
        if (siteCache.isEmpty()) loadFromDisk()
        return siteCache.values.find { it.name == name }
    }

    fun createSite(name: String, type: String, template: String, description: String): Site {
        if (getSiteByName(name) != null) {
            throw IllegalArgumentException("站点「$name」已存在")
        }

        val port = findAvailablePort()
        val siteDir = File(sitesDir, name)
        siteDir.mkdirs()

        // 部署模板文件
        deployTemplate(siteDir, type, template)

        val site = Site(
            name = name,
            type = type,
            template = template,
            port = port,
            status = "stopped",
            description = description
        )
        siteCache[site.id] = site
        return site
    }

    fun startSite(id: Long): Site? {
        val site = getSiteById(id) ?: return null
        val updatedSite = site.copy(status = "running", updatedAt = System.currentTimeMillis())
        siteCache[id] = updatedSite
        return updatedSite
    }

    fun stopSite(id: Long): Site? {
        val site = getSiteById(id) ?: return null
        val updatedSite = site.copy(status = "stopped", updatedAt = System.currentTimeMillis())
        siteCache[id] = updatedSite
        return updatedSite
    }

    fun deleteSite(id: Long) {
        val site = getSiteById(id) ?: return
        // 删除站点目录
        File(sitesDir, site.name).deleteRecursively()
        siteCache.remove(id)
        // 重新分配 ID
        saveToDisk()
    }

    /**
     * 部署网站模板到站点目录
     */
    private fun deployTemplate(siteDir: File, type: String, template: String) {
        // 生成默认站点页面
        val indexContent = generateDefaultPage(type)
        val cssContent = generateDefaultCss()

        File(siteDir, "index.html").writeText(indexContent)
        val cssDir = File(siteDir, "css")
        cssDir.mkdirs()
        File(cssDir, "style.css").writeText(cssContent)
    }

    private fun generateDefaultPage(type: String): String {
        return """<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>欢迎来到我的站点</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
    <div class="container">
        <header>
            <h1>🚀 站点搭建成功！</h1>
            <p>由 <strong>SiteForge</strong> 自动生成 — 运行在 Android 设备上</p>
        </header>
        <main>
            <section class="card">
                <h2>开始构建你的网站</h2>
                <p>这是一个 <strong>${if (type == "static") "静态" else "单页应用"}</strong> 站点。</p>
                <p>你可以通过 SiteForge 的文件管理器编辑此页面。</p>
            </section>
            <section class="card">
                <h2>访问方式</h2>
                <p>在同一 Wi-Fi 网络下，其他设备可通过以下地址访问：</p>
                <p class="highlight">http://设备IP:端口/站点名/</p>
            </section>
        </main>
        <footer>
            <p>&copy; 2026 SiteForge · Android 自动建站平台</p>
        </footer>
    </div>
</body>
</html>"""
    }

    private fun generateDefaultCss(): String {
        return """
* { margin: 0; padding: 0; box-sizing: border-box; }
body {
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", "Microsoft YaHei", sans-serif;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
}
.container { max-width: 720px; margin: 2rem; }
header { text-align: center; color: #fff; margin-bottom: 2rem; }
header h1 { font-size: 2.2rem; margin-bottom: 0.5rem; }
header p { font-size: 1.05rem; opacity: 0.9; }
.card {
    background: #fff;
    border-radius: 16px;
    padding: 1.75rem;
    margin-bottom: 1rem;
    box-shadow: 0 20px 60px rgba(0,0,0,0.12);
}
.card h2 { color: #333; margin-bottom: 0.75rem; font-size: 1.2rem; }
.card p { color: #555; line-height: 1.8; }
.highlight {
    background: #f0f0f0;
    padding: 0.5rem 1rem;
    border-radius: 8px;
    font-family: "SF Mono", monospace;
    font-size: 0.9rem;
    word-break: break-all;
}
footer { text-align: center; color: rgba(255,255,255,0.7); margin-top: 2rem; font-size: 0.85rem; }
@media (max-width: 600px) {
    .container { margin: 1rem; }
    header h1 { font-size: 1.6rem; }
}
""".trimIndent()
    }

    private fun findAvailablePort(): Int {
        var port = 8081
        val usedPorts = siteCache.values.map { it.port }.toSet()
        while (port in usedPorts) port++
        return port
    }

    private fun loadFromDisk() {
        // 从应用内部存储恢复站点信息
        siteCache.clear()
    }

    private fun saveToDisk() {
        // 持久化到 Room 数据库（通过 Application.repository）
    }
}
