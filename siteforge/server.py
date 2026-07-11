"""
SiteForge 主服务器模块
FastAPI 应用，整合管理 API 与站点代理服务。
"""

import os
import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, Request, UploadFile, File, Form
from fastapi.responses import (
    HTMLResponse, FileResponse, JSONResponse, PlainTextResponse, StreamingResponse, Response,
)
from fastapi.staticfiles import StaticFiles
from fastapi.middleware.cors import CORSMiddleware
import uvicorn

from .config import (
    ADMIN_HOST, ADMIN_PORT, WEB_DIR, SITES_DIR, DATA_DIR,
    SITE_PORT_START, SITE_PORT_END, PHP_HOST,
)
from . import db_manager as db
from . import site_manager as sm
from . import file_manager as fm
from . import template_engine as te
from .php_handler import proxy_php_request

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("siteforge")


# ---- 生命周期 ----

@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用启动和关闭时的处理。"""
    db.init_db()
    logger.info("SiteForge 数据库初始化完成")

    # 自动启动标记为 running 的站点
    sites = db.get_all_sites()
    auto_start = db.get_setting("auto_start", "false") == "true"
    for site in sites:
        if auto_start and site["status"] == "running":
            try:
                sm.start_site(site["id"])
                logger.info(f"自动启动站点: {site['name']}")
            except Exception as e:
                logger.warning(f"自动启动站点失败 {site['name']}: {e}")

    yield

    # 关闭时清理
    sm.cleanup_all()
    logger.info("SiteForge 已停止")


# ---- 应用创建 ----

app = FastAPI(
    title="SiteForge",
    description="一键式Web站点部署与管理平台",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# 挂载静态文件目录
app.mount("/static", StaticFiles(directory=WEB_DIR), name="static")


# ---- 管理面板 ----

@app.get("/", response_class=HTMLResponse)
async def admin_panel():
    """返回管理面板首页。"""
    index_path = os.path.join(WEB_DIR, "index.html")
    if os.path.exists(index_path):
        with open(index_path, "r", encoding="utf-8") as f:
            return f.read()
    return "<h1>SiteForge 管理面板 - 请安装前端文件</h1>"


# ---- 站点管理 API ----

@app.get("/api/sites")
async def api_list_sites():
    return db.get_all_sites()


@app.get("/api/sites/{site_id}")
async def api_get_site(site_id: int):
    site = db.get_site(site_id)
    if not site:
        raise HTTPException(404, "站点不存在")
    return site


@app.post("/api/sites")
async def api_create_site(request: Request):
    data = await request.json()
    try:
        site = sm.create_site(
            name=data.get("name", ""),
            site_type=data.get("type", "static"),
            template=data.get("template", "blank"),
            description=data.get("description", ""),
        )
        return site
    except ValueError as e:
        raise HTTPException(400, str(e))
    except Exception as e:
        raise HTTPException(500, str(e))


@app.post("/api/sites/{site_id}/start")
async def api_start_site(site_id: int):
    try:
        return sm.start_site(site_id)
    except ValueError as e:
        raise HTTPException(400, str(e))


@app.post("/api/sites/{site_id}/stop")
async def api_stop_site(site_id: int):
    try:
        return sm.stop_site(site_id)
    except ValueError as e:
        raise HTTPException(404, str(e))


@app.post("/api/sites/{site_id}/restart")
async def api_restart_site(site_id: int):
    try:
        return sm.restart_site(site_id)
    except ValueError as e:
        raise HTTPException(400, str(e))


@app.put("/api/sites/{site_id}")
async def api_update_site(site_id: int, request: Request):
    data = await request.json()
    allowed = {"name", "domain", "description"}
    updates = {k: v for k, v in data.items() if k in allowed and v is not None}
    if not updates:
        raise HTTPException(400, "无有效更新字段")
    try:
        db.update_site(site_id, **updates)
        return db.get_site(site_id)
    except Exception as e:
        raise HTTPException(500, str(e))


@app.delete("/api/sites/{site_id}")
async def api_delete_site(site_id: int):
    try:
        sm.delete_site(site_id)
        return {"ok": True}
    except ValueError as e:
        raise HTTPException(404, str(e))


@app.get("/api/sites/{site_id}/logs")
async def api_site_logs(site_id: int, lines: int = 50):
    return {"logs": sm.get_site_logs(site_id, lines)}


@app.get("/api/sites/{site_id}/export")
async def api_export_site(site_id: int):
    site = db.get_site(site_id)
    if not site:
        raise HTTPException(404, "站点不存在")
    zip_path = fm.export_site_zip(site["path"])
    return FileResponse(zip_path, filename=f"{site['name']}.zip", media_type="application/zip")


# ---- 文件管理 API ----

@app.get("/api/sites/{site_id}/files")
async def api_list_files(site_id: int, path: str = ""):
    site = db.get_site(site_id)
    if not site:
        raise HTTPException(404, "站点不存在")
    try:
        return fm.list_files(site["path"], path)
    except Exception as e:
        raise HTTPException(400, str(e))


@app.get("/api/sites/{site_id}/files/read")
async def api_read_file(site_id: int, path: str):
    site = db.get_site(site_id)
    if not site:
        raise HTTPException(404, "站点不存在")
    try:
        return fm.read_file(site["path"], path)
    except Exception as e:
        raise HTTPException(400, str(e))


@app.post("/api/sites/{site_id}/files/write")
async def api_write_file(site_id: int, request: Request):
    site = db.get_site(site_id)
    if not site:
        raise HTTPException(404, "站点不存在")
    data = await request.json()
    try:
        return fm.write_file(site["path"], data["path"], data["content"])
    except Exception as e:
        raise HTTPException(400, str(e))


@app.post("/api/sites/{site_id}/files/mkdir")
async def api_create_dir(site_id: int, request: Request):
    site = db.get_site(site_id)
    if not site:
        raise HTTPException(404, "站点不存在")
    data = await request.json()
    try:
        return fm.create_directory(site["path"], data.get("path", ""), data["name"])
    except Exception as e:
        raise HTTPException(400, str(e))


@app.post("/api/sites/{site_id}/files/create")
async def api_create_file(site_id: int, request: Request):
    site = db.get_site(site_id)
    if not site:
        raise HTTPException(404, "站点不存在")
    data = await request.json()
    try:
        return fm.create_file(site["path"], data.get("path", ""), data["name"])
    except Exception as e:
        raise HTTPException(400, str(e))


@app.delete("/api/sites/{site_id}/files")
async def api_delete_item(site_id: int, path: str):
    site = db.get_site(site_id)
    if not site:
        raise HTTPException(404, "站点不存在")
    try:
        return fm.delete_item(site["path"], path)
    except Exception as e:
        raise HTTPException(400, str(e))


@app.put("/api/sites/{site_id}/files/rename")
async def api_rename_item(site_id: int, request: Request):
    site = db.get_site(site_id)
    if not site:
        raise HTTPException(404, "站点不存在")
    data = await request.json()
    try:
        return fm.rename_item(site["path"], data["path"], data["new_name"])
    except Exception as e:
        raise HTTPException(400, str(e))


@app.post("/api/sites/{site_id}/files/upload")
async def api_upload_file(site_id: int, path: str = "", file: UploadFile = File(...)):
    site = db.get_site(site_id)
    if not site:
        raise HTTPException(404, "站点不存在")
    try:
        return fm.upload_file(site["path"], path, file)
    except Exception as e:
        raise HTTPException(400, str(e))


# ---- 模板 API ----

@app.get("/api/templates")
async def api_list_templates():
    return te.list_templates()


# ---- 数据库管理 API ----

@app.get("/api/sites/{site_id}/databases")
async def api_list_databases(site_id: int):
    return db.get_site_databases(site_id)


@app.post("/api/sites/{site_id}/databases")
async def api_create_database(site_id: int, request: Request):
    site = db.get_site(site_id)
    if not site:
        raise HTTPException(404, "站点不存在")
    data = await request.json()
    name = data.get("name", "database")
    db_dir = os.path.join(site["path"], "data")
    os.makedirs(db_dir, exist_ok=True)
    db_path = os.path.join(db_dir, f"{name}.db")
    # 创建 SQLite 数据库文件
    import sqlite3
    conn = sqlite3.connect(db_path)
    conn.close()
    db.create_database(site_id, name, db_path)
    return {"name": name, "path": db_path}


@app.delete("/api/sites/{site_id}/databases/{db_id}")
async def api_delete_database(site_id: int, db_id: int):
    dbs = db.get_site_databases(site_id)
    target = next((d for d in dbs if d["id"] == db_id), None)
    if target and os.path.exists(target["path"]):
        os.remove(target["path"])
    db.delete_database(db_id)
    return {"ok": True}


# ---- 备份 API ----

@app.get("/api/sites/{site_id}/backups")
async def api_list_backups(site_id: int):
    return db.get_site_backups(site_id)


@app.post("/api/sites/{site_id}/backups")
async def api_create_backup(site_id: int):
    site = db.get_site(site_id)
    if not site:
        raise HTTPException(404, "站点不存在")
    zip_path = fm.export_site_zip(site["path"])
    size = os.path.getsize(zip_path)
    name = f"backup_{site['name']}_{__import__('datetime').datetime.now().strftime('%Y%m%d_%H%M%S')}.zip"
    db.create_backup(site_id, name, zip_path, size)
    return {"name": name, "size": size}


@app.delete("/api/sites/{site_id}/backups/{backup_id}")
async def api_delete_backup(site_id: int, backup_id: int):
    backups = db.get_site_backups(site_id)
    target = next((b for b in backups if b["id"] == backup_id), None)
    if target and os.path.exists(target["path"]):
        os.remove(target["path"])
    db.delete_backup(backup_id)
    return {"ok": True}


# ---- 设置 API ----

@app.get("/api/settings")
async def api_get_settings():
    settings = {}
    for k in ["site_title", "theme", "language", "auto_start", "max_sites"]:
        settings[k] = db.get_setting(k, "")
    return settings


@app.put("/api/settings")
async def api_update_settings(request: Request):
    data = await request.json()
    for k, v in data.items():
        db.set_setting(k, str(v))
    return {"ok": True}


# ---- 系统状态 API ----

@app.get("/api/status")
async def api_status():
    import platform, sys
    sites = db.get_all_sites()
    running = sum(1 for s in sites if s["status"] == "running")
    return {
        "version": "1.0.0",
        "platform": platform.system(),
        "python": sys.version,
        "total_sites": len(sites),
        "running_sites": running,
        "port_range": f"{SITE_PORT_START}-{SITE_PORT_END}",
    }


# ---- 站点代理：将外部请求转发到对应站点 ----

@app.api_route("/s/{site_name}", methods=["GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"])
async def proxy_site_root(site_name: str, request: Request):
    """站点代理路由（根路径）。"""
    return await _proxy_site_request(site_name, "", request)


@app.api_route("/s/{site_name}/{rest_of_path:path}", methods=["GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"])
async def proxy_site(site_name: str, rest_of_path: str, request: Request):
    """站点代理路由（子路径）。"""
    return await _proxy_site_request(site_name, rest_of_path, request)


async def _proxy_site_request(site_name: str, rest_of_path: str, request: Request):
    """
    站点代理路由：
    所有 /s/{站点名}/... 的请求将被代理到对应的站点。
    - 静态站点：直接返回文件
    - PHP 站点：转发到 PHP 内置服务器
    """
    site = db.get_site_by_name(site_name)
    if not site:
        raise HTTPException(404, f"站点 '{site_name}' 不存在")

    if site["status"] != "running":
        return HTMLResponse(
            "<h1 style='text-align:center;margin-top:100px;font-family:sans-serif;color:#999'>站点未启动</h1>",
            status_code=503,
        )

    # PHP 站点：代理到 PHP 内置服务器
    if site["type"] == "php":
        status, headers, body = await proxy_php_request(site, request)
        return Response(content=body, status_code=status, headers=headers)

    # 静态站点：直接提供文件
    file_path = rest_of_path or "index.html"
    abs_path = os.path.join(site["path"], file_path)
    if os.path.isfile(abs_path):
        return FileResponse(abs_path)
    # 检查 index.html
    index_path = os.path.join(site["path"], file_path, "index.html")
    if os.path.isdir(abs_path) and os.path.isfile(index_path):
        return FileResponse(index_path)
    # 尝试 index.html 回退
    abs_index = os.path.join(site["path"], "index.html")
    if os.path.isfile(abs_index):
        return FileResponse(abs_index)

    raise HTTPException(404, "文件未找到")


def run():
    """启动 SiteForge 服务器。"""
    logger.info(f"SiteForge v1.0.0 启动中...")
    logger.info(f"管理面板: http://{ADMIN_HOST}:{ADMIN_PORT}")
    logger.info(f"站点代理: http://{ADMIN_HOST}:{ADMIN_PORT}/s/站点名/")
    uvicorn.run(app, host=ADMIN_HOST, port=ADMIN_PORT, log_level="info")
