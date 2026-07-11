"""
文件管理模块
提供站点的文件浏览、读取、写入、上传、删除功能。
"""

import os
import shutil
import zipfile
from datetime import datetime
from .config import SITES_DIR


def _validate_path(site_path, relative_path):
    """
    路径安全校验：确保相对路径不会逃逸到站点目录之外。
    返回绝对路径。
    """
    rel = relative_path.lstrip("/")
    abs_path = os.path.realpath(os.path.join(site_path, rel))
    real_site = os.path.realpath(site_path)
    if not abs_path.startswith(real_site):
        raise PermissionError("路径越权访问被阻止")
    return abs_path


def list_files(site_path, relative_path=""):
    """列出指定目录下的文件和子目录。"""
    target = _validate_path(site_path, relative_path) if relative_path else site_path
    if not os.path.isdir(target):
        raise FileNotFoundError("目录不存在")

    items = []
    for name in sorted(os.listdir(target)):
        full = os.path.join(target, name)
        stat = os.stat(full)
        items.append({
            "name": name,
            "type": "directory" if os.path.isdir(full) else "file",
            "size": stat.st_size if os.path.isfile(full) else 0,
            "modified": datetime.fromtimestamp(stat.st_mtime).isoformat(),
            "ext": os.path.splitext(name)[1].lower() if os.path.isfile(full) else "",
        })
    return items


def read_file(site_path, relative_path):
    """读取文件内容。"""
    target = _validate_path(site_path, relative_path)
    if not os.path.isfile(target):
        raise FileNotFoundError("文件不存在")

    # 仅对文本文件返回内容
    text_exts = {
        ".html", ".htm", ".css", ".js", ".php", ".txt", ".md", ".json",
        ".xml", ".yml", ".yaml", ".ini", ".cfg", ".conf", ".log", ".csv",
        ".htaccess", ".env", ".sql", ".py", ".rb", ".sh", ".bat",
    }
    ext = os.path.splitext(target)[1].lower()
    name = os.path.basename(target)
    is_text = ext in text_exts or name.startswith(".")

    # 限制读取大小
    max_size = 5 * 1024 * 1024  # 5MB
    if os.path.getsize(target) > max_size:
        raise ValueError("文件过大，无法在线编辑（超过 5MB）")

    try:
        with open(target, "r", encoding="utf-8", errors="ignore") as f:
            content = f.read()
    except Exception:
        # 尝试二进制读取
        with open(target, "rb") as f:
            content = f.read().decode("latin-1", errors="ignore")
        is_text = False

    return {
        "content": content,
        "size": os.path.getsize(target),
        "is_text": is_text,
        "ext": ext,
        "name": name,
    }


def write_file(site_path, relative_path, content):
    """写入文件内容。"""
    target = _validate_path(site_path, relative_path)
    os.makedirs(os.path.dirname(target), exist_ok=True)
    with open(target, "w", encoding="utf-8") as f:
        f.write(content)
    return {"path": relative_path, "size": os.path.getsize(target)}


def create_directory(site_path, relative_path, dirname):
    """创建子目录。"""
    target = _validate_path(site_path, os.path.join(relative_path, dirname))
    os.makedirs(target, exist_ok=True)
    return {"path": os.path.join(relative_path, dirname)}


def create_file(site_path, relative_path, filename):
    """创建空文件。"""
    target = _validate_path(site_path, os.path.join(relative_path, filename))
    with open(target, "w", encoding="utf-8") as f:
        f.write("")
    return {"path": os.path.join(relative_path, filename)}


def delete_item(site_path, relative_path):
    """删除文件或目录。"""
    target = _validate_path(site_path, relative_path)
    if os.path.isdir(target):
        shutil.rmtree(target)
    else:
        os.remove(target)
    return {"deleted": relative_path}


def rename_item(site_path, relative_path, new_name):
    """重命名文件或目录。"""
    target = _validate_path(site_path, relative_path)
    parent = os.path.dirname(target)
    new_target = os.path.join(parent, new_name)
    os.rename(target, new_target)
    return {"old": relative_path, "new": os.path.relpath(new_target, site_path)}


def move_item(site_path, src_path, dst_path):
    """移动文件或目录。"""
    src = _validate_path(site_path, src_path)
    dst = _validate_path(site_path, dst_path)
    shutil.move(src, dst)
    return {"src": src_path, "dst": dst_path}


def copy_item(site_path, src_path, dst_path):
    """复制文件或目录。"""
    src = _validate_path(site_path, src_path)
    dst = _validate_path(site_path, dst_path)
    if os.path.isdir(src):
        shutil.copytree(src, dst, dirs_exist_ok=True)
    else:
        shutil.copy2(src, dst)
    return {"src": src_path, "dst": dst_path}


def upload_file(site_path, relative_path, file_obj):
    """上传文件到站点目录。"""
    filename = file_obj.filename or "untitled"
    target = _validate_path(site_path, os.path.join(relative_path, filename))
    os.makedirs(os.path.dirname(target), exist_ok=True)
    with open(target, "wb") as f:
        f.write(file_obj.file.read())
    return {"filename": filename, "size": os.path.getsize(target)}


def export_site_zip(site_path):
    """将整个站点打包为 ZIP 文件。"""
    zip_path = site_path + ".zip"
    with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as zf:
        for root, dirs, files in os.walk(site_path):
            for f in files:
                full = os.path.join(root, f)
                arcname = os.path.relpath(full, site_path)
                zf.write(full, arcname)
    return zip_path
