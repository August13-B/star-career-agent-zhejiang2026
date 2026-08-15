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
SERVICES = {
    "backend": {
        "name": "后端（Spring Boot 合并工程）",
        "cwd": str(BACKEND_DIR),
        "cmd": ["mvnw.cmd", "spring-boot:run"] if IS_WINDOWS else ["./mvnw", "spring-boot:run"],
        "log": LOG_DIR / "backend.log",
        "pid": PID_DIR / "backend.pid",
        "port": "8080",
    },
    "frontend": {
        "name": "前端（Vite Dev Server）",
        "cwd": str(FRONTEND_DIR),
        "cmd": ["npm", "run", "dev"],
        "log": LOG_DIR / "frontend.log",
        "pid": PID_DIR / "frontend.pid",
        "port": "5173",
    },
    "nginx": {
        "name": "Nginx（反向代理，可选）",
        "cwd": str(NGINX_DIR),
        "cmd": ["nginx", "-p", str(NGINX_DIR), "-c", "conf/nginx.conf"],
        "log": LOG_DIR / "nginx.log",
        "pid": PID_DIR / "nginx.pid",
        "port": "80",
    },
}

# ── .env 加载 ─────────────────────────────────────────────────────────


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


# ── 进程管理 ──────────────────────────────────────────────────────────


def _run_cmd(cmd, **kw):
    if IS_WINDOWS:
        kw.setdefault("creationflags", subprocess.CREATE_NO_WINDOW)
    return subprocess.run(cmd, **kw)


def start_service(name: str) -> bool:
    svc = SERVICES[name]
    if is_running(name):
        print(f"⏭  {svc['name']} 已在运行")
        return True
    print(f"🚀 启动 {svc['name']} ...")
    env = {**os.environ, "PYTHONUTF8": "1"}
    if name == "backend":
        env.update(load_env(ENV_FILE))  # 注入 .env 配置
    log_f = open(svc["log"], "ab")
    try:
        kw = dict(cwd=svc["cwd"], stdout=log_f, stderr=subprocess.STDOUT)
        if IS_WINDOWS:
            kw["creationflags"] = subprocess.CREATE_NEW_PROCESS_GROUP | subprocess.CREATE_NO_WINDOW
        proc = subprocess.Popen(svc["cmd"], env=env, **kw)
    except FileNotFoundError as e:
        print(f"❌ 启动失败：{e}（请确认 {svc['cwd']} 环境就绪）")
        return False
    svc["pid"].write_text(str(proc.pid), encoding="utf-8")
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


if __name__ == "__main__":
    main()
