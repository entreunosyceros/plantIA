from __future__ import annotations

import json
import urllib.parse
import urllib.request
from pathlib import Path

from plantia.config import APP_DATA_DIR, ensure_directories

_CACHE_PATH = APP_DATA_DIR / "candidate_thumbnails.json"


def _load_cache() -> dict[str, str]:
    try:
        if not _CACHE_PATH.exists():
            return {}
        data = json.loads(_CACHE_PATH.read_text(encoding="utf-8"))
        return data if isinstance(data, dict) else {}
    except Exception:
        return {}


def _save_cache(cache: dict[str, str]) -> None:
    ensure_directories()
    tmp = Path(str(_CACHE_PATH) + ".tmp")
    tmp.write_text(json.dumps(cache, ensure_ascii=False, indent=2), encoding="utf-8")
    tmp.replace(_CACHE_PATH)


def get_thumbnail_url(nombre_cientifico: str) -> str:
    """
    Devuelve una miniatura (URL) para un nombre científico usando Wikipedia (REST summary).
    Si no hay imagen disponible o falla la consulta, devuelve "".
    """
    key = (nombre_cientifico or "").strip()
    if not key:
        return ""

    cache = _load_cache()
    if key in cache:
        return cache[key]

    title = key.replace(" ", "_")
    url = (
        "https://en.wikipedia.org/api/rest_v1/page/summary/"
        + urllib.parse.quote(title)
    )
    try:
        req = urllib.request.Request(
            url,
            headers={"User-Agent": "PlantIA/0.1 (+https://localhost)"},
        )
        with urllib.request.urlopen(req, timeout=6) as resp:
            payload = json.loads(resp.read().decode("utf-8"))
        thumb = payload.get("thumbnail", {}) or {}
        src = (thumb.get("source") or "").strip()
        cache[key] = src
        _save_cache(cache)
        return src
    except Exception:
        cache[key] = ""
        _save_cache(cache)
        return ""

