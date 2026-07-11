"""
站点生命周期管理模块
负责站点的创建、启动、停止、删除等核心操作。
"""

import os
import shutil
import subprocess
import signal
import time
from .config import SITES_DIR, TEMPLATES_DIR, SITE_PORT_START, SITE_PORT_END, PHP_PORT_START, PHP_PORT_END
from . import db_manager as db

# 运行中的站点进程字典: {site_id: {"php": process, "web": process}}
_running_sites = {}


def _find_free_port(start, end, used_ports=None):
    """在给定的端口范围内查找未被占用的端口。"""
    import socket
    if used_ports is None:
        used_ports = set()
    for port in range(start, end + 1):
        if port in used_ports:
            continue
        with socket.socket(socket.AF_INET, socket.SO_REUSEADDR) as s:
            try:
                s.bind(("", port))
                return port
            except OSError:
                continue
    raise RuntimeError(f"端口范围 {start}-{end} 内无可用端口")


def _get_used_ports():
    """获取所有已分配站点的端口。"""
    sites = db.get_all_sites()
    ports = set()
    for s in sites:
        if s["port"]:
            ports.add(s["port"])
        if s["php_port"]:
            ports.add(s["php_port"])
    return ports


def get_site_path(site_name):
    """获取站点文件系统路径。"""
    return os.path.join(SITES_DIR, site_name)


def create_site(name, site_type="static", template="blank", description=""):
    """
    创建新站点：
    1. 校验名称合法性
    2. 分配端口
    3. 创建目录结构
    4. 根据模板填充初始文件
    5. 写入数据库
    """
    # 校验名称
    if not name or not name.replace("-", "").replace("_", "").isalnum():
        raise ValueError("站点名称只能包含字母、数字、连字符和下划线")
    if db.get_site_by_name(name):
        raise ValueError(f"站点 '{name}' 已存在")

    used_ports = _get_used_ports()
    port = _find_free_port(SITE_PORT_START, SITE_PORT_END, used_ports)
    php_port = _find_free_port(PHP_PORT_START, PHP_PORT_END, used_ports | {port})

    site_path = get_site_path(name)
    os.makedirs(site_path, exist_ok=True)

    # 复制模板文件
    _deploy_template(site_path, site_type, template)

    site_id = db.create_site(name, site_type, template, port, site_path, description)

    return db.get_site(site_id)


def _deploy_template(site_path, site_type, template):
    """将模板文件部署到站点目录。"""
    template_path = os.path.join(TEMPLATES_DIR, template)
    if os.path.isdir(template_path):
        # 复制模板中的所有文件
        for item in os.listdir(template_path):
            src = os.path.join(template_path, item)
            dst = os.path.join(site_path, item)
            if os.path.isfile(src):
                shutil.copy2(src, dst)
            elif os.path.isdir(src):
                shutil.copytree(src, dst, dirs_exist_ok=True)
    else:
        # 无模板则生成默认站点
        _generate_default_site(site_path, site_type)


def _generate_default_site(site_path, site_type):
    """为站点生成默认首页与基础文件。"""
    index_file = os.path.join(site_path, "index.html" if site_type != "php" else "index.php")
    css_dir = os.path.join(site_path, "css")
    os.makedirs(css_dir, exist_ok=True)

    html_content = """<!DOCTYPE html>
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
            <p>该站点由 SiteForge 自动生成</p>
        </header>
        <main>
            <section class="card">
                <h2>开始构建你的网站</h2>
                <p>编辑 <code>index.html</code> 文件来修改此页面内容。</p>
                <p>你可以通过 SiteForge 管理面板的文件管理器在线编辑文件。</p>
            </section>
        </main>
        <footer>
            <p>&copy; 2026 SiteForge - 自动建站平台</p>
        </footer>
    </div>
</body>
</html>"""

    php_content = """<?php
$pageTitle = "欢迎来到我的站点";
$currentTime = date("Y-m-d H:i:s");
?>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?php echo $pageTitle; ?></title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
    <div class="container">
        <header>
            <h1>🚀 站点搭建成功！</h1>
            <p>该站点由 SiteForge 自动生成 - PHP 环境运行正常</p>
        </header>
        <main>
            <section class="card">
                <h2>PHP 运行环境已就绪</h2>
                <p>当前服务器时间：<strong><?php echo $currentTime; ?></strong></p>
                <p>PHP 版本：<strong><?php echo phpversion(); ?></strong></p>
                <p>编辑 <code>index.php</code> 来修改此页面。</p>
            </section>
        </main>
        <footer>
            <p>&copy; <?php echo date("Y"); ?> SiteForge - 自动建站平台</p>
        </footer>
    </div>
</body>
</html>"""

    css_content = """* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); min-height: 100vh; display: flex; align-items: center; justify-content: center; }
.container { max-width: 720px; margin: 2rem; }
header { text-align: center; color: #fff; margin-bottom: 2rem; }
header h1 { font-size: 2.5rem; margin-bottom: 0.5rem; }
header p { font-size: 1.1rem; opacity: 0.9; }
.card { background: #fff; border-radius: 16px; padding: 2rem; box-shadow: 0 20px 60px rgba(0,0,0,0.15); }
.card h2 { color: #333; margin-bottom: 1rem; font-size: 1.4rem; }
.card p { color: #555; line-height: 1.8; margin-bottom: 0.5rem; }
.card code { background: #f0f0f0; padding: 2px 8px; border-radius: 4px; font-family: "SF Mono", Monaco, monospace; font-size: 0.9em; color: #764ba2; }
footer { text-align: center; color: rgba(255,255,255,0.7); margin-top: 2rem; font-size: 0.9rem; }"""

    with open(index_file, "w", encoding="utf-8") as f:
        f.write(php_content if site_type == "php" else html_content)
    with open(os.path.join(css_dir, "style.css"), "w", encoding="utf-8") as f:
        f.write(css_content)


def start_site(site_id):
    """
    启动站点服务：
    - 静态站点：由管理服务器代理提供
    - PHP 站点：启动 PHP 内置服务器作为后端
    """
    site = db.get_site(site_id)
    if not site:
        raise ValueError("站点不存在")
    if site["status"] == "running":
        return site

    site_path = site["path"]
    if not os.path.isdir(site_path):
        raise ValueError("站点目录不存在")

    # 如果是 PHP 站点，启动 PHP 内置服务器
    try:
        if site["type"] == "php":
            _start_php_server(site)
    except FileNotFoundError:
        # 如果系统没有 PHP，降级为静态站点
        db.update_site(site_id, type="static")
        site["type"] = "static"

    db.update_site_status(site_id, "running")
    return db.get_site(site_id)


def _start_php_server(site):
    """启动 PHP 内置开发服务器。"""
    php_path = shutil.which("php")
    if not php_path:
        raise FileNotFoundError("系统中未检测到 PHP，请安装 PHP 后再启用 PHP 站点")

    docroot = os.path.join(site["path"])
    router = os.path.join(docroot, "router.php")
    cmd = [php_path, "-S", f"127.0.0.1:{site['php_port']}", "-t", docroot]
    if os.path.exists(router):
        cmd.append(router)

    proc = subprocess.Popen(
        cmd,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
        preexec_fn=os.setsid,
    )
    _running_sites.setdefault(site["id"], {})["php"] = proc
    time.sleep(0.3)


def stop_site(site_id):
    """停止站点服务并清理进程。"""
    site = db.get_site(site_id)
    if not site:
        raise ValueError("站点不存在")

    _kill_site_processes(site_id)
    db.update_site_status(site_id, "stopped")
    return db.get_site(site_id)


def restart_site(site_id):
    stop_site(site_id)
    return start_site(site_id)


def delete_site(site_id):
    """删除站点：先停止服务，再删除文件和数据库记录。"""
    site = db.get_site(site_id)
    if not site:
        raise ValueError("站点不存在")

    stop_site(site_id)
    if os.path.isdir(site["path"]):
        shutil.rmtree(site["path"])
    db.delete_site(site_id)


def _kill_site_processes(site_id):
    """终止与指定站点关联的所有子进程。"""
    procs = _running_sites.pop(site_id, {})
    for key, proc in procs.items():
        try:
            os.killpg(os.getpgid(proc.pid), signal.SIGTERM)
            proc.wait(timeout=3)
        except Exception:
            try:
                os.killpg(os.getpgid(proc.pid), signal.SIGKILL)
            except Exception:
                pass


def get_site_logs(site_id, lines=50):
    """获取站点访问日志（预留）。"""
    site = db.get_site(site_id)
    if not site:
        return []
    log_file = os.path.join(site["path"], "access.log")
    if not os.path.exists(log_file):
        return ["暂无日志"]
    with open(log_file, "r", encoding="utf-8", errors="ignore") as f:
        return f.readlines()[-lines:]


def cleanup_all():
    """程序退出时清理所有运行中的站点进程。"""
    for site_id in list(_running_sites.keys()):
        _kill_site_processes(site_id)
