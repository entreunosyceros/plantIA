from __future__ import annotations

import json
import os
import socket
import signal
import subprocess
import sys
import time
from re import findall
from pathlib import Path

PROJECT_DIR = Path(__file__).resolve().parent
REQUIREMENTS_FILE = PROJECT_DIR / "requirements.txt"
DEFAULT_PORT = int(os.getenv("PLANTIA_PORT", "8000"))

APP_DATA_DIR = Path.home() / ".plantia"
LOCAL_CONFIG_PATH = APP_DATA_DIR / "config.json"


def get_venv_dir() -> Path:
    override = os.environ.get("PLANTIA_VENV")
    if override:
        return Path(override)
    local = PROJECT_DIR / ".venv"
    if os.access(PROJECT_DIR, os.W_OK):
        return local
    data_home = Path(
        os.environ.get("XDG_DATA_HOME", Path.home() / ".local" / "share")
    )
    return data_home / "plantia" / ".venv"


def get_venv_python() -> Path:
    venv_dir = get_venv_dir()
    if sys.platform == "win32":
        return venv_dir / "Scripts" / "python.exe"
    return venv_dir / "bin" / "python"


def is_running_in_venv() -> bool:
    if not get_venv_dir().exists():
        return False
    return Path(sys.prefix).resolve() == get_venv_dir().resolve()


def create_venv() -> None:
    venv_dir = get_venv_dir()
    venv_dir.parent.mkdir(parents=True, exist_ok=True)
    print(f"Creando entorno virtual en {venv_dir} …")
    subprocess.run(
        [sys.executable, "-m", "venv", str(venv_dir)],
        check=True,
        cwd=PROJECT_DIR,
    )
    print("Entorno virtual creado.")


def ensure_venv() -> Path:
    venv_python = get_venv_python()
    if not venv_python.exists():
        create_venv()
        venv_python = get_venv_python()
    if not venv_python.exists():
        raise RuntimeError("No se pudo crear el entorno virtual.")
    return venv_python


def install_dependencies(venv_python: Path) -> None:
    print("Instalando dependencias en el entorno virtual …")
    subprocess.run(
        [str(venv_python), "-m", "pip", "install", "-q", "--upgrade", "pip"],
        check=True,
        cwd=PROJECT_DIR,
    )
    subprocess.run(
        [
            str(venv_python),
            "-m",
            "pip",
            "install",
            "-q",
            "-r",
            str(REQUIREMENTS_FILE),
        ],
        check=True,
        cwd=PROJECT_DIR,
    )
    print("Dependencias instaladas correctamente.")


def launch_app() -> int:
    import uvicorn

    port = int(os.getenv("PLANTIA_PORT", str(DEFAULT_PORT)))

    def _is_port_open(p: int) -> bool:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            return s.connect_ex(("127.0.0.1", p)) == 0

    def _pids_using_port(p: int) -> list[int]:
        try:
            out = subprocess.check_output(["ss", "-ltnp"], text=True, stderr=subprocess.STDOUT)
        except Exception:
            return []
        # Example: users:(("python",pid=33160,fd=16))
        pids: list[int] = []
        for line in out.splitlines():
            if f":{p} " in line or f":{p}\t" in line or f":{p}," in line or f":{p}: " in line:
                pids.extend([int(x) for x in findall(r"pid=(\d+)", line)])
        return list(dict.fromkeys(pids))

    def _is_plantia_pid(pid: int) -> bool:
        try:
            cmd = subprocess.check_output(
                ["ps", "-p", str(pid), "-o", "cmd="], text=True, stderr=subprocess.DEVNULL
            ).strip()
        except Exception:
            return False
        cmd_l = cmd.lower()
        # Heurística: evita matar algo que no sea PlantIA/uvicorn.
        return ("plantia" in cmd_l) or ("run_app.py" in cmd_l) or ("uvicorn" in cmd_l)

    if _is_port_open(port):
        # Intento 1: cerrar procesos que parezcan PlantIA.
        pids = _pids_using_port(port)
        plantia_pids = [pid for pid in pids if _is_plantia_pid(pid)]
        if plantia_pids:
            print(
                f"Puerto {port} ya está en uso. Intentando cerrar PlantIA (PIDs: {plantia_pids}) …",
                file=sys.stderr,
            )
            for pid in plantia_pids:
                try:
                    os.kill(pid, signal.SIGTERM)
                except Exception:
                    continue
            time.sleep(1.0)

        # Reintento: si ya está libre, seguimos.
        if _is_port_open(port):
            print(
                f"Error: el puerto {port} sigue ocupado. Detén el proceso que lo usa o arranca con otro puerto, por ejemplo:\n"
                f"  PLANTIA_PORT=8001 python3 run_app.py",
                file=sys.stderr,
            )
            return 1

    print(f"Iniciando PlantIA en http://localhost:{port}")
    uvicorn.run("main:app", host="0.0.0.0", port=port, reload=False)
    return 0


def has_configured_api_key() -> bool:
    if (os.getenv("GEMINI_API_KEY") or "").strip():
        return True
    try:
        if not LOCAL_CONFIG_PATH.exists():
            return False
        data = json.loads(LOCAL_CONFIG_PATH.read_text(encoding="utf-8"))
        if not isinstance(data, dict):
            return False
        return bool((data.get("GEMINI_API_KEY") or "").strip())
    except Exception:
        return False


def main() -> int:
    os.chdir(PROJECT_DIR)
    if str(PROJECT_DIR) not in sys.path:
        sys.path.insert(0, str(PROJECT_DIR))

    env_file = PROJECT_DIR / ".env"
    if not env_file.exists():
        example = PROJECT_DIR / ".env.example"
        if example.exists():
            if not has_configured_api_key():
                print(
                    "Aviso: no existe .env. Copia .env.example a .env y añade tu GEMINI_API_KEY "
                    "(o configúrala desde la interfaz en /configuracion).",
                    file=sys.stderr,
                )

    if is_running_in_venv():
        try:
            return launch_app()
        except Exception as exc:
            import traceback

            print(f"Error al iniciar PlantIA: {exc}", file=sys.stderr)
            traceback.print_exc()
            return 1

    try:
        venv_python = ensure_venv()
        install_dependencies(venv_python)

        print("Iniciando PlantIA …")
        env = os.environ.copy()
        env["PYTHONPATH"] = str(PROJECT_DIR)
        os.environ.update(env)
        argv = [str(venv_python), str(__file__), *sys.argv[1:]]
        os.execv(str(venv_python), argv)
        return 1

    except subprocess.CalledProcessError as exc:
        print(f"Error al preparar el entorno: {exc}", file=sys.stderr)
        return 1
    except KeyboardInterrupt:
        print("\nServidor detenido.")
        return 0
    except Exception as exc:
        print(f"Error: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
