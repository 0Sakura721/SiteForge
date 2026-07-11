"""
模板引擎模块
管理站点模板的部署、自定义及模板变量替换。
"""

import os
import json
from .config import TEMPLATES_DIR


# 内置模板描述
BUILTIN_TEMPLATES = {
    "blog": {
        "name": "个人博客",
        "description": "清新简洁的博客模板，适合写作和分享",
        "type": "static",
        "preview": "紫色渐变风格，含文章列表与关于页面",
    },
    "business": {
        "name": "企业官网",
        "description": "专业大气的企业展示模板，适合公司官网",
        "type": "static",
        "preview": "蓝色商务风格，含产品展示和联系方式",
    },
    "portfolio": {
        "name": "作品集",
        "description": "个人作品展示模板，适合设计师和开发者",
        "type": "static",
        "preview": "极简黑白风格，网格布局展示作品",
    },
    "blank": {
        "name": "空白站点",
        "description": "从零开始构建你的网站",
        "type": "static",
        "preview": "无预设内容，完全自定义",
    },
    "php-basic": {
        "name": "PHP 基础站点",
        "description": "包含 PHP 运行环境的空白站点",
        "type": "php",
        "preview": "PHP 环境就绪，可运行动态网站",
    },
}


def list_templates():
    """获取所有可用模板列表（内置 + 自定义）。"""
    templates = dict(BUILTIN_TEMPLATES)
    # 扫描自定义模板目录
    if os.path.isdir(TEMPLATES_DIR):
        for name in os.listdir(TEMPLATES_DIR):
            tpl_dir = os.path.join(TEMPLATES_DIR, name)
            if os.path.isdir(tpl_dir) and name not in templates:
                config_file = os.path.join(tpl_dir, "template.json")
                if os.path.exists(config_file):
                    with open(config_file, "r", encoding="utf-8") as f:
                        cfg = json.load(f)
                    templates[name] = {
                        "name": cfg.get("name", name),
                        "description": cfg.get("description", ""),
                        "type": cfg.get("type", "static"),
                        "preview": cfg.get("preview", ""),
                        "custom": True,
                    }
    return templates


def get_template_info(name):
    """获取单个模板的详细信息。"""
    templates = list_templates()
    return templates.get(name)
