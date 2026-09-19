#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
星职（StarCareer）服务管理器 CLI

用法:
    python manage.py start [all|backend|frontend|nginx]   启动服务
    python manage.py stop  [all|backend|frontend|nginx]   停止服务
    python manage.py restart [all|backend|frontend|nginx] 重启服务
    python manage.py status                              查看全部服务状态
    python manage.py logs [backend|frontend|nginx] [-f]  查看日志（-f 持续跟踪）
    python manage.py gui                                 打开可视化启动界面

说明:
    - 后端配置从 后端/.env 读取（复制 .env.example 为 .env 后填写）
    - 服务 PID 记录在 .pids/，日志输出到 logs/
    - 跨平台：Windows 使用 taskkill，Linux/macOS 使用 kill
"""

from __future__ import annotations

import argparse
import os
import signal
import subprocess
import sys
import time
from pathlib import Path

PROJECT_DIR = Path(__file__).resolve().parent
BACKEND_DIR = PROJECT_DIR / "后端"
FRONTEND_DIR = PROJECT_DIR / "前端"
NGINX_DIR = PROJECT_DIR / "nginx"
PID_DIR = PROJECT_DIR / ".pids"
LOG_DIR = PROJECT_DIR / "logs"
ENV_FILE = BACKEND_DIR / ".env"

IS_WINDOWS = sys.platform.startswith("win")

PID_DIR.mkdir(exist_ok=True)
LOG_DIR.mkdir(exist_ok=True)

# ── 服务定义 ──────────────────────────────────────────────────────────
def _win(cmd: list[str]) -> list[str]:
    """Windows 下 .cmd/.bat 与 npm/npx 需通过 cmd /c 执行。"""
    if not IS_WINDOWS:
        return cmd
    exe = cmd[0].lower()
    if exe.endswith((".cmd", ".bat")) or exe in ("npm", "npx", "nginx"):
        return ["cmd", "/c"] + cmd
    return cmd


SERVICES = {
    "backend": {
        "name": "后端（Spring Boot 合并工程）",
        "cwd": str(BACKEND_DIR),
        "cmd": _win(["mvnw.cmd", "spring-boot:run"] if IS_WINDOWS else ["./mvnw", "spring-boot:run"]),
        "log": LOG_DIR / "backend.log",
        "pid": PID_DIR / "backend.pid",
        "port": "8080",
    },
    "frontend": {
        "name": "前端（Vite Dev Server）",
        "cwd": str(FRONTEND_DIR),
        "cmd": _win(["npm", "run", "dev"]),
        "log": LOG_DIR / "frontend.log",
        "pid": PID_DIR / "frontend.pid",
        "port": "5173",
    },
    "nginx": {
        "name": "Nginx（反向代理，可选）",
        "cwd": str(NGINX_DIR),
        "cmd": _win(["nginx", "-p", str(NGINX_DIR), "-c", "conf/nginx.conf"]),
        "log": LOG_DIR / "nginx.log",
        "pid": PID_DIR / "nginx.pid",
        "port": "80",
    },
}

# ── .env 加载 ─────────────────────────────────────────────────────────



def _pid_on_port(port: str) -> int | None:
    """返回监听指定端口的进程 PID（Windows: netstat；Linux: ss/lsof）。"""
    try:
        if IS_WINDOWS:
            r = subprocess.run(["netstat", "-ano"], capture_output=True, text=True,
                               creationflags=subprocess.CREATE_NO_WINDOW)
            for line in r.stdout.splitlines():
                if f":{port} " in line and "LISTENING" in line:
                    return int(line.split()[-1])
        else:
            for cmd in (["ss", "-tlnp", f"sport = :{port}"], ["lsof", "-ti", f":{port}"]):
                r = subprocess.run(cmd, capture_output=True, text=True)
                if r.stdout.strip():
                    import re as _re
                    m = _re.search(r"(\d+)", r.stdout)
                    if m:
                        return int(m.group(1))
    except Exception:
        pass
    return None


def free_port(name: str) -> bool:
    """强制释放某服务端口（杀掉占用进程）。"""
    port = SERVICES[name].get("port")
    if not port:
        return False
    pid = _pid_on_port(port)
    if pid is None:
        print(f"ℹ️  端口 {port} 未被占用")
        return True
    print(f"🔫 释放端口 {port}（杀进程 PID {pid}）...")
    if IS_WINDOWS:
        _run_cmd(["taskkill", "/F", "/PID", str(pid)])
    else:
        try:
            os.kill(pid, signal.SIGKILL)
        except OSError:
            pass
    time.sleep(1)
    print("✅ 已释放" if _pid_on_port(port) is None else "⚠️ 仍被占用，请手动处理")
    return True

def _port_listening(port: str, host: str = "127.0.0.1") -> bool:
    """检测端口是否有进程监听（用于启动前端前确认后端已就绪）。"""
    import socket
    try:
        with socket.create_connection((host, int(port)), timeout=2):
            return True
    except Exception:
        return False

def load_env(path: Path) -> dict:
    """解析 .env（KEY=VALUE，忽略注释与空行，值允许含 =）。"""
    env = {}
    if not path.exists():
        return env
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, _, value = line.partition("=")
        env[key.strip()] = value.strip()
    return env


# 后端端口跟随 .env 的 SERVER_PORT
_ENV = load_env(ENV_FILE)
SERVICES_BACKEND_PORT = _ENV.get("SERVER_PORT", "8080")
SERVICES["backend"]["port"] = SERVICES_BACKEND_PORT

# ── 数据库灌库相关 ────────────────────────────────────────────────────
DB_DIR = PROJECT_DIR / "数据库"
STRUCTURE_SQL = DB_DIR / "数据库结构.sql"   # 建库建表（31 张）
DATA_SQL = DB_DIR / "数据库数据.sql"        # 业务数据（岗位/画像等）
MIGRATIONS_DIR = DB_DIR / "migrations"
# 灌库后自动执行的「幂等」迁移（可重复执行，不会丢数据）
#   005：画像表加宽加密列 + is_deleted 收敛（修复「Data too long」与「保存后查不到」）
AUTO_MIGRATIONS = [
    MIGRATIONS_DIR / "005_fix_profile_schema.sql",
]
EXPECTED_TABLES = 31


def _find_mysql() -> str | None:
    """定位 mysql 客户端：优先 PATH，否则搜常见安装目录。"""
    import glob
    import shutil
    exe = shutil.which("mysql")
    if exe:
        return exe
    for pat in (r"D:\MySQL\MySQL Server *\bin\mysql.exe",
                r"C:\Program Files\MySQL\MySQL Server *\bin\mysql.exe",
                r"C:\xampp\mysql\bin\mysql.exe",
                "/mnt/d/MySQL/MySQL Server */bin/mysql.exe",
                "/mnt/c/Program Files/MySQL/MySQL Server */bin/mysql.exe",
                "/usr/bin/mysql", "/usr/local/bin/mysql"):
        hits = sorted(glob.glob(pat), reverse=True)
        if hits:
            return hits[0]
    return None


def _db_params() -> dict:
    """从 后端/.env 解析数据库连接参数（与 application.yml 一致）。"""
    import re as _re
    env = load_env(ENV_FILE)
    url = env.get("DB_URL", "")
    m = _re.search(r"//([^:/]+)(?::(\d+))?/([^?]+)", url)
    return {
        "host": m.group(1) if m else "localhost",
        "port": m.group(2) if (m and m.group(2)) else "3306",
        "db": m.group(3) if m else "youthpath",
        "user": env.get("DB_USERNAME", "root"),
        "password": env.get("DB_PASSWORD", ""),
    }


def _mysql_base(p: dict) -> list:
    cmd = [_find_mysql(), f"-h{p['host']}", f"-P{p['port']}", f"-u{p['user']}"]
    if p["password"]:
        cmd.append(f"-p{p['password']}")
    cmd.append("--default-character-set=utf8mb4")
    return cmd


def _mysql_scalar(sql: str) -> int | None:
    """执行查询并返回首个整数值；失败返回 None。"""
    p = _db_params()
    try:
        r = subprocess.run(_mysql_base(p) + ["-N", "-B", "-e", sql],
                           capture_output=True, text=True, timeout=60)
        if r.returncode != 0:
            return None
        return int(r.stdout.strip().splitlines()[0])
    except Exception:
        return None


def _mysql_file(path: Path) -> bool:
    """导入 SQL 文件。"""
    p = _db_params()
    with open(path, "rb") as f:
        r = subprocess.run(_mysql_base(p), stdin=f, capture_output=True, text=True, timeout=1800)
    if r.returncode != 0:
        print(f"❌ 导入失败 {path.name}: {(r.stderr or '').strip()[:300]}")
        return False
    return True


def db_seed(force: bool = False) -> bool:
    """自动灌库（幂等）：缺表则建表，无业务数据则导入数据。

    - 结构：数据库/数据库结构.sql（含 CREATE DATABASE）
    - 数据：数据库/数据库数据.sql（岗位 9958 条 + 画像/能力/用户等）
    """
    if not _find_mysql():
        print("⏭  未找到 mysql 客户端，跳过自动灌库"
              "（可手动执行 数据库/数据库结构.sql 与 数据库数据.sql）")
        return False
    if not STRUCTURE_SQL.exists():
        print(f"⏭  未找到建表脚本：{STRUCTURE_SQL}")
        return False
    p = _db_params()
    db = p["db"]

    tables = _mysql_scalar(
        f"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='{db}'")
    if tables is None:
        tables = 0
    if force or tables < EXPECTED_TABLES:
        print(f"📦 初始化数据库 {db}（当前 {tables}/{EXPECTED_TABLES} 张表）...")
        if not _mysql_file(STRUCTURE_SQL):
            return False
        tables = _mysql_scalar(
            f"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='{db}'") or 0

    rows = _mysql_scalar(f"SELECT COUNT(*) FROM `{db}`.job_info") if tables else 0
    if force or not rows:
        if not DATA_SQL.exists():
            print(f"⏭  无数据脚本 {DATA_SQL.name}，仅建表完成")
            return True
        print("📦 导入业务数据（岗位/画像/能力/用户等，约 24MB）...")
        if not _mysql_file(DATA_SQL):
            return False
        rows = _mysql_scalar(f"SELECT COUNT(*) FROM `{db}`.job_info") or 0

    # 灌库后自动执行幂等迁移，避免「结构漂移」导致灌库后仍报
    # Data too long / 保存成功却查不到（见 数据库/migrations/005）
    _apply_migrations(AUTO_MIGRATIONS)

    print(f"✅ 数据库就绪：{db} | 表 {tables} 张 | job_info {rows} 条")
    return True


def _apply_migrations(files: list) -> None:
    """执行幂等迁移脚本（失败仅告警，不阻断启动）。"""
    for path in files:
        if not path.exists():
            continue
        if _mysql_file(path):
            print(f"🔧 已应用迁移：{path.name}")
        else:
            print(f"⚠️  迁移执行失败（可手动执行）：{path.name}")


def cmd_db(args):
    action = getattr(args, "db_action", "seed")
    if action == "status":
        p = _db_params()
        tables = _mysql_scalar(
            f"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='{p['db']}'")
        rows = _mysql_scalar(f"SELECT COUNT(*) FROM `{p['db']}`.job_info")
        print("═" * 46)
        print(f"  数据库: {p['user']}@{p['host']}:{p['port']}/{p['db']}")
        print(f"  表数量: {tables if tables is not None else '无法连接'}")
        print(f"  岗位数据: {rows if rows is not None else '—'} 条")
        print("═" * 46)
    elif action == "migrate":
        # 对「已有库」执行幂等迁移（加宽加密列 / 收敛 is_deleted）
        if not _find_mysql():
            print("⏭  未找到 mysql 客户端，无法执行迁移")
            return
        files = [MIGRATIONS_DIR / n for n in (
            "003_widen_encrypted_columns.sql",
            "004_fix_null_is_deleted.sql",
            "005_fix_profile_schema.sql",
        )]
        _apply_migrations(files)
    else:
        db_seed(force=getattr(args, "force", False))


# ── 进程管理 ──────────────────────────────────────────────────────────


def _run_cmd(cmd, **kw):
    if IS_WINDOWS:
        kw.setdefault("creationflags", subprocess.CREATE_NO_WINDOW)
    return subprocess.run(cmd, **kw)


def _check_java() -> None:
    """后端需 JDK 17；若当前 java 不是 17，给出提醒。"""
    try:
        kw = {"capture_output": True, "text": True}
        if IS_WINDOWS:
            kw["creationflags"] = subprocess.CREATE_NO_WINDOW
        out = subprocess.run(["java", "-version"], **kw)
        ver = (out.stderr or out.stdout).split('"')[1] if '"' in (out.stderr or out.stdout) else "?"
        if not ver.startswith("17"):
            auto = _auto_java_home()
            if auto:
                print(f"ℹ️  当前 Java = {ver}，自动改用 JDK 17：{auto}")
            else:
                print(f"⚠️  当前 Java 版本 = {ver}，项目需要 JDK 17。"
                      f"\n    请先设置：set JAVA_HOME=C:\\Program Files\\Java\\jdk-17")
    except Exception:
        print("⚠️  未检测到 java，请安装/配置 JDK 17")


def _auto_java_home() -> str | None:
    """自动定位 JDK 17：优先 JAVA_HOME，否则扫描常见安装目录。"""
    jh = os.environ.get("JAVA_HOME")
    if jh and "17" in Path(jh).name:
        return jh
    if IS_WINDOWS:
        for base in [r"C:\Program Files\Java", r"C:\Program Files\Eclipse Adoptium",
                     r"C:\Program Files\Microsoft", r"D:\Program Files\Java"]:
            p = Path(base)
            if p.exists():
                for d in sorted(p.iterdir(), reverse=True):
                    if d.is_dir() and ("jdk-17" in d.name.lower() or d.name.endswith("17")):
                        return str(d)
    return jh if jh else None


def clear_log(name: str) -> bool:
    """清空指定服务的日志文件。"""
    try:
        Path(SERVICES[name]["log"]).write_text("", encoding="utf-8")
        return True
    except OSError:
        return False


def start_service(name: str) -> bool:
    svc = SERVICES[name]
    if is_running(name):
        print(f"⏭  {svc['name']} 已在运行")
        return True
    if name == "backend":
        _check_java()
        db_seed()  # 启动前自动灌库（幂等）

    # 端口被"非本工具启动"的进程占用时给出明确提示（常见：上次的后端没关）
    _port = svc.get("port")
    if _port and _port_listening(_port) and not is_running(name):
        _pid = _pid_on_port(_port)
        print(f"❌ 端口 {_port} 已被占用（PID {_pid}），无法启动 {svc['name']}。"
              f"\n    这通常是上一次的服务未关闭；可执行："
              f"\n      python manage.py free-port {name}"
              f"\n    或 Windows：taskkill /F /PID {_pid}")
        return False

    # 每次启动前清空日志，保证本次运行日志干净可读
    log_f = open(svc["log"], "wb")

    # Nginx 未安装时优雅跳过（不阻塞一键启动）
    if name == "nginx":
        probe = _run_cmd(_win(["nginx", "-v"]), capture_output=True, text=True)
        if probe.returncode != 0:
            print("⚠️  未检测到已安装的 nginx，跳过该服务"
                  "（安装见 nginx/README.md，安装后可单独启动）")
            log_f.close()
            return False

    # 前端首次/依赖变化时自动 npm install（否则找不到 vite 或缺失模块）
    if name == "frontend":
        pkg = Path(svc["cwd"]) / "package.json"
        mods = Path(svc["cwd"]) / "node_modules"
        need = (not mods.exists()) or (pkg.exists() and pkg.stat().st_mtime > mods.stat().st_mtime)
        if need:
            print("📦 正在安装/更新前端依赖（npm install，请稍候）...")
            log_f.write(b"\n=== npm install ===\n")
            log_f.flush()
            rc = _run_cmd(_win(["npm", "install"]), cwd=svc["cwd"],
                          stdout=log_f, stderr=subprocess.STDOUT, env={**os.environ, "PYTHONUTF8": "1"})
            if rc.returncode != 0:
                print(f"❌ npm install 失败（退出码 {rc.returncode}），详见日志：{svc['log']}")
                log_f.close()
                return False
            print("✅ 前端依赖安装完成")

    # 启动前端前确认后端已就绪（否则 Vite 代理会 ECONNREFUSED，页面报错却查不到原因）
    if name == "frontend" and not _port_listening(SERVICES["backend"]["port"]):
        print(f"⚠️  后端（端口 {SERVICES['backend']['port']}）尚未就绪！"
              f"\n    前端虽然能启动，但所有 /api 请求都会失败（ECONNREFUSED）。"
              f"\n    建议先启动后端：python manage.py start backend")

    print(f"🚀 启动 {svc['name']} ...")
    env = {**os.environ, "PYTHONUTF8": "1"}
    if name == "backend":
        env.update(load_env(ENV_FILE))  # 注入 .env 配置
        jh = _auto_java_home()
        if jh:
            env["JAVA_HOME"] = jh
            env["PATH"] = str(Path(jh) / "bin") + os.pathsep + env.get("PATH", "")
    try:
        kw = dict(cwd=svc["cwd"], stdout=log_f, stderr=subprocess.STDOUT)
        if IS_WINDOWS:
            kw["creationflags"] = subprocess.CREATE_NEW_PROCESS_GROUP | subprocess.CREATE_NO_WINDOW
        proc = subprocess.Popen(svc["cmd"], env=env, **kw)
    except FileNotFoundError as e:
        print(f"❌ 启动失败：{e}（请确认 {svc['cwd']} 环境就绪" +
              ("；Nginx 未安装可跳过该服务）" if name == "nginx" else "）"))
        log_f.close()
        return False
    svc["pid"].write_text(str(proc.pid), encoding="utf-8")

    # 存活校验：等几秒确认进程仍在（避免“报成功但已崩溃”）
    settle = {"backend": 6, "frontend": 4, "nginx": 2}.get(name, 3)
    time.sleep(settle)
    if not _pid_alive(proc.pid):
        svc["pid"].unlink(missing_ok=True)
        tail = ""
        try:
            tail = Path(svc["log"]).read_text(encoding="utf-8", errors="replace")[-800:]
        except OSError:
            pass
        print(f"❌ {svc['name']} 启动后立即退出，日志末尾：\n{tail}")
        return False

    print(f"✅ {svc['name']} 已启动 (PID {proc.pid})，日志：{svc['log']}")
    return True


def _read_pid(name: str) -> int | None:
    pid_f = SERVICES[name]["pid"]
    if pid_f.exists():
        try:
            return int(pid_f.read_text(encoding="utf-8").strip())
        except ValueError:
            return None
    return None


def _pid_alive(pid: int) -> bool:
    if IS_WINDOWS:
        r = subprocess.run(["tasklist", "/FI", f"PID eq {pid}"], capture_output=True, text=True,
                           creationflags=subprocess.CREATE_NO_WINDOW)
        return str(pid) in r.stdout
    try:
        os.kill(pid, 0)
        return True
    except OSError:
        return False


def is_running(name: str) -> bool:
    pid = _read_pid(name)
    return pid is not None and _pid_alive(pid)


def stop_service(name: str) -> bool:
    svc = SERVICES[name]
    pid = _read_pid(name)
    if pid is None:
        print(f"ℹ️  {svc['name']} 无 PID 记录")
    elif _pid_alive(pid):
        print(f"🛑 停止 {svc['name']} (PID {pid}) ...")
        if IS_WINDOWS:
            _run_cmd(["taskkill", "/F", "/T", "/PID", str(pid)])
        else:
            try:
                os.kill(pid, signal.SIGTERM)
            except OSError:
                pass
        time.sleep(1)
        if _pid_alive(pid):
            if not IS_WINDOWS:
                try:
                    os.kill(pid, signal.SIGKILL)
                except OSError:
                    pass
        print("✅ 已停止")
    else:
        print(f"ℹ️  {svc['name']} 未在运行（PID 残留已清理）")
    svc["pid"].unlink(missing_ok=True)
    return True


# ── 命令实现 ──────────────────────────────────────────────────────────


def cmd_start(args):
    targets = ["backend", "frontend", "nginx"] if args.all or args.service == "all" else [args.service]
    for t in targets:
        start_service(t)


def cmd_stop(args):
    targets = ["backend", "frontend", "nginx"] if args.all or args.service == "all" else [args.service]
    for t in reversed(targets):
        stop_service(t)


def cmd_restart(args):
    targets = ["backend", "frontend", "nginx"] if args.all or args.service == "all" else [args.service]
    for t in reversed(targets):
        stop_service(t)
    for t in targets:
        start_service(t)


def cmd_status(_args):
    print("═" * 46)
    print("  星职服务状态")
    print("═" * 46)
    for name, svc in SERVICES.items():
        pid = _read_pid(name)
        alive = pid is not None and _pid_alive(pid)
        mark = "● 运行中" if alive else "○ 已停止"
        pid_str = f"PID {pid}" if alive else "—"
        print(f"  {mark}  {svc['name']:<22} 端口 {svc['port']:<6} {pid_str}")
    print("═" * 46)


def cmd_logs(args):
    svc = SERVICES.get(args.service)
    if not svc:
        print("服务不存在")
        return
    log_f = svc["log"]
    if not log_f.exists():
        print(f"暂无日志：{log_f}")
        return
    if args.follow:
        import subprocess as sp
        try:
            sp.run(["tail", "-f", str(log_f)])
        except FileNotFoundError:
            # Windows 无 tail
            with open(log_f, encoding="utf-8", errors="replace") as f:
                print(f.read())
    else:
        text = log_f.read_text(encoding="utf-8", errors="replace")
        print(text[-3000:]) if len(text) > 3000 else print(text)


def main():
    parser = argparse.ArgumentParser(description="星职服务管理器")
    sub = parser.add_subparsers(dest="command")

    p_start = sub.add_parser("start", help="启动服务")
    p_start.add_argument("service", nargs="?", default="all", choices=["all", "backend", "frontend", "nginx"])
    p_start.add_argument("--all", action="store_true", help="启动全部")

    p_stop = sub.add_parser("stop", help="停止服务")
    p_stop.add_argument("service", nargs="?", default="all", choices=["all", "backend", "frontend", "nginx"])
    p_stop.add_argument("--all", action="store_true")

    p_restart = sub.add_parser("restart", help="重启服务")
    p_restart.add_argument("service", nargs="?", default="all", choices=["all", "backend", "frontend", "nginx"])
    p_restart.add_argument("--all", action="store_true")

    sub.add_parser("status", help="查看状态")
    p_logs = sub.add_parser("logs", help="查看日志")
    p_logs.add_argument("service", choices=["backend", "frontend", "nginx"])
    p_logs.add_argument("-f", "--follow", action="store_true", help="持续跟踪")

    sub.add_parser("gui", help="打开可视化界面")

    p_fp = sub.add_parser("free-port", help="强制释放某服务端口（杀掉占用进程）")
    p_fp.add_argument("service", choices=["backend", "frontend", "nginx"])

    p_db = sub.add_parser("db", help="数据库灌库 / 查看状态 / 执行迁移")
    p_db.add_argument("db_action", nargs="?", default="seed",
                      choices=["seed", "status", "migrate"])
    p_db.add_argument("--force", action="store_true", help="强制重建表并重新导入数据")

    args = parser.parse_args()
    if not args.command:
        parser.print_help()
        return

    if args.command == "start":
        cmd_start(args)
    elif args.command == "stop":
        cmd_stop(args)
    elif args.command == "restart":
        cmd_restart(args)
    elif args.command == "status":
        cmd_status(args)
    elif args.command == "logs":
        cmd_logs(args)
    elif args.command == "gui":
        from manage_gui import run_gui
        run_gui()
    elif args.command == "db":
        cmd_db(args)
    elif args.command == "free-port":
        free_port(args.service)


if __name__ == "__main__":
    main()
