package com.siteforge.app.server

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build

/**
 * 管理面板 HTML 生成器
 * 为 Web 访问提供内嵌的管理面板界面。
 */
object AdminPanelGenerator {

    fun generate(context: Context, siteManager: SiteManager): String {
        val sites = siteManager.getAllSites()
        val running = sites.count { it.status == "running" }
        val ip = getDeviceIp(context)
        val port = 8080

        return """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>SiteForge - 管理面板</title>
<style>
${styles()}
</style>
</head>
<body>
<div class="app">
    <aside class="sidebar">
        <div class="logo">
            <svg width="28" height="28" viewBox="0 0 32 32"><rect width="32" height="32" rx="8" fill="url(#g)"/><circle cx="16" cy="16" r="8" stroke="#fff" stroke-width="1.5" fill="none"/><defs><linearGradient id="g" x1="0" y1="0" x2="32" y2="32"><stop offset="0%" stop-color="#667eea"/><stop offset="100%" stop-color="#764ba2"/></linearGradient></defs></svg>
            <span>SiteForge</span>
        </div>
        <nav>
            <a class="nav-item active" data-tab="dashboard" onclick="switchTab('dashboard')">📊 控制台</a>
            <a class="nav-item" data-tab="sites" onclick="switchTab('sites')">🌐 站点</a>
            <a class="nav-item" data-tab="create" onclick="switchTab('create')">➕ 创建</a>
        </nav>
        <div class="sidebar-footer">
            <div class="status-indicator" id="statusDot"></div>
            <span>v1.0 · Android</span>
        </div>
    </aside>
    <main>
        <!-- 控制台 -->
        <div class="tab active" id="tab-dashboard">
            <h1>控制台</h1>
            <div class="stats">
                <div class="stat-card"><span class="stat-num">${sites.size}</span><span class="stat-label">站点总数</span></div>
                <div class="stat-card"><span class="stat-num">$running</span><span class="stat-label">运行中</span></div>
                <div class="stat-card"><span class="stat-num">${sites.size - running}</span><span class="stat-label">已停止</span></div>
            </div>
            <div class="card tutorial-box">
                <h2>📖 快速教程</h2>
                <div class="tutorial-steps">
                    <div class="t-step"><span>1</span> 在 App 中启动服务器</div>
                    <div class="t-step"><span>2</span> 创建站点或导入 HTML 文件夹</div>
                    <div class="t-step"><span>3</span> 在浏览器访问：<code>http://$ip:$port/站点名/</code></div>
                </div>
            </div>
            <div class="card">
                <h2>访问信息</h2>
                <div class="info-row"><span>设备 IP</span><strong>$ip</strong></div>
                <div class="info-row"><span>服务器端口</span><strong>$port</strong></div>
                <div class="info-row"><span>系统版本</span><strong>Android ${Build.VERSION.SDK_INT}</strong></div>
            </div>
        </div>

        <!-- 站点列表 -->
        <div class="tab" id="tab-sites">
            <h1>站点管理</h1>
            ${generateSitesTable(sites)}
        </div>

        <!-- 创建站点 -->
        <div class="tab" id="tab-create">
            <h1>创建站点</h1>
            <form class="card" onsubmit="createSite(event)">
                <div class="form-group">
                    <label>站点名称 *</label>
                    <input type="text" id="siteName" required placeholder="my-website" pattern="[a-zA-Z0-9_-]+">
                    <small>仅支持字母、数字、连字符和下划线</small>
                </div>
                <div class="form-group">
                    <label>站点类型 *</label>
                    <select id="siteType">
                        <option value="static">静态站点 (HTML/CSS/JS)</option>
                        <option value="single-page">单页应用 (SPA)</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>描述</label>
                    <textarea id="siteDesc" rows="2" placeholder="简要描述此站点的用途"></textarea>
                </div>
                <button type="submit" class="btn btn-primary">🚀 创建站点</button>
            </form>
        </div>
    </main>
</div>
<div class="toast" id="toast"></div>
<script>
${javascript(sites)}
</script>
</body>
</html>
""".trimIndent()
    }

    private fun generateSitesTable(sites: List<com.siteforge.app.data.model.Site>): String {
        if (sites.isEmpty()) {
            return """<div class="card empty"><p>📭 还没有任何站点</p><button class="btn btn-primary" onclick="switchTab('create')">创建第一个站点</button></div>"""
        }
        val rows = sites.joinToString("\n") { site ->
            val badge = if (site.status == "running") "badge-running" else "badge-stopped"
            val statusText = if (site.status == "running") "运行中" else "已停止"
            val btnAction = if (site.status == "stopped") {
                """<button class="btn btn-sm btn-start" onclick="startSite(${site.id})">▶ 启动</button>"""
            } else {
                """<button class="btn btn-sm btn-stop" onclick="stopSite(${site.id})">⏹ 停止</button>"""
            }
            """
            <tr>
                <td><strong>${site.name}</strong>${if (site.description.isNotEmpty()) "<br><small>${site.description}</small>" else ""}</td>
                <td>${if (site.type == "static") "静态" else "SPA"}</td>
                <td><code>${site.port}</code></td>
                <td><span class="badge $badge">$statusText</span></td>
                <td>
                    $btnAction
                    <a class="btn btn-sm btn-view" href="/${site.name}/" target="_blank">🔗 访问</a>
                    <button class="btn btn-sm btn-danger" onclick="deleteSite(${site.id}, '${site.name}')">🗑 删除</button>
                </td>
            </tr>
            """
        }
        return """<div class="card"><table><thead><tr><th>名称</th><th>类型</th><th>端口</th><th>状态</th><th>操作</th></tr></thead><tbody>$rows</tbody></table></div>"""
    }

    private fun javascript(sites: List<com.siteforge.app.data.model.Site>): String {
        return """
function switchTab(name) {
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
    document.querySelector('[data-tab="' + name + '"]').classList.add('active');
    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    document.getElementById('tab-' + name).classList.add('active');
}

function toast(msg) {
    var t = document.getElementById('toast');
    t.textContent = msg;
    t.classList.add('show');
    setTimeout(() => t.classList.remove('show'), 2500);
}

function api(method, path, body) {
    return fetch('/api/' + path, {
        method: method,
        headers: body ? {'Content-Type': 'application/json'} : {},
        body: body ? JSON.stringify(body) : null
    }).then(r => r.json());
}

function createSite(e) {
    e.preventDefault();
    var name = document.getElementById('siteName').value.trim();
    var type = document.getElementById('siteType').value;
    var desc = document.getElementById('siteDesc').value.trim();
    api('POST', 'sites', {name: name, type: type, description: desc})
        .then(() => { toast('站点创建成功！'); setTimeout(() => location.reload(), 800); })
        .catch(err => toast('创建失败: ' + err.message));
}

function startSite(id) {
    api('POST', 'sites/' + id + '/start').then(() => location.reload());
}
function stopSite(id) {
    api('POST', 'sites/' + id + '/stop').then(() => location.reload());
}
function deleteSite(id, name) {
    if (confirm('确定删除站点 "' + name + '" 吗？此操作不可恢复。')) {
        api('POST', 'sites/' + id + '/delete').then(() => location.reload());
    }
}
""".trimIndent()
    }

    private fun styles(): String {
        return """
* { margin:0; padding:0; box-sizing:border-box; }
body { font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,"PingFang SC","Microsoft YaHei",sans-serif; background:#f0f2f5; color:#333; }
.app { display:flex; min-height:100vh; }
.sidebar { width:220px; background:#1a1d29; color:#a0a7b8; display:flex; flex-direction:column; position:fixed; top:0; left:0; bottom:0; }
.sidebar .logo { display:flex; align-items:center; gap:0.5rem; padding:1.25rem; border-bottom:1px solid rgba(255,255,255,0.06); }
.sidebar .logo span { color:#fff; font-weight:700; font-size:1.05rem; }
.sidebar nav { flex:1; padding:0.75rem; display:flex; flex-direction:column; gap:2px; }
.nav-item { display:flex; align-items:center; gap:0.5rem; padding:0.65rem 0.75rem; border-radius:8px; color:#a0a7b8; text-decoration:none; cursor:pointer; font-size:0.875rem; transition:all 0.15s; border:none; background:none; width:100%; text-align:left; }
.nav-item:hover { background:#252a3a; color:#d0d5e0; }
.nav-item.active { background:#667eea; color:#fff; }
.sidebar-footer { padding:1rem 1.25rem; border-top:1px solid rgba(255,255,255,0.06); font-size:0.8rem; display:flex; align-items:center; gap:0.5rem; }
.status-indicator { width:8px; height:8px; border-radius:50%; background:#4caf50; }
main { margin-left:220px; flex:1; padding:2rem; max-width:1000px; }
h1 { font-size:1.6rem; margin-bottom:1.5rem; }
.stats { display:grid; grid-template-columns:repeat(3,1fr); gap:1rem; margin-bottom:1.5rem; }
.stat-card { background:#fff; border-radius:12px; padding:1.25rem; text-align:center; box-shadow:0 1px 3px rgba(0,0,0,0.06); }
.stat-num { display:block; font-size:2rem; font-weight:700; color:#667eea; }
.stat-label { font-size:0.8rem; color:#888; margin-top:0.25rem; }
.card { background:#fff; border-radius:12px; padding:1.5rem; box-shadow:0 1px 3px rgba(0,0,0,0.06); margin-bottom:1rem; }
.card h2 { font-size:1.05rem; margin-bottom:1rem; }
.card.empty { text-align:center; padding:3rem; color:#999; }
.tutorial-box { background:linear-gradient(135deg,#eef0fd,#f3e5f5); border:1px solid #d4c5f9; }
.tutorial-steps { display:flex; flex-direction:column; gap:10px; }
.t-step { display:flex; align-items:center; gap:10px; font-size:0.875rem; color:#333; }
.t-step span { display:inline-flex; align-items:center; justify-content:center; width:24px; height:24px; border-radius:50%; background:#667eea; color:#fff; font-size:0.75rem; font-weight:700; flex-shrink:0; }
.t-step code { font-size:0.78rem; background:rgba(255,255,255,0.8); }
.info-row { display:flex; justify-content:space-between; padding:0.5rem 0; border-bottom:1px solid #f0f0f0; }
.info-row:last-child { border:none; }
.info-row span { color:#888; font-size:0.875rem; }
table { width:100%; border-collapse:collapse; }
th { text-align:left; padding:0.75rem 0.5rem; font-size:0.78rem; color:#888; text-transform:uppercase; border-bottom:1px solid #eee; }
td { padding:0.75rem 0.5rem; border-bottom:1px solid #eee; font-size:0.85rem; }
.badge { display:inline-block; padding:2px 10px; border-radius:100px; font-size:0.73rem; font-weight:600; }
.badge-running { background:#e8f5e9; color:#2e7d32; }
.badge-stopped { background:#fce4ec; color:#c62828; }
.btn { display:inline-flex; align-items:center; gap:0.3rem; padding:0.5rem 1rem; border-radius:8px; border:none; font-size:0.85rem; cursor:pointer; transition:0.15s; text-decoration:none; }
.btn-primary { background:#667eea; color:#fff; }
.btn-primary:hover { background:#5a6fd6; }
.btn-sm { padding:0.3rem 0.6rem; font-size:0.78rem; }
.btn-start { background:#e8f5e9; color:#2e7d32; }
.btn-stop { background:#fff3e0; color:#e65100; }
.btn-view { background:#e3f2fd; color:#1565c0; }
.btn-danger { background:#fce4ec; color:#c62828; }
.form-group { margin-bottom:1rem; }
.form-group label { display:block; font-size:0.85rem; font-weight:500; margin-bottom:0.3rem; }
.form-group input,.form-group select,.form-group textarea { width:100%; padding:0.6rem 0.75rem; border:1px solid #ddd; border-radius:8px; font-size:0.9rem; font-family:inherit; transition:border-color 0.15s; }
.form-group input:focus,.form-group select:focus,.form-group textarea:focus { outline:none; border-color:#667eea; }
.form-group small { display:block; color:#999; font-size:0.75rem; margin-top:0.2rem; }
.toast { position:fixed; top:1rem; right:1rem; background:#333; color:#fff; padding:0.75rem 1.25rem; border-radius:8px; font-size:0.85rem; opacity:0; transform:translateX(100%); transition:all 0.3s; z-index:9999; }
.toast.show { opacity:1; transform:translateX(0); }
code { background:#f0f0f0; padding:2px 6px; border-radius:4px; font-size:0.85em; }
@media (max-width:768px) { .sidebar { width:50px; } .sidebar .logo span,.sidebar nav a span,.sidebar-footer span { display:none; } .nav-item { justify-content:center; } main { margin-left:50px; padding:1rem; } .stats { grid-template-columns:1fr; } }
""".trimIndent()
    }

    private fun getDeviceIp(context: Context): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val ip = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (ip != 0) {
                String.format(
                    "%d.%d.%d.%d",
                    ip and 0xff,
                    ip shr 8 and 0xff,
                    ip shr 16 and 0xff,
                    ip shr 24 and 0xff
                )
            } else "127.0.0.1"
        } catch (e: Exception) {
            "127.0.0.1"
        }
    }
}
