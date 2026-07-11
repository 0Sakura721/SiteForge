#!/usr/bin/env python3
"""
SiteForge - 一键式Web站点部署与管理平台
入口模块，负责启动管理服务器。
"""

import sys
import os

# 将项目根目录加入 Python 路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

if __name__ == "__main__":
    from siteforge.server import run
    run()
