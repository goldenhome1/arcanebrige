import ctypes
from ctypes import wintypes
import os
import re
import shutil
import subprocess
import threading
import time
import tkinter as tk
from pathlib import Path

user32 = ctypes.windll.user32


# Структура для хранения координат мыши
class POINT(ctypes.Structure):
    _fields_ = [("x", ctypes.c_long), ("y", ctypes.c_long)]


def find_tlauncher_hwnd():
    """Находит дескриптор (HWND) видимого окна TLauncher"""
    found = []

    @ctypes.WINFUNCTYPE(wintypes.BOOL, wintypes.HWND, wintypes.LPARAM)
    def enum_windows_callback(hwnd, lparam):
        if user32.IsWindowVisible(hwnd):
            length = user32.GetWindowTextLengthW(hwnd)
            if length > 0:
                buff = ctypes.create_unicode_buffer(length + 1)
                user32.GetWindowTextW(hwnd, buff, length + 1)
                title = buff.value
                if "TLauncher" in title:
                    found.append(hwnd)
        return True

    user32.EnumWindows(enum_windows_callback, 0)
    return found[0] if found else None


def click_and_restore_cursor(x, y):
    """
    1. Запоминает где стояла мышь
    2. Мгновенно переносит на кнопку и делает клик ЛКМ
    3. Сразу возвращает курсор в исходную точку
    """
    orig_pos = POINT()
    user32.GetCursorPos(ctypes.byref(orig_pos))

    user32.SetCursorPos(x, y)
    time.sleep(0.03)
    user32.mouse_event(0x0002, 0, 0, 0, 0)  # MOUSEEVENTF_LEFTDOWN
    time.sleep(0.03)
    user32.mouse_event(0x0004, 0, 0, 0, 0)  # MOUSEEVENTF_LEFTUP
    time.sleep(0.02)

    user32.SetCursorPos(orig_pos.x, orig_pos.y)


def force_foreground_window(hwnd):
    """Надежно разворачивает и переводит окно на передний план, обходя блокировку фокуса Windows"""
    try:
        user32.ShowWindow(hwnd, 9)  # SW_RESTORE
        fore_hwnd = user32.GetForegroundWindow()
        fore_thread = user32.GetWindowThreadProcessId(fore_hwnd, None)
        app_thread = ctypes.windll.kernel32.GetCurrentThreadId()

        if fore_thread != app_thread:
            user32.AttachThreadInput(fore_thread, app_thread, True)
            user32.SetForegroundWindow(hwnd)
            user32.SetFocus(hwnd)
            user32.AttachThreadInput(fore_thread, app_thread, False)
        else:
            user32.SetForegroundWindow(hwnd)
            user32.SetFocus(hwnd)
    except Exception:
        user32.ShowWindow(hwnd, 9)
        user32.SetForegroundWindow(hwnd)


# === Конфигурация путей ===
PROJECT_DIR = Path(__file__).resolve().parent
SRC_DIR = PROJECT_DIR / "build" / "libs"
APPDATA = os.environ.get("APPDATA", r"C:\Users\golde\AppData\Roaming")
DST_DIR = Path(APPDATA) / ".minecraft" / "versions" / "arcane_assamblage" / "mods"

# Префиксы мода для очистки старых версий (arcane_bridge и etheral_armor)
MOD_PREFIXES = ["arcane_bridge", "etheral_armor"]


def kill_previous_console():
    """Закрывает предыдущее открытое окно консоли сборки перед запуском новой"""
    pid_file = PROJECT_DIR / ".build_console.pid"
    if pid_file.exists():
        try:
            old_pid = pid_file.read_text(encoding="utf-8").strip()
            if old_pid:
                startupinfo = subprocess.STARTUPINFO()
                startupinfo.dwFlags |= subprocess.STARTF_USESHOWWINDOW
                subprocess.run(
                    ["taskkill", "/F", "/T", "/PID", old_pid],
                    startupinfo=startupinfo,
                    capture_output=True,
                )
            pid_file.unlink()
        except Exception:
            pass


def copy_to_clipboard(text: str):
    """Надежно копирует текст в буфер обмена Windows через Tkinter"""
    if not text:
        return
    try:
        root = tk.Tk()
        root.withdraw()
        root.clipboard_clear()
        root.clipboard_append(text)
        root.update()
        root.destroy()
    except Exception:
        try:
            startupinfo = subprocess.STARTUPINFO()
            startupinfo.dwFlags |= subprocess.STARTF_USESHOWWINDOW
            subprocess.run(
                ["cmd.exe", "/c", "clip"],
                input=text.encode("utf-16le"),
                startupinfo=startupinfo,
                check=True,
            )
        except Exception:
            pass


def extract_build_error(log_path: Path) -> str:
    """Извлекает ключевой блок ошибок компилятора и Gradle из лога"""
    if not log_path.exists():
        return ""
    try:
        text = log_path.read_text(encoding="utf-8", errors="replace")
    except Exception:
        return ""

    # Очистка ANSI escape-последовательностей терминала
    text = re.sub(r"\x1b\[[0-9;]*[a-zA-Z]", "", text)

    lines = text.splitlines()
    extracted = []
    capture = False

    for line in lines:
        if any(
            marker in line
            for marker in [
                ":compileJava FAILED",
                "error:",
                "FAILURE:",
                "* What went wrong:",
            ]
        ):
            capture = True
        if capture:
            extracted.append(line)

    if extracted:
        return "\n".join(extracted).strip()
    return text[-3000:].strip()


def show_auto_close_popup(title, message, is_success=True, timeout_ms=1500):
    """Создает всплывающее окно по центру экрана, закрывающееся автоматически"""
    root = tk.Tk()
    root.overrideredirect(True)
    root.attributes("-topmost", True)

    bg_color = "#16a34a" if is_success else "#dc2626"
    fg_color = "#ffffff"

    label = tk.Label(
        root,
        text=f"{title}\n\n{message}",
        font=("Segoe UI", 10, "bold"),
        bg=bg_color,
        fg=fg_color,
        padx=25,
        pady=15,
        justify="center",
    )
    label.pack()
    root.config(bg=bg_color)

    root.update_idletasks()
    w = root.winfo_width()
    h = root.winfo_height()
    x = (root.winfo_screenwidth() // 2) - (w // 2)
    y = (root.winfo_screenheight() // 2) - (h // 2)
    root.geometry(f"+{x}+{y}")

    root.after(timeout_ms, root.destroy)
    root.mainloop()


class DeployAndRestartApp:
    """Всплывающее окно с визуальным прогрессом развертывания и перезапуска игры через TLauncher"""

    def __init__(self, root, file_info):
        self.root = root
        self.file_info = file_info
        self.root.overrideredirect(True)
        self.root.attributes("-topmost", True)

        self.bg_color = "#1e293b"
        self.root.config(bg=self.bg_color)

        self.frame = tk.Frame(self.root, bg=self.bg_color, padx=25, pady=20)
        self.frame.pack()

        self.label_file = tk.Label(
            self.frame,
            text=f"📦 {self.file_info}",
            font=("Segoe UI", 9, "bold"),
            bg=self.bg_color,
            fg="#94a3b8",
        )
        self.label_file.pack(pady=(0, 5))

        self.label_title = tk.Label(
            self.frame,
            text="🔄 Перезапуск игры...",
            font=("Segoe UI", 11, "bold"),
            bg=self.bg_color,
            fg="#f8fafc",
        )
        self.label_title.pack()

        self._center_window()
        threading.Thread(target=self._run_restart_thread, daemon=True).start()

    def _center_window(self):
        self.root.update_idletasks()
        w = self.root.winfo_width()
        h = self.root.winfo_height()
        x = (self.root.winfo_screenwidth() // 2) - (w // 2)
        y = (self.root.winfo_screenheight() // 2) - (h // 2)
        self.root.geometry(f"+{x}+{y}")

    def _update_status(self, text, bg_color=None):
        if bg_color:
            self.root.config(bg=bg_color)
            self.frame.config(bg=bg_color)
            self.label_file.config(bg=bg_color)
            self.label_title.config(bg=bg_color)
        self.label_title.config(text=text)
        self._center_window()

    def _run_restart_thread(self):
        try:
            startupinfo = subprocess.STARTUPINFO()
            startupinfo.dwFlags |= subprocess.STARTF_USESHOWWINDOW

            # 1. Мягкое закрытие Minecraft ("по крестику")
            self.root.after(0, self._update_status, "❌ Выходим из Minecraft...")

            soft_close_ps = (
                "Get-Process javaw -ErrorAction SilentlyContinue | "
                "ForEach-Object { $_.CloseMainWindow() }; "
                "$p = Get-Process javaw -ErrorAction SilentlyContinue; "
                "if ($p) { $p | Wait-Process -Timeout 8 -ErrorAction SilentlyContinue }"
            )
            subprocess.run(["powershell", "-Command", soft_close_ps], startupinfo=startupinfo)

            self.root.after(0, self._update_status, "⏳ Поиск окна TLauncher...")

            # 2. Ожидание разворачивания окна TLauncher
            hwnd = None
            for _ in range(20):
                hwnd = find_tlauncher_hwnd()
                if hwnd:
                    break
                time.sleep(0.5)

            if not hwnd:
                self.root.after(0, self._show_finish, "⚠️ Мод обновлен (окно TLauncher не найдено)", False)
                return

            # 3. Активация окна TLauncher
            user32.ShowWindow(hwnd, 9)  # SW_RESTORE
            user32.SetForegroundWindow(hwnd)
            time.sleep(0.3)

            # 4. Расчет координат кнопки "Войти в игру"
            rect = wintypes.RECT()
            user32.GetWindowRect(hwnd, ctypes.byref(rect))

            win_width = rect.right - rect.left
            win_height = rect.bottom - rect.top

            button_x = rect.left + int(win_width * 0.62)
            button_y = rect.top + int(win_height * 0.92)

            self.root.after(0, self._update_status, "🚀 Запуск...")

            # 5. Мгновенный клик с авто-возвратом курсора
            click_and_restore_cursor(button_x, button_y)

            self.root.after(0, self._show_finish, "✅ Игра перезапускается!", True)

        except Exception as e:
            self.root.after(0, self._show_finish, f"❌ Ошибка перезапуска: {e}", False)

    def _show_finish(self, text, is_success):
        color = "#16a34a" if is_success else "#f59e0b"
        self._update_status(text, bg_color=color)
        timeout = 1500 if is_success else 2500
        self.root.after(timeout, self.root.destroy)


def build_and_deploy():
    # 1. Проверка существования проекта
    if not PROJECT_DIR.exists():
        show_auto_close_popup(
            "❌ Ошибка",
            f"Папка проекта не найдена:\n{PROJECT_DIR}",
            is_success=False,
            timeout_ms=1500,
        )
        return

    gradle_bat = PROJECT_DIR / "gradlew.bat"
    if not gradle_bat.exists():
        show_auto_close_popup(
            "❌ Ошибка",
            "Файл gradlew.bat не найден в папке проекта!",
            is_success=False,
            timeout_ms=1500,
        )
        return

    # 2. Закрытие предыдущей зависшей консоли и очистка старых файлов
    kill_previous_console()

    status_file = PROJECT_DIR / ".build_status"
    if status_file.exists():
        try:
            status_file.unlink()
        except Exception:
            pass

    log_file = PROJECT_DIR / ".build_log.txt"
    if log_file.exists():
        try:
            log_file.unlink()
        except Exception:
            pass

    # Основная сборка идет с быстрым инкрементальным билдом
    cmd = (
        "call gradlew.bat build && "
        "(echo SUCCESS > .build_status) || "
        "(call gradlew.bat build > .build_log.txt 2>&1 & "
        "echo FAILED > .build_status & echo. & "
        "echo ====================================================== & "
        "echo  [ERROR] Build failed! Error copied to clipboard. & "
        "echo ====================================================== & "
        "pause)"
    )

    try:
        proc = subprocess.Popen(
            ["cmd.exe", "/c", cmd],
            cwd=str(PROJECT_DIR),
            creationflags=subprocess.CREATE_NEW_CONSOLE if os.name == "nt" else 0,
        )

        # Сохраняем PID консоли для авто-закрытия при следующем запуске
        try:
            (PROJECT_DIR / ".build_console.pid").write_text(str(proc.pid), encoding="utf-8")
        except Exception:
            pass

        while proc.poll() is None and not status_file.exists():
            time.sleep(0.1)

        time.sleep(0.2)

        build_success = False
        if status_file.exists():
            try:
                status = status_file.read_text(encoding="utf-8").strip()
                build_success = (status == "SUCCESS")
                status_file.unlink()
            except Exception:
                pass

        if not build_success:
            error_details = extract_build_error(log_file)
            if error_details:
                copy_to_clipboard(error_details)

            show_auto_close_popup(
                "❌ Ошибка Gradle",
                "Сборка завершилась с ошибкой!\nТекст ошибки скопирован в буфер.",
                is_success=False,
                timeout_ms=2200,
            )
            return

    except Exception as e:
        show_auto_close_popup(
            "❌ Ошибка запуска",
            f"Не удалось запустить сборку:\n{e}",
            is_success=False,
            timeout_ms=1500,
        )
        return

    # 3. Проверка папки с собранными jar-файлами
    if not SRC_DIR.exists():
        show_auto_close_popup(
            "❌ Ошибка",
            f"Папка сборки не найдена:\n{SRC_DIR}",
            is_success=False,
            timeout_ms=1500,
        )
        return

    jar_files = [
        f
        for f in SRC_DIR.glob("*.jar")
        if not f.name.endswith(("-sources.jar", "-dev.jar", "-shadow.jar"))
    ]

    if not jar_files:
        show_auto_close_popup(
            "⚠️ Предупреждение",
            "Готовые .jar файлы в build/libs не найдены!",
            is_success=False,
            timeout_ms=1500,
        )
        return

    DST_DIR.mkdir(parents=True, exist_ok=True)

    # 4. Очистка прошлых версий мода в папке назначения
    for prefix in MOD_PREFIXES:
        for old_jar in DST_DIR.glob(f"{prefix}*.jar"):
            try:
                old_jar.unlink()
            except Exception:
                pass

    for jar in jar_files:
        base_prefix = jar.name.split("-")[0]
        for old_jar in DST_DIR.glob(f"{base_prefix}*.jar"):
            try:
                old_jar.unlink()
            except Exception:
                pass

    # 5. Копирование нового .jar файла
    try:
        copied_files = []
        for jar in jar_files:
            target_path = DST_DIR / jar.name
            shutil.copy2(str(jar), str(target_path))
            copied_files.append(jar.name)

        files_str = ", ".join(copied_files)

        # Запускаем единое окно статуса и перезапуска игры
        root = tk.Tk()
        DeployAndRestartApp(root, files_str)
        root.mainloop()

    except Exception as e:
        show_auto_close_popup(
            "❌ Ошибка копирования",
            f"{e}",
            is_success=False,
            timeout_ms=1500,
        )


if __name__ == "__main__":
    build_and_deploy()