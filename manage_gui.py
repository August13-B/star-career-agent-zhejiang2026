#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
星职（StarCareer）可视化服务管理器（Tkinter）

用法:
    python manage.py gui

功能:
    - 一键启动 / 一键停止 / 一键重启（后端 + 前端 + Nginx）
    - 单个服务 启动 / 停止 / 重启
    - 实时状态 + 端口显示
    - 日志查看 + 清空日志

端口约定:
    后端  http://localhost:8080   （context-path /api，可在 后端/.env 改 SERVER_PORT）
    前端  http://localhost:5173   （Vite dev server，可在 前端/vite.config.js 改）
    Nginx http://localhost        （可选，生产反代）
"""

from __future__ import annotations

import contextlib
import io
import threading
from datetime import datetime
from pathlib import Path
import tkinter as tk
from tkinter import scrolledtext

# ── 主题色（PolyPlexII 同款暗色）──────────────────────────────────────
BG_DARK = "#1e1e2e"
BG_CARD = "#2a2a3e"
BG_INPUT = "#35354a"
FG_PRIMARY = "#cdd6f4"
FG_SECONDARY = "#a6adc8"
ACCENT = "#89b4fa"
ACCENT_OK = "#a6e3a1"
ACCENT_WARN = "#f9e2af"
ACCENT_ERR = "#f38ba8"

# ── 端口 / 地址（与 manage.SERVICES 保持一致）──
URLS = {
    "backend": "http://localhost:8080/api",
    "frontend": "http://localhost:5173",
    "nginx": "http://localhost",
}


class ManageGUI:
    """星职服务管理主界面"""

    def __init__(self, root: tk.Tk) -> None:
        self.root = root
        self.root.title("星职 · 服务管理器")
        self.root.geometry("1040x680")
        self.root.minsize(820, 560)
        self.root.configure(bg=BG_DARK)

        import manage as M  # 复用 manage.py 逻辑
        self.M = M

        self.service_names = ["backend", "frontend", "nginx"]
        self.status_vars: dict[str, tk.Label] = {}
        self._busy = False

        self._setup_header()
        self._setup_cards()
        self._setup_logs()
        self._refresh_status()
        self.root.after(3000, self._auto_refresh)

    # ── 界面构建 ──────────────────────────────────────────────────────

    def _setup_header(self) -> None:
        header = tk.Frame(self.root, bg=BG_DARK)
        header.pack(fill="x", padx=20, pady=(14, 6))

        tk.Label(header, text="✨ 星职 · 服务管理器", bg=BG_DARK, fg=ACCENT,
                 font=("Microsoft YaHei", 17, "bold")).pack(side="left")

        # 一键操作按钮
        btns = tk.Frame(header, bg=BG_DARK)
        btns.pack(side="left", padx=24)
        self._btn(btns, "⚡ 一键启动", ACCENT_OK, lambda: self._run_all("start"), bold=True).pack(side="left", padx=3)
        self._btn(btns, "■ 全部停止", ACCENT_ERR, lambda: self._run_all("stop")).pack(side="left", padx=3)
        self._btn(btns, "↻ 全部重启", ACCENT_WARN, lambda: self._run_all("restart")).pack(side="left", padx=3)

        self.status_bar = tk.Label(header, text="", bg=BG_DARK, fg=FG_SECONDARY,
                                   font=("Microsoft YaHei", 9))
        self.status_bar.pack(side="right")

    def _setup_cards(self) -> None:
        cards = tk.Frame(self.root, bg=BG_DARK)
        cards.pack(fill="x", padx=20, pady=6)
        for i, name in enumerate(self.service_names):
            cards.columnconfigure(i, weight=1)
            card = tk.Frame(cards, bg=BG_CARD, padx=12, pady=10,
                            highlightthickness=1, highlightbackground=BG_INPUT)
            card.grid(row=0, column=i, sticky="nsew", padx=6)

            svc = self.M.SERVICES[name]
            tk.Label(card, text=svc["name"], bg=BG_CARD, fg=FG_PRIMARY,
                     font=("Microsoft YaHei", 10, "bold")).pack(anchor="w")

            # 端口 / 访问地址
            tk.Label(card, text=f"端口 {svc['port']}  ·  {URLS[name]}", bg=BG_CARD,
                     fg=ACCENT, font=("Consolas", 8)).pack(anchor="w", pady=(1, 3))

            self.status_vars[name] = tk.Label(card, text="○ 已停止", bg=BG_CARD,
                                              fg=FG_SECONDARY, font=("Consolas", 10))
            self.status_vars[name].pack(anchor="w", pady=(0, 8))

            btns = tk.Frame(card, bg=BG_CARD)
            btns.pack(fill="x")
            self._btn(btns, "▶ 启动", ACCENT_OK, lambda n=name: self._run_one("start", n)).pack(side="left", expand=True, padx=2)
            self._btn(btns, "■ 停止", ACCENT_ERR, lambda n=name: self._run_one("stop", n)).pack(side="left", expand=True, padx=2)
            self._btn(btns, "↻ 重启", ACCENT_WARN, lambda n=name: self._run_one("restart", n)).pack(side="left", expand=True, padx=2)

    def _setup_logs(self) -> None:
        frame = tk.Frame(self.root, bg=BG_DARK)
        frame.pack(fill="both", expand=True, padx=20, pady=(2, 10))

        bar = tk.Frame(frame, bg=BG_DARK)
        bar.pack(fill="x")
        tk.Label(bar, text="📜 日志", bg=BG_DARK, fg=FG_PRIMARY,
                 font=("Microsoft YaHei", 11, "bold")).pack(side="left")

        self.log_var = tk.StringVar(value="backend")
        self.log_sel = tk.OptionMenu(bar, self.log_var, *self.service_names)
        self.log_sel.configure(bg=BG_CARD, fg=FG_PRIMARY, activebackground=BG_INPUT,
                               highlightthickness=0, font=("Consolas", 9))
        self.log_sel.pack(side="left", padx=8)

        self._btn(bar, "⟳ 刷新日志", ACCENT, self._refresh_log).pack(side="left", padx=4)
        self._btn(bar, "🗑 清空日志", ACCENT_ERR, self._clear_log).pack(side="left", padx=4)

        self.log_view = scrolledtext.ScrolledText(frame, height=15, bg=BG_INPUT, fg=FG_PRIMARY,
                                                  font=("Consolas", 9), insertbackground=FG_PRIMARY)
        self.log_view.pack(fill="both", expand=True, pady=(6, 0))
        self.log_view.configure(state="disabled")

    def _btn(self, parent, text, color, cmd, bold=False):
        return tk.Button(parent, text=text, command=cmd, bg=BG_CARD, fg=color,
                         activebackground=BG_INPUT, activeforeground=color,
                         relief="flat", padx=8, pady=2,
                         font=("Microsoft YaHei", 9, "bold" if bold else "normal"))

    # ── 日志面板 ──────────────────────────────────────────────────────

    def _append_log(self, text: str) -> None:
        if not text:
            return
        self.log_view.configure(state="normal")
        self.log_view.insert("end", text if text.endswith("\n") else text + "\n")
        self.log_view.see("end")
        self.log_view.configure(state="disabled")

    def _refresh_log(self) -> None:
        name = self.log_var.get()
        log_f = self.M.SERVICES.get(name, {}).get("log")
        try:
            text = Path(log_f).read_text(encoding="utf-8", errors="replace")[-10000:] if log_f else "(无)"
        except FileNotFoundError:
            text = "(暂无日志)"
        self.log_view.configure(state="normal")
        self.log_view.delete("1.0", "end")
        self.log_view.insert("1.0", text)
        self.log_view.configure(state="disabled")
        self.log_view.see("end")
        self.status_bar.config(text=f"已刷新 {name} 日志 {datetime.now():%H:%M:%S}")

    def _clear_log(self) -> None:
        """清空当前服务的日志文件 + 面板。"""
        name = self.log_var.get()
        log_f = self.M.SERVICES.get(name, {}).get("log")
        if log_f:
            Path(log_f).write_text("", encoding="utf-8")
        self.log_view.configure(state="normal")
        self.log_view.delete("1.0", "end")
        self.log_view.configure(state="disabled")
        self.status_bar.config(text=f"已清空 {name} 日志")
        self._append_log(f"[{datetime.now():%H:%M:%S}] 已清空 {name} 日志文件")

    # ── 操作逻辑 ──────────────────────────────────────────────────────

    def _run_one(self, action: str, name: str) -> None:
        self._dispatch(action, [name])

    def _run_all(self, action: str) -> None:
        # 一键启动/重启默认只启核心（后端+前端），Nginx 需另行安装且可选
        self._dispatch(action, ["backend", "frontend"])

    def _dispatch(self, action: str, names: list[str]) -> None:
        if self._busy:
            self._append_log("⚠️ 上一个操作还在进行中，请稍候…")
            return
        self._busy = True

        def worker():
            buf = io.StringIO()
            try:
                with contextlib.redirect_stdout(buf):
                    if action == "restart":
                        for n in reversed(names):
                            self.M.stop_service(n)
                        for n in names:
                            self.M.start_service(n)
                    elif action == "stop":
                        for n in reversed(names):
                            self.M.stop_service(n)
                    else:
                        for n in names:
                            self.M.start_service(n)
            except Exception as e:  # noqa: BLE001
                buf.write(f"❌ 执行出错: {e}\n")
            text = buf.getvalue()
            self.root.after(0, lambda: self._append_log(text))
            self.root.after(0, self._refresh_status)
            self.root.after(0, lambda: setattr(self, "_busy", False))

        threading.Thread(target=worker, daemon=True).start()

    def _refresh_status(self) -> None:
        for name in self.service_names:
            try:
                alive = self.M.is_running(name)
            except Exception:
                alive = False
            self.status_vars[name].config(
                text=f"● 运行中 · PID {self.M._read_pid(name)}" if alive else "○ 已停止",
                fg=ACCENT_OK if alive else FG_SECONDARY)
        self.status_bar.config(text=f"状态已刷新 {datetime.now():%H:%M:%S}")

    def _auto_refresh(self) -> None:
        if not self._busy:
            self._refresh_status()
        self.root.after(5000, self._auto_refresh)


def run_gui() -> None:
    root = tk.Tk()
    ManageGUI(root)
    root.mainloop()


if __name__ == "__main__":
    run_gui()
