"""
SiteForge 全局配置模块
管理所有路径、端口、默认设置等可调参数。
"""

import os
import json

# 项目根目录
ROOT_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# 数据目录
DATA_DIR = os.path.join(ROOT_DIR, "data")
SITES_DIR = os.path.join(ROOT_DIR, "sites")
TEMPLATES_DIR = os.path.join(ROOT_DIR, "templates")
WEB_DIR = os.path.join(ROOT_DIR, "web")

# 数据库文件
DB_PATH = os.path.join(DATA_DIR, "siteforge.db")

# 管理面板配置
ADMIN_HOST = "0.0.0.0"
ADMIN_PORT = 8080

# 站点端口范围
SITE_PORT_START = 8081
SITE_PORT_END = 8199

# PHP 配置
PHP_HOST = "127.0.0.1"
PHP_PORT_START = 9000
PHP_PORT_END = 9100

# 默认站点模板
DEFAULT_TEMPLATE = "blog"

# 支持的站点类型
SITE_TYPES = ["static", "php", "single-page"]

# 确保必要目录存在
for d in [DATA_DIR, SITES_DIR, TEMPLATES_DIR]:
    os.makedirs(d, exist_ok=True)

# 配置文件路径
CONFIG_FILE = os.path.join(DATA_DIR, "config.json")


def load_config():
    """从磁盘加载持久化配置，若不存在则返回默认值。"""
    if os.path.exists(CONFIG_FILE):
        with open(CONFIG_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    return {}


def save_config(cfg: dict):
    """将配置字典持久化到磁盘。"""
    os.makedirs(DATA_DIR, exist_ok=True)
    with open(CONFIG_FILE, "w", encoding="utf-8") as f:
        json.dump(cfg, f, indent=2, ensure_ascii=False)
