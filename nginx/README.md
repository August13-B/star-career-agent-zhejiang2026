# Nginx（星职部署用）

`nginx/conf/nginx.conf` 提供生产环境反向代理：

- **前端静态文件**：`../frontend/dist`（需先 `npm run build`）
- **API 反代**：`/api/*` → `http://127.0.0.1:8080`（合并后端，context-path 为 `/api`，SSE 已关闭缓冲）

## 安装 nginx（二选一）

**Windows**（PowerShell，需管理员）：
```powershell
winget install nginx
# 或从 https://nginx.org/en/download.html 下载 Windows 版解压
```

**WSL / Linux**：
```bash
sudo apt update && sudo apt install -y nginx
```

## 使用

通过 `manage.py` 一键管理：
```bash
python manage.py start nginx      # 启动（自动使用 nginx/conf/nginx.conf）
python manage.py status           # 查看状态
```

或手动：
```bash
# 进入 nginx 目录后
nginx -p ./ -c conf/nginx.conf    # 启动
nginx -s stop                     # 停止
nginx -s reload                   # 重载配置
```

> ⚠️ 注意：
> 1. `mime.types` 需随 nginx 安装包提供（本仓库只含 `conf/nginx.conf`）
> 2. 若 nginx 不在 PATH，请在 `manage.py` 的 `SERVICES["nginx"]["cmd"]` 中改为 nginx 可执行文件完整路径
> 3. Windows 下端口 80 可能被占用，可自行修改 `listen` 端口
