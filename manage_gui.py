#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
星职（StarCareer）可视化服务管理器（Tkinter）

用法:
    python manage.py gui

提供：启动 / 停止 / 重启 / 状态 / 日志查看 的可视化界面。
"""

from __future__ import annotations

import threading
import tkinter as tk
from tkinter import scrolledtext, ttk

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


class ManageGUI:
    """星职服务管理主界面"""

    def __init__(self, root: tk.Tk) -> None:
        self.root = root
        self.root.title("星职 · 服务管理器")
        self.root.geometry("980x640")
        self.root.minsize(760, 520)
        self.root.configure(bg=BG_DARK)

        import manage as M  # 复用 manage.py 逻辑

        self.M = M

        # 服务状态缓存
        self.service_names = ["backend", "frontend", "nginx"]
        self.status_vars = {}

        self._setup_header()
        self._setup_cards()
        self._setup_logs()
        self._refresh_status()
        self.root.after(3000, self._auto_refresh)

    # ── 构建 ──────────────────────────────────────────────────────────

    def _setup_header(self) -> None:
        header = tk.Frame(self.root, bg=BG_DARK)
        header.pack(fill="x", padx=20, pady=(16, 8))
        title = tk.Label(header, text="✨ 星职 · 服务管理器",
                         bg=BG_DARK, fg=ACCENT, font=("Microsoft YaHei", 18, "bold"))
        title.pack(side="left")
        self.status_bar = tk.Label(header, text="", bg=BG_DARK, fg=FG_SECONDARY,
                                   font=("Microsoft YaHei", 10))
        self.status_bar.pack(side="right")

    def _setup_cards(self) -> None:
        cards = tk.Frame(self.root, bg=BG_DARK)
        cards.pack(fill="x", padx=20, pady=4)
        for i, name in enumerate(self.service_names):
            cards.columnconfigure(i, weight=1)
            card = tk.Frame(cards, bg=BG_CARD, padx=12, pady=10, highlightthickness=1,
                            highlightbackground=BG_INPUT)
            card.grid(row=0, column=i, sticky="nsew", padx=6)

            svc = self.M.SERVICES[name]
            tk.Label(card, text=svc["name"], bg=BG_CARD, fg=FG_PRIMARY,
                     font=("Microsoft YaHei", 11, "bold")).pack(anchor="w")
            self.status_vars[name] = tk.Label(card, text="○ 已停止", bg=BG_CARD,
                                              fg=FG_SECONDARY, font=("Consolas", 10))
            self.status_vars[name].pack(anchor="w", pady=(2, 8))

            btns = tk.Frame(card, bg=BG_CARD)
            btns.pack(fill="x")
            self._btn(btns, "▶ 启动", ACCENT_OK, lambda n=name: self._run("start", n)).pack(side="left", expand=True, padx=2)
            self._btn(btns, "■ 停止", ACCENT_ERR, lambda n=name: self._run("stop", n)).pack(side="left", expand=True, padx=2)
            self._btn(btns, "↻ 重启", ACCENT_WARN, lambda n=name: self._run("restart", n)).pack(side="left", expand=True, padx=2)

    def _setup_logs(self) -> None:
        frame = tk.Frame(self.root, bg=BG_DARK)
        frame.pack(fill="both", expand=True, padx=20, pady=8)
        bar = tk.Frame(frame, bg=BG_DARK)
        bar.pack(fill="x")
        tk.Label(bar, text="📜 日志", bg=BG_DARK, fg=FG_PRIMARY,
                 font=("Microsoft YaHei", 11, "bold")).pack(side="left")
        self.log_sel = ttk.Combobox(bar, values=["backend", "frontend", "nginx"],
                                    state="readonly", width=12)
        self.log_sel.set("backend")
        self.log_sel.pack(side="left", padx=8)
        self._btn(bar, "⟳ 刷新日志", ACCENT, self._refresh_log).pack(side="left", padx=4)

        self.log_view = scrolledtext.ScrolledText(frame, height=14, bg=BG_INPUT, fg=FG_PRIMARY,
                                                  font=("Consolas", 9), insertbackground=FG_PRIMARY)
        self.log_view.pack(fill="both", expand=True, pady=(6, 0))
        self.log_view.configure(state="disabled")

    def _btn(self, parent, text, color, cmd):
        return tk.Button(parent, text=text, command=cmd, bg=BG_CARD, fg=color,
                         activebackground=BG_INPUT, activeforeground=color,
                         relief="flat", font=("Microsoft YaHei", 9), padx=6)

    # ── 逻辑 ──────────────────────────────────────────────────────────

    def _run(self, action: str, name: str) -> None:
        def worker():
            try:
                if action == "start":
                    self.M.start_service(name)
                elif action == "stop":
                    self.M.stop_service(name)
                elif action == "restart":
                    self.M.stop_service(name)
                    self.M.start_service(name)
                self.root.after(0, self._refresh_status)
            except Exception as e:  # noqa: BLE001
                self.root.after(0, lambda: self._flash_status(f"❌ {e}"))

        threading.Thread(target=worker, daemon=True).start()

    def _refresh_status(self) -> None:
        for name in self.service_names:
            alive = self.M.is_running(name)
            self.status_vars[name].config(
                text=f"● 运行中 · PID {self.M._read_pid(name)}" if alive else "○ 已停止",
                fg=ACCENT_OK if alive else FG_SECONDARY)
        self.status_bar.config(text=f"上次刷新 {__import__('datetime').datetime.now():%H:%M:%S}")

    def _auto_refresh(self) -> None:
        self._refresh_status()
        self.root.after(5000, self._auto_refresh)

    def _flash_status(self, msg: str) -> None:
        self.status_bar.config(text=msg)

    def _refresh_log(self) -> None:
        name = self.log_sel.get()
        log_f = self.M.SERVICES[name]["log"]
        try:
            text = log_f.read_text(encoding="utf-8", errors="replace")[-8000:]
        except FileNotFoundError:
            text = "(暂无日志)"
        self.log_view.configure(state="normal")
        self.log_view.delete("1.0", "end")
        self.log_view.insert("1.0", text)
        self.log_view.configure(state="disabled")
        self.log_view.see("end")


def run_gui() -> None:
    root = tk.Tk()
    ManageGUI(root)
    root.mainloop()


if __name__ == "__main__":
    run_gui()
