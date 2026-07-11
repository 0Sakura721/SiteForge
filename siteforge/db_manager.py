"""
数据库管理模块
使用 SQLite 存储站点元数据、用户信息等。
"""

import sqlite3
import os
from datetime import datetime
from contextlib import contextmanager
from .config import DB_PATH


def get_db_path():
    """确保数据库目录存在并返回路径。"""
    os.makedirs(os.path.dirname(DB_PATH), exist_ok=True)
    return DB_PATH


@contextmanager
def get_db():
    """获取数据库连接的上下文管理器，自动提交和关闭。"""
    conn = sqlite3.connect(get_db_path())
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("PRAGMA foreign_keys=ON")
    try:
        yield conn
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


def init_db():
    """初始化数据库表结构。"""
    with get_db() as db:
        db.executescript("""
            CREATE TABLE IF NOT EXISTS sites (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                name        TEXT NOT NULL UNIQUE,
                domain      TEXT NOT NULL DEFAULT '',
                type        TEXT NOT NULL DEFAULT 'static',
                template    TEXT NOT NULL DEFAULT 'blank',
                port        INTEGER NOT NULL DEFAULT 0,
                php_port    INTEGER NOT NULL DEFAULT 0,
                status      TEXT NOT NULL DEFAULT 'stopped',
                path        TEXT NOT NULL,
                description TEXT DEFAULT '',
                created_at  TEXT NOT NULL,
                updated_at  TEXT NOT NULL
            );

            CREATE TABLE IF NOT EXISTS databases (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                site_id     INTEGER NOT NULL,
                name        TEXT NOT NULL,
                path        TEXT NOT NULL,
                created_at  TEXT NOT NULL,
                FOREIGN KEY (site_id) REFERENCES sites(id) ON DELETE CASCADE
            );

            CREATE TABLE IF NOT EXISTS backups (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                site_id     INTEGER NOT NULL,
                name        TEXT NOT NULL,
                path        TEXT NOT NULL,
                size        INTEGER DEFAULT 0,
                created_at  TEXT NOT NULL,
                FOREIGN KEY (site_id) REFERENCES sites(id) ON DELETE CASCADE
            );

            CREATE TABLE IF NOT EXISTS settings (
                key   TEXT PRIMARY KEY,
                value TEXT NOT NULL
            );
        """)
        # 插入默认设置
        defaults = {
            "site_title": "SiteForge",
            "theme": "light",
            "language": "zh-CN",
            "auto_start": "false",
            "max_sites": "50",
        }
        for k, v in defaults.items():
            db.execute(
                "INSERT OR IGNORE INTO settings (key, value) VALUES (?, ?)", (k, v)
            )


# ---- 站点 CRUD ----

def create_site(name, site_type, template, port, path, description=""):
    now = datetime.now().isoformat()
    with get_db() as db:
        db.execute(
            """INSERT INTO sites (name, type, template, port, status, path, description, created_at, updated_at)
               VALUES (?, ?, ?, ?, 'stopped', ?, ?, ?, ?)""",
            (name, site_type, template, port, path, description, now, now),
        )
        return db.execute("SELECT last_insert_rowid()").fetchone()[0]


def get_all_sites():
    with get_db() as db:
        return [dict(r) for r in db.execute("SELECT * FROM sites ORDER BY id DESC").fetchall()]


def get_site(site_id):
    with get_db() as db:
        r = db.execute("SELECT * FROM sites WHERE id=?", (site_id,)).fetchone()
        return dict(r) if r else None


def get_site_by_name(name):
    with get_db() as db:
        r = db.execute("SELECT * FROM sites WHERE name=?", (name,)).fetchone()
        return dict(r) if r else None


def update_site_status(site_id, status):
    now = datetime.now().isoformat()
    with get_db() as db:
        db.execute("UPDATE sites SET status=?, updated_at=? WHERE id=?", (status, now, site_id))


def update_site(site_id, **kwargs):
    if not kwargs:
        return
    kwargs["updated_at"] = datetime.now().isoformat()
    sets = ", ".join(f"{k}=?" for k in kwargs)
    vals = list(kwargs.values()) + [site_id]
    with get_db() as db:
        db.execute(f"UPDATE sites SET {sets} WHERE id=?", vals)


def delete_site(site_id):
    with get_db() as db:
        db.execute("DELETE FROM sites WHERE id=?", (site_id,))


# ---- 数据库（站点级 SQLite）CRUD ----

def create_database(site_id, name, path):
    now = datetime.now().isoformat()
    with get_db() as db:
        db.execute(
            "INSERT INTO databases (site_id, name, path, created_at) VALUES (?, ?, ?, ?)",
            (site_id, name, path, now),
        )


def get_site_databases(site_id):
    with get_db() as db:
        return [dict(r) for r in db.execute("SELECT * FROM databases WHERE site_id=?", (site_id,)).fetchall()]


def delete_database(db_id):
    with get_db() as db:
        db.execute("DELETE FROM databases WHERE id=?", (db_id,))


# ---- 备份 CRUD ----

def create_backup(site_id, name, path, size=0):
    now = datetime.now().isoformat()
    with get_db() as db:
        db.execute(
            "INSERT INTO backups (site_id, name, path, size, created_at) VALUES (?, ?, ?, ?, ?)",
            (site_id, name, path, size, now),
        )


def get_site_backups(site_id):
    with get_db() as db:
        return [dict(r) for r in db.execute("SELECT * FROM backups WHERE site_id=? ORDER BY id DESC", (site_id,)).fetchall()]


def delete_backup(backup_id):
    with get_db() as db:
        db.execute("DELETE FROM backups WHERE id=?", (backup_id,))


# ---- 设置 ----

def get_setting(key, default=None):
    with get_db() as db:
        r = db.execute("SELECT value FROM settings WHERE key=?", (key,)).fetchone()
        return r["value"] if r else default


def set_setting(key, value):
    with get_db() as db:
        db.execute(
            "INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)", (key, value)
        )
