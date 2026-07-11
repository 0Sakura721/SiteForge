"""
PHP 代理处理器
将管理面板收到的 PHP 请求转发到 PHP 内置服务器。
"""

import httpx
from .config import PHP_HOST


async def proxy_php_request(site, request):
    """
    将请求代理到 PHP 内置服务器。
    
    Args:
        site: 站点信息字典，需包含 php_port
        request: FastAPI Request 对象
    
    Returns:
        (status_code, headers_dict, body_bytes)
    """
    php_port = site.get("php_port")
    if not php_port:
        return 503, {"Content-Type": "text/html"}, "<h1>PHP 服务未启动</h1>".encode()

    target_url = f"http://{PHP_HOST}:{php_port}{request.url.path}"
    if request.url.query:
        target_url += f"?{request.url.query}"

    headers = dict(request.headers)
    headers.pop("host", None)
    headers.pop("transfer-encoding", None)

    body = await request.body()

    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            resp = await client.request(
                method=request.method,
                url=target_url,
                headers=headers,
                content=body,
                follow_redirects=False,
            )
        resp_headers = dict(resp.headers)
        resp_headers.pop("transfer-encoding", None)
        resp_headers.pop("content-encoding", None)
        return resp.status_code, resp_headers, resp.content
    except httpx.ConnectError:
        return 502, {"Content-Type": "text/html"}, "<h1>PHP 服务不可用</h1>".encode()
    except Exception as e:
        return 500, {"Content-Type": "text/html"}, f"<h1>代理错误: {str(e)}</h1>".encode()
