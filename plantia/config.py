"""
Configuración centralizada de rutas y constantes de PlantIA.
"""

from __future__ import annotations

import json
import os
from pathlib import Path

from dotenv import load_dotenv

load_dotenv()

APP_DATA_DIR = Path.home() / ".plantia"
DATABASE_PATH = APP_DATA_DIR / "plantia.db"
UPLOADS_DIR = APP_DATA_DIR / "uploads"
CONFIG_PATH = APP_DATA_DIR / "config.json"

DEFAULT_GEMINI_MODEL = "gemini-2.5-flash"

DEPRECATED_GEMINI_MODELS = {
    "gemini-2.0-flash",
    "gemini-2.0-flash-001",
    "gemini-2.0-flash-lite",
    "gemini-2.0-flash-lite-001",
}

AVAILABLE_GEMINI_MODELS: list[tuple[str, str]] = [
    ("gemini-2.5-flash", "Gemini 2.5 Flash (recomendado)"),
    ("gemini-2.5-flash-lite", "Gemini 2.5 Flash Lite (rápido y económico)"),
    ("gemini-3-flash-preview", "Gemini 3 Flash Preview"),
    ("gemini-3.1-flash-lite", "Gemini 3.1 Flash Lite"),
]

ALLOWED_GEMINI_MODELS = {model_id for model_id, _ in AVAILABLE_GEMINI_MODELS}

MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024  # 10 MB
MAX_IMAGE_DIMENSION = 2048
ALLOWED_IMAGE_TYPES = {"image/jpeg", "image/png", "image/webp"}

PAGE_SIZE = 20


def ensure_directories() -> None:
    """Crea las carpetas necesarias si no existen."""
    APP_DATA_DIR.mkdir(parents=True, exist_ok=True)
    UPLOADS_DIR.mkdir(parents=True, exist_ok=True)


def _read_local_config() -> dict:
    try:
        if not CONFIG_PATH.exists():
            return {}
        data = json.loads(CONFIG_PATH.read_text(encoding="utf-8"))
        return data if isinstance(data, dict) else {}
    except Exception:
        return {}


def _write_local_config(data: dict) -> None:
    ensure_directories()
    tmp = CONFIG_PATH.with_suffix(".tmp")
    tmp.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
    os.chmod(tmp, 0o600)
    tmp.replace(CONFIG_PATH)


def get_gemini_api_key() -> str:
    """
    Obtiene la clave de Gemini.

    Prioridad:
    1) Archivo local ~/.plantia/config.json (si existe)
    2) Variable de entorno / .env: GEMINI_API_KEY
    """
    cfg = _read_local_config()
    key = (cfg.get("GEMINI_API_KEY") or "").strip()
    if key:
        return key
    return (os.getenv("GEMINI_API_KEY") or "").strip()


def _normalize_gemini_model(model: str) -> str:
    model = (model or "").strip()
    if not model or model in DEPRECATED_GEMINI_MODELS:
        return DEFAULT_GEMINI_MODEL
    if model not in ALLOWED_GEMINI_MODELS:
        raise ValueError(
            f"Modelo no permitido: {model}. "
            f"Usa uno de: {', '.join(sorted(ALLOWED_GEMINI_MODELS))}."
        )
    return model


def get_gemini_model() -> str:
    """
    Obtiene el modelo de Gemini.

    Prioridad:
    1) Archivo local ~/.plantia/config.json (si existe)
    2) Variable de entorno / .env: GEMINI_MODEL
    """
    cfg = _read_local_config()
    model = (cfg.get("GEMINI_MODEL") or "").strip()
    if model:
        return _normalize_gemini_model(model)
    return _normalize_gemini_model(os.getenv("GEMINI_MODEL", DEFAULT_GEMINI_MODEL))


def save_gemini_config(
    api_key: str | None = None,
    model: str | None = None,
) -> None:
    """Guarda clave y/o modelo en ~/.plantia/config.json."""
    cfg = _read_local_config()
    changed = False

    if api_key is not None:
        key = api_key.strip()
        if key:
            cfg["GEMINI_API_KEY"] = key
            changed = True

    if model is not None:
        cfg["GEMINI_MODEL"] = _normalize_gemini_model(model)
        changed = True

    if not changed:
        raise ValueError("No hay cambios para guardar.")

    if not (cfg.get("GEMINI_API_KEY") or get_gemini_api_key()):
        raise ValueError(
            "Debes configurar una API Key de Gemini antes de guardar la configuración."
        )

    _write_local_config(cfg)


def set_gemini_api_key(api_key: str) -> None:
    """Guarda la clave en ~/.plantia/config.json con permisos 600."""
    key = (api_key or "").strip()
    if not key:
        raise ValueError("La clave no puede estar vacía.")
    save_gemini_config(api_key=key)


def clear_gemini_api_key() -> None:
    """Elimina la clave guardada localmente (no toca el .env)."""
    cfg = _read_local_config()
    if "GEMINI_API_KEY" in cfg:
        cfg.pop("GEMINI_API_KEY", None)
        _write_local_config(cfg)
