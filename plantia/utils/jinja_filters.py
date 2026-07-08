from __future__ import annotations

import json
from typing import Any


def from_json(value: str | None, default: Any = None) -> Any:
    """Parsea un string JSON para usarlo en plantillas Jinja."""
    if default is None:
        default = None
    if not value or not str(value).strip():
        return default
    try:
        return json.loads(value)
    except (json.JSONDecodeError, TypeError):
        return default


def detalle_json_fields(planta) -> dict[str, Any]:
    """Campos JSON de la ficha parseados para la plantilla de detalle."""
    guia = from_json(getattr(planta, "guia_inicio_json", None), {})
    if not isinstance(guia, dict):
        guia = {}
    rasgos = from_json(getattr(planta, "rasgos_observados_json", None), [])
    if not isinstance(rasgos, list):
        rasgos = []
    preguntas = from_json(getattr(planta, "preguntas_para_mejorar_json", None), [])
    if not isinstance(preguntas, list):
        preguntas = []
    return {
        "guia": guia,
        "rasgos": rasgos,
        "preguntas": preguntas,
    }
