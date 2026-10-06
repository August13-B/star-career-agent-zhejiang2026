# Nginx 部署

本目录代理构建后的 Vue 应用和 Spring Boot `/api`。开发模式使用 Vite 即可，Nginx 可选。

- 静态根目录 `../前端/dist`，相对 `nginx/` 前缀目录。
- `/api/` 转发到 `127.0.0.1:8080`，保留 `/api` 路径。
- SSE 关闭缓冲，读写超时 600 秒。
- 已提供 `conf/mime.types` 和 `logs/` 目录，不包含 Nginx 可执行程序。

先在 `前端/` 执行 `npm ci`、`npm run build`，启动后端，再安装 Nginx 并加入 PATH。从仓库根目录执行：

```powershell
nginx -t -p "$PWD/nginx/" -c conf/nginx.conf
python manage.py start nginx
```

配置检查通过后访问 `http://localhost`。手动管理也应指定同一个前缀：

```powershell
nginx -p "$PWD/nginx/" -c conf/nginx.conf -s reload
nginx -p "$PWD/nginx/" -c conf/nginx.conf -s quit
```

修改后端端口时同步修改代理端口。当前是本机 HTTP 示例，没有 TLS。本轮修正了路径和缺失的 MIME 配置，未完成 Nginx 运行验收；旧接口权限修复前不要公开部署。Windows 上若 Nginx 不支持中文静态路径，请部署到 ASCII 路径并修改 `root`。
