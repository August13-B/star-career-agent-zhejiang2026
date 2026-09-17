#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
星职（StarCareer）可视化服务管理器（Tkinter）

用法:
    python manage.py gui

功能:
    - 一键启动 / 一键停止 / 一键重启（后端 + 前端 + Nginx）
    - 单个服务 启动 / 停止 / 重启
    - 状态显示：运行状态 + PID + 启动时间（重启后时间变化，一眼可辨）
    - 操作过程中显示「处理中」过渡态 + 实时进度输出
    - 日志查看 / 清空当前日志 / 清空全部日志

端口约定:
    后端  http://localhost:8080   （context-path /api，可在 后端/.env 改 SERVER_PORT）
    前端  http://localhost:5173   （Vite dev server）
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

# ── 主题色 ────────────────────────────────────────────────────────────
BG_DARK = "#1e1e2e"
BG_CARD = "#2a2a3e"
BG_INPUT = "#35354a"
FG_PRIMARY = "#cdd6f4"
FG_SECONDARY = "#a6adc8"
ACCENT = "#89b4fa"
ACCENT_OK = "#a6e3a1"
ACCENT_WARN = "#f9e2af"
ACCENT_ERR = "#f38ba8"

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
        self.root.geometry("1060x700")
        self.root.minsize(840, 580)
        self.root.configure(bg=BG_DARK)

        import manage as M
        self.M = M

        self.service_names = ["backend", "frontend", "nginx"]
        self.status_vars: dict[str, tk.Label] = {}
        self.time_vars: dict[str, tk.Label] = {}
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

        btns = tk.Frame(header, bg=BG_DARK)
        btns.pack(side="left", padx=24)
        self._btn(btns, "⚡ 一键启动", ACCENT_OK, lambda: self._run_all("start"), bold=True).pack(side="left", padx=3)
        self._btn(btns, "■ 全部停止", ACCENT_ERR, lambda: self._run_all("stop")).pack(side="left", padx=3)
        self._btn(btns, "↻ 全部重启", ACCENT_WARN, lambda: self._run_all("restart")).pack(side="left", padx=3)
        self._btn(btns, "🗑 清空全部日志", FG_SECONDARY, self._clear_all_logs).pack(side="left", padx=12)

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
            tk.Label(card, text=f"端口 {svc['port']}  ·  {URLS[name]}", bg=BG_CARD,
                     fg=ACCENT, font=("Consolas", 8)).pack(anchor="w", pady=(1, 4))

            self.status_vars[name] = tk.Label(card, text="○ 已停止", bg=BG_CARD,
                                              fg=FG_SECONDARY, font=("Consolas", 10, "bold"))
            self.status_vars[name].pack(anchor="w")
            self.time_vars[name] = tk.Label(card, text="—", bg=BG_CARD,
                                            fg=FG_SECONDARY, font=("Consolas", 8))
            self.time_vars[name].pack(anchor="w", pady=(1, 8))

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
        self._btn(bar, "🗑 清空当前日志", ACCENT_ERR, self._clear_log).pack(side="left", padx=4)

        self.log_view = scrolledtext.ScrolledText(frame, height=15, bg=BG_INPUT, fg=FG_PRIMARY,
                                                  font=("Consolas", 9), insertbackground=FG_PRIMARY,
                                                  wrap="word")
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

    def _refresh_log(self, name: str | None = None) -> None:
        name = name or self.log_var.get()
        log_f = self.M.SERVICES.get(name, {}).get("log")
        try:
            text = Path(log_f).read_text(encoding="utf-8", errors="replace")[-12000:] if log_f else "(无)"
        except (FileNotFoundError, AttributeError):
            text = "(暂无日志)"
        if not text.strip():
            text = f"({name} 暂无日志)"
        self.log_view.configure(state="normal")
        self.log_view.delete("1.0", "end")
        self.log_view.insert("1.0", text)
        self.log_view.configure(state="disabled")
        self.log_view.see("end")

    def _clear_log(self) -> None:
        name = self.log_var.get()
        self.M.clear_log(name)
        self.log_view.configure(state="normal")
        self.log_view.delete("1.0", "end")
        self.log_view.configure(state="disabled")
        self.status_bar.config(text=f"已清空 {name} 日志  {datetime.now():%H:%M:%S}")
        self._append_log(f"[{datetime.now():%H:%M:%S}] 已清空 {name} 日志文件")

    def _clear_all_logs(self) -> None:
        for n in self.service_names:
            self.M.clear_log(n)
        self.log_view.configure(state="normal")
        self.log_view.delete("1.0", "end")
        self.log_view.configure(state="disabled")
        self.status_bar.config(text=f"已清空全部日志  {datetime.now():%H:%M:%S}")
        self._append_log(f"[{datetime.now():%H:%M:%S}] 已清空全部服务日志")

    # ── 操作逻辑 ──────────────────────────────────────────────────────

    def _run_one(self, action: str, name: str) -> None:
        self._dispatch(action, [name])

    def _run_all(self, action: str) -> None:
        self._dispatch(action, list(self.service_names))

    def _dispatch(self, action: str, names: list[str]) -> None:
        if self._busy:
            self.status_bar.config(text="⚠️ 上一个操作还在进行中，请稍候…")
            return
        self._busy = True
        self.status_bar.config(text=f"⟳ {action} 中… {datetime.now():%H:%M:%S}")

        # 立刻显示过渡态，让用户看到动作已生效
        for n in names:
            self.status_vars[n].config(text="⟳ 处理中…", fg=ACCENT_WARN)

        def worker():
            for n in names:
                buf = io.StringIO()
                try:
                    with contextlib.redirect_stdout(buf):
                        if action in ("stop", "restart"):
                            self.M.stop_service(n)
                        if action in ("start", "restart"):
                            self.M.start_service(n)
                except Exception as e:  # noqa: BLE001
                    buf.write(f"❌ {n} 执行出错: {e}\n")
                text = buf.getvalue()
                # 每完成一个服务就刷新状态与日志，进度可见
                self.root.after(0, lambda t=text: self._append_log(t))
                self.root.after(0, self._refresh_status)
                self.root.after(0, lambda nn=n: (self.log_var.set(nn), self._refresh_log(nn)))
            self.root.after(0, self._done)

        threading.Thread(target=worker, daemon=True).start()

    def _done(self) -> None:
        self._busy = False
        self._refresh_status()
        self.status_bar.config(text=f"✅ 操作完成  {datetime.now():%H:%M:%S}")

    def _started_at(self, name: str) -> str | None:
        """以 PID 文件的修改时间作为启动时间（重启后会变化，便于确认）。"""
        pid_f = self.M.SERVICES[name]["pid"]
        try:
            return datetime.fromtimestamp(Path(pid_f).stat().st_mtime).strftime("%H:%M:%S")
        except OSError:
            return None

    def _refresh_status(self) -> None:
        for name in self.service_names:
            try:
                alive = self.M.is_running(name)
            except Exception:  # noqa: BLE001
                alive = False
            if alive:
                pid = self.M._read_pid(name)
                started = self._started_at(name) or "—"
                self.status_vars[name].config(text=f"● 运行中  PID {pid}", fg=ACCENT_OK)
                self.time_vars[name].config(text=f"启动于 {started}", fg=FG_SECONDARY)
            else:
                self.status_vars[name].config(text="○ 已停止", fg=ACCENT_ERR)
                self.time_vars[name].config(text="—", fg=FG_SECONDARY)
        if not self._busy:
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
