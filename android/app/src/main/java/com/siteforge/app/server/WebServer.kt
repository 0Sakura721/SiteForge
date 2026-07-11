package com.siteforge.app.server

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileNotFoundException
import java.net.ServerSocket

/**
 * SiteForge 嵌入式 Web 服务器
 * 基于 NanoHTTPD，支持多站点、静态文件服务、MIME 类型识别。
 */
class SiteForgeWebServer(
    private val context: Context,
    port: Int = DEFAULT_PORT
) : NanoHTTPD(port) {

    companion object {
        private const val TAG = "SiteForgeServer"
        const val DEFAULT_PORT = 8080

        private val MIME_TYPES = mapOf(
            "html" to "text/html; charset=utf-8",
            "htm" to "text/html; charset=utf-8",
            "css" to "text/css; charset=utf-8",
            "js" to "application/javascript; charset=utf-8",
            "json" to "application/json; charset=utf-8",
            "xml" to "application/xml; charset=utf-8",
            "txt" to "text/plain; charset=utf-8",
            "md" to "text/markdown; charset=utf-8",
            "png" to "image/png",
            "jpg" to "image/jpeg",
            "jpeg" to "image/jpeg",
            "gif" to "image/gif",
            "svg" to "image/svg+xml",
            "ico" to "image/x-icon",
            "webp" to "image/webp",
            "woff" to "font/woff",
            "woff2" to "font/woff2",
            "ttf" to "font/ttf",
            "pdf" to "application/pdf",
            "zip" to "application/zip",
            "mp4" to "video/mp4",
            "mp3" to "audio/mpeg",
        )
    }

    private val sitesDir: File
        get() = File(context.filesDir, "sites")

    private val siteManager = SiteManager(context)

    fun isRunning(): Boolean = wasStarted() && isAlive

    override fun serve(session: IHTTPSession): Response {
        return try {
            handleRequest(session)
        } catch (e: Exception) {
            Log.e(TAG, "请求处理异常", e)
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "text/html",
                "<h1>500 - 服务器内部错误</h1><p>${e.message}</p>"
            )
        }
    }

    private fun handleRequest(session: IHTTPSession): Response {
        val uri = session.uri.removePrefix("/").ifEmpty { "index.html" }
        val parts = uri.split("/", limit = 2)

        if (parts.isEmpty()) {
            return serveAdminPanel()
        }

        val siteName = parts[0]
        val filePath = if (parts.size > 1) parts[1] else "index.html"

        // 根路径 -> 管理面板
        if (siteName.isEmpty()) {
            return serveAdminPanel()
        }

        // API 路由
        if (siteName == "api") {
            return handleApi(session, filePath)
        }

        // 站点代理: /站点名/文件路径
        val site = siteManager.getSiteByName(siteName)
        if (site == null || site.status != "running") {
            return newFixedLengthResponse(
                Response.Status.SERVICE_UNAVAILABLE,
                "text/html",
                "<h1>站点未启动</h1><p>请先在管理面板中启动站点「${siteName}」</p>"
            )
        }

        return serveSiteFile(site.name, filePath)
    }

    private fun serveAdminPanel(): Response {
        val html = AdminPanelGenerator.generate(context, siteManager)
        return newFixedLengthResponse(Response.Status.OK, "text/html", html)
    }

    private fun serveSiteFile(siteName: String, path: String): Response {
        val site = siteManager.getSiteByName(siteName) ?: return serve404()

        // 如果是自定义路径（SAF URI），从 content provider 读取
        if (site.customPath.isNotEmpty()) {
            return serveFromCustomPath(site, path)
        }

        // 默认：从 filesDir/sites/ 读取
        var file = File(sitesDir, "$siteName/$path")
        if (!file.exists() || !file.isFile) {
            val indexFile = File(sitesDir, "$siteName/$path/index.html")
            if (indexFile.exists() && indexFile.isFile) {
                file = indexFile
            } else {
                file = File(sitesDir, "$siteName/index.html")
            }
        }

        return if (file.exists() && file.isFile) {
            val mime = getMimeType(file.name)
            try {
                val bytes = file.readBytes()
                newFixedLengthResponse(Response.Status.OK, mime, ByteArrayInputStream(bytes), bytes.size.toLong())
            } catch (e: FileNotFoundException) {
                serve404()
            }
        } else {
            serve404()
        }
    }

    /**
     * 从用户选择的 SAF 目录中提供文件服务
     */
    private fun serveFromCustomPath(site: com.siteforge.app.data.model.Site, path: String): Response {
        return try {
            val baseUri = Uri.parse(site.customPath)
            // 尝试查找文件
            val fileUri = findFileInDocumentTree(baseUri, path)
            if (fileUri != null) {
                context.contentResolver.openInputStream(fileUri)?.use { stream ->
                    val bytes = stream.readBytes()
                    val mime = getMimeType(path)
                    return newFixedLengthResponse(Response.Status.OK, mime, ByteArrayInputStream(bytes), bytes.size.toLong())
                }
            }
            // 回退到 index.html
            val indexUri = findFileInDocumentTree(baseUri, "index.html")
            if (indexUri != null) {
                context.contentResolver.openInputStream(indexUri)?.use { stream ->
                    val bytes = stream.readBytes()
                    return newFixedLengthResponse(Response.Status.OK, "text/html", ByteArrayInputStream(bytes), bytes.size.toLong())
                }
            }
            serve404()
        } catch (e: Exception) {
            Log.e(TAG, "自定义路径文件读取失败", e)
            serve404()
        }
    }

    /**
     * 在 DocumentFile 树中按路径查找文件
     */
    private fun findFileInDocumentTree(baseUri: Uri, path: String): Uri? {
        val cleanPath = path.trim('/')
        if (cleanPath.isEmpty() || cleanPath == "index.html") {
            // 尝试直接在根目录找 index.html
            return findChildDocument(baseUri, "index.html") ?: baseUri
        }

        val parts = cleanPath.split("/")
        var currentUri: Uri? = baseUri

        for ((index, part) in parts.withIndex()) {
            if (currentUri == null) return null
            currentUri = findChildDocument(currentUri, part)
        }
        return currentUri
    }

    private fun findChildDocument(parentUri: Uri, displayName: String): Uri? {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            parentUri,
            DocumentsContract.getDocumentId(parentUri)
        )
        val cursor = context.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            ),
            null, null, null
        )
        cursor?.use {
            while (it.moveToNext()) {
                val name = it.getString(1) ?: continue
                if (name.equals(displayName, ignoreCase = true)) {
                    val docId = it.getString(0)
                    return DocumentsContract.buildDocumentUriUsingTree(parentUri, docId)
                }
            }
        }
        return null
    }

    private fun handleApi(session: IHTTPSession, path: String): Response {
        val params = session.parms ?: mapOf()
        val method = session.method

        return try {
            when {
                path == "status" -> apiStatus()
                path == "sites" && method == Method.GET -> apiListSites()
                path == "sites" && method == Method.POST -> apiCreateSite(params)
                path.startsWith("sites/") && path.endsWith("/start") -> {
                    val id = extractId(path, "sites/", "/start")
                    apiStartSite(id)
                }
                path.startsWith("sites/") && path.endsWith("/stop") -> {
                    val id = extractId(path, "sites/", "/stop")
                    apiStopSite(id)
                }
                path.startsWith("sites/") && path.endsWith("/delete") -> {
                    val id = extractId(path, "sites/", "/delete")
                    apiDeleteSite(id)
                }
                else -> jsonResponse(Response.Status.NOT_FOUND, """{"error":"未知 API"}""")
            }
        } catch (e: Exception) {
            jsonResponse(Response.Status.INTERNAL_ERROR, """{"error":"${e.message}"}""")
        }
    }

    private fun apiStatus(): Response {
        val sites = siteManager.getAllSites()
        val running = sites.count { it.status == "running" }
        val json = """
            {
                "version": "1.0.0",
                "total_sites": ${sites.size},
                "running_sites": $running,
                "port": ${listeningPort}
            }
        """.trimIndent()
        return jsonResponse(Response.Status.OK, json)
    }

    private fun apiListSites(): Response {
        val sites = siteManager.getAllSites()
        val json = sites.joinToString(",", "[", "]") { site ->
            """{"id":${site.id},"name":"${site.name}","type":"${site.type}","template":"${site.template}","port":${site.port},"status":"${site.status}","description":"${site.description}"}"""
        }
        return jsonResponse(Response.Status.OK, json)
    }

    private fun apiCreateSite(params: Map<String, String>): Response {
        val name = params["name"] ?: throw IllegalArgumentException("缺少站点名称")
        if (name.length < 2 || name.length > 32 || !name.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
            throw IllegalArgumentException("站点名称仅支持字母、数字、连字符和下划线")
        }
        val type = params["type"] ?: "static"
        val template = params["template"] ?: "blank"
        val desc = params["description"] ?: ""

        val site = siteManager.createSite(name, type, template, desc)
        return jsonResponse(Response.Status.OK, siteToJson(site))
    }

    private fun apiStartSite(id: Long): Response {
        val site = siteManager.startSite(id)
            ?: return jsonResponse(Response.Status.NOT_FOUND, """{"error":"站点不存在"}""")
        return jsonResponse(Response.Status.OK, siteToJson(site))
    }

    private fun apiStopSite(id: Long): Response {
        val site = siteManager.stopSite(id)
            ?: return jsonResponse(Response.Status.NOT_FOUND, """{"error":"站点不存在"}""")
        return jsonResponse(Response.Status.OK, siteToJson(site))
    }

    private fun apiDeleteSite(id: Long): Response {
        siteManager.deleteSite(id)
        return jsonResponse(Response.Status.OK, """{"ok":true}""")
    }

    private fun extractId(path: String, prefix: String, suffix: String): Long {
        return path.removePrefix(prefix).removeSuffix(suffix).toLongOrNull()
            ?: throw IllegalArgumentException("无效的站点 ID")
    }

    private fun siteToJson(site: com.siteforge.app.data.model.Site): String {
        return """{"id":${site.id},"name":"${site.name}","type":"${site.type}","template":"${site.template}","port":${site.port},"status":"${site.status}","description":"${site.description}"}"""
    }

    private fun jsonResponse(status: Response.Status, json: String): Response {
        return newFixedLengthResponse(status, "application/json", json)
    }

    private fun serve404(): Response {
        return newFixedLengthResponse(
            Response.Status.NOT_FOUND,
            "text/html",
            "<h1>404 - 文件未找到</h1>"
        )
    }

    private fun getMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return MIME_TYPES[ext] ?: "application/octet-stream"
    }

    /**
     * 查找可用端口（自动避开已占用端口）
     */
    fun findAvailablePort(startPort: Int = DEFAULT_PORT): Int {
        for (port in startPort..(startPort + 100)) {
            try {
                ServerSocket(port).use { it.close() }
                return port
            } catch (_: Exception) {
                continue
            }
        }
        return DEFAULT_PORT
    }
}
