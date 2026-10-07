# Nginx 部署

本目录代理构建后的 Vue 应用和 Spring Boot `/api`。开发模式使用 Vite 即可，Nginx 可选。

- 静态根目录 `../前端/dist`（相对本目录），或 Windows 实例里写绝对路径。
- `/api/` 转发到 `127.0.0.1:8080`，保留 `/api` 路径。
- SSE 关闭缓冲，读写超时 600 秒。
- 已提供 `conf/mime.types` 和 `logs/` 目录，**不包含 Nginx 可执行程序**。

---

## Windows 本机部署（已验证 ✅）

> ⚠️ **关键限制：Windows 版 nginx 不能使用含中文的 `-p` 前缀路径。**
> 实测 `nginx -p "F:\大学\...\星职\nginx" -c conf/nginx.conf` 直接失败：
>
> ```
> CreateFile() "...\nginx\conf/nginx.conf" failed
> (1113: No mapping for the Unicode character exists in the target multi-byte code page)
> ```
>
> 但**配置里的中文 `root` 是可以用的**（conf 文件按 UTF-8 读取，本机已实测托管成功）。
> 所以：**nginx 程序与前缀放 ASCII 路径，中文只出现在 `root` 里。**

### 本机实例位置

| 项 | 值 |
|---|---|
| nginx 程序/前缀 | `C:\xingzhi-nginx\`（nginx 1.28.0，ASCII 路径） |
| 配置文件 | `C:\xingzhi-nginx\conf\nginx.conf` |
| 日志 | `C:\xingzhi-nginx\logs\`（access.log / error.log / nginx.pid） |
| 静态根 | `F:/大学/.../星职/前端/dist`（写在 conf 的 `root` 里） |
| 监听 | `http://localhost`（80） |

`manage.py` 已适配：Windows 下自动使用该实例（可用环境变量 `NGINX_EXE` / `NGINX_PREFIX` 覆盖）。

### 启动 / 停止

```powershell
python manage.py start nginx     # 启动
python manage.py status          # 查看（nginx 以「80 端口是否监听」判定）
python manage.py stop nginx      # 停止
python manage.py restart nginx
```

手动管理（在 `C:\xingzhi-nginx` 目录下）：

```powershell
nginx.exe -t -p C:/xingzhi-nginx/ -c conf/nginx.conf      # 配置检查
nginx.exe -p C:/xingzhi-nginx/ -c conf/nginx.conf         # 启动（会自我 daemon 化）
nginx.exe -p C:/xingzhi-nginx/ -c conf/nginx.conf -s quit # 优雅停止
nginx.exe -p C:/xingzhi-nginx/ -c conf/nginx.conf -s reload
```

> 说明：Windows 上 nginx 会 daemon 化，PID 文件里是 master 而非启动器进程；
> 且 `-s quit` / `-s reload` 可能因事件跨会话或 pid 文件过期报
> `OpenEvent("Global\ngx_quit_<pid>") failed`。
> 因此 `manage.py stop nginx` 会先试 `-s quit`，再按 `nginx.pid` 与 80 端口各强杀一次兜底。

### 前置条件

1. **先在 Windows 侧构建前端**（不要在 WSL 里 build）：
   ```powershell
   python manage.py build        # 等价于 cd 前端 && npm run build（自动写 logs/build.log）
   ```
   或手动：
   ```powershell
   cd 前端
   npm ci        # 或 npm install
   npm run build # 生成 前端/dist
   ```
2. 启动后端（8080）：`python manage.py start backend`
3. 再启动 nginx，浏览器访问 `http://localhost`

> 可视化界面（`python manage.py gui`）右侧有「前端构建（npm run build）」卡片，
> 点「🔨 构建前端」即可构建，构建输出会实时回显到日志面板，卡片上会显示构建时间。

### 本机验收结果（2026-10-07）

| 检查项 | 结果 |
|---|---|
| `nginx -t` 配置检查 | ✅ syntax is ok |
| 首页 `GET /` | ✅ 200（中文 `root` 托管成功） |
| SPA 深链路 `GET /graph/xyz` | ✅ 200（`try_files` 回退到 index.html） |
| `/api/*` 反代到 8080 | ✅ 链路通（后端鉴权返回 401，非 502） |
| `Permissions-Policy: camera=(), microphone=(self), ...` | ✅ 已下发（语音输入必需） |
| `X-Frame-Options` / `X-Content-Type-Options` / `Referrer-Policy` | ✅ 已下发 |
| `Server: nginx`（版本号隐藏） | ✅ |

### 注意

- **80 端口独占**：本实例与其它项目的 nginx 实例（如 `C:\nginx-linknote`）不能同时启动，先停一个。
- 页面打开是「Nginx 已就绪，请先构建」占位页时，说明 `前端/dist` 还没构建（`npm run build` 会整体覆盖 `dist`）。
- 修改后端端口时同步改 `conf/nginx.conf` 的 `proxy_pass`。
- 当前是本机 HTTP 示例，**没有 TLS**；旧接口权限修复前不要公开部署。

---

## Linux / WSL 部署

Linux 下可以直接用仓库内本目录作为前缀（无中文前缀限制）：

```bash
nginx -t -p "$PWD/nginx/" -c conf/nginx.conf
nginx -p "$PWD/nginx/" -c conf/nginx.conf
nginx -p "$PWD/nginx/" -c conf/nginx.conf -s reload
nginx -p "$PWD/nginx/" -c conf/nginx.conf -s quit
```

> WSL 里若用 Windows 侧 nginx，同样遵守上面的「ASCII 前缀」限制；conf 里的 `root`
> 写成 `/mnt/f/.../前端/dist` 亦可（Linux 版 nginx 无中文路径问题）。
