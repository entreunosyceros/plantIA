"""
Servicio de identificación de plantas con Gemini Vision.
"""

from __future__ import annotations

import json
import re
import time
from io import BytesIO

from google import genai
from google.genai import types
from PIL import Image

from plantia.config import (
    MAX_IMAGE_DIMENSION,
    MAX_IMAGE_SIZE_BYTES,
    get_gemini_api_key,
    get_gemini_model,
)
from plantia.schemas.planta import IdentificacionGemini

PROMPT = """Eres un botánico experto. Analiza la imagen de esta planta e identifícala.

Responde ÚNICAMENTE con un objeto JSON válido (sin markdown, sin texto adicional) con esta estructura exacta.

Reglas:
- Si no estás seguro de la especie exacta, rellena al menos género y familia en taxonomía, y deja especie vacía.
- Si la confianza es "bajo", incluye 3-5 candidatos y 2-5 preguntas para mejorar la identificación.
- Sé conservador con las recomendaciones si la confianza es baja.

{
  "nombre_comun": "nombre común en español",
  "nombre_cientifico": "nombre científico (género y especie)",
  "familia": "familia botánica",
  "tipo": "tipo de planta (ej. suculenta, árbol, hierba, arbusto, trepadora)",
  "descripcion": "breve descripción de la planta (2-3 frases)",
  "taxonomia": {
    "reino": "Plantae u otro",
    "orden": "orden botánico si es posible",
    "familia": "familia botánica",
    "genero": "género botánico",
    "especie": "especie (solo si estás razonablemente seguro)"
  },
  "rasgos_observados": [
    "lista corta de rasgos visibles (3-10) que justifican la identificación"
  ],
  "preguntas_para_mejorar": [
    "preguntas para el usuario (2-5) si hace falta confirmar"
  ],
  "candidatos": [
    {
      "nombre_comun": "opcional",
      "nombre_cientifico": "candidato",
      "razon": "por qué podría ser esta planta (1-2 frases)",
      "confianza": "alto|medio|bajo"
    }
  ],
  "guia_inicio": {
    "pasos": [
      "pasos simples para empezar hoy (5-8), aptos para principiantes"
    ],
    "errores_comunes": [
      "errores comunes y cómo evitarlos (3-6)"
    ]
  },
  "cuidados": {
    "riego": "frecuencia y cantidad de riego",
    "luz": "necesidades de luz (pleno sol, sombra parcial, etc.)",
    "temperatura": "rango de temperatura ideal",
    "humedad": "necesidades de humedad ambiental",
    "suelo": "tipo de sustrato recomendado",
    "fertilizacion": "cuándo y cómo fertilizar",
    "problemas_comunes": "plagas, enfermedades o problemas frecuentes de cultivo",
    "plagas_habituales": "plagas e insectos que suelen afectar a esta planta"
  },
  "caracteristicas": {
    "dificultad": "fácil|media|difícil",
    "tamano_adulto": "altura y anchura aproximadas en condiciones normales",
    "crecimiento": "ritmo y tipo de crecimiento (lento, moderado, rápido, trepador, etc.)",
    "floracion": "si florece, color y época aproximada",
    "epoca_poda": "cuándo y cómo podar, o 'no requiere poda' si aplica",
    "epoca_trasplante": "mejor época para trasplantar (meses/estación) y frecuencia orientativa",
    "senales_trasplante": "señales de que necesita trasplante (raíces, drenaje, sustrato agotado, etc.)",
    "maceta_y_sustrato": "recomendación de tipo de maceta (tamaño/material/drenaje) y sustrato"
  },
  "toxicidad": {
    "perros": "toxicidad para perros (no tóxica, leve, moderada, alta) y síntomas si aplica",
    "gatos": "toxicidad para gatos (no tóxica, leve, moderada, alta) y síntomas si aplica"
  },
  "confianza": "alto|medio|bajo"
}

Si no puedes identificar la planta con certeza, indica tu mejor estimación y usa confianza "bajo".
Todos los textos deben estar en español."""


class GeminiService:
    """Identifica plantas a partir de imágenes usando Gemini."""

    @staticmethod
    def _is_transient(exc: Exception) -> bool:
        """Errores temporales del lado de Google que conviene reintentar."""
        raw = str(exc)
        upper = raw.upper()
        return (
            "UNAVAILABLE" in upper
            or "OVERLOADED" in upper
            or "503" in raw
            or "RESOURCE_EXHAUSTED" in upper
            or " 429" in raw
            or "CODE': 429" in upper
        )

    @staticmethod
    def _friendly_error(exc: Exception) -> str:
        raw = str(exc)
        upper = raw.upper()

        if "RESOURCE_EXHAUSTED" in upper or " 429" in raw or "CODE': 429" in upper:
            wait = None
            m = re.search(r"retry in\s+(\d+(?:\.\d+)?)s", raw, flags=re.IGNORECASE)
            if m:
                wait = m.group(1)
            else:
                m = re.search(r"retryDelay'\s*:\s*'(\d+)s'", raw)
                if m:
                    wait = m.group(1)

            extra = f" (espera ~{wait}s y reintenta)" if wait else ""
            return (
                "Gemini rechazó la petición por límite de cuota (429 RESOURCE_EXHAUSTED)"
                f"{extra}. Revisa tu plan/cuotas en Google AI Studio y prueba de nuevo."
            )

        if "UNAVAILABLE" in upper or "OVERLOADED" in upper or "503" in raw:
            return (
                "El modelo de Gemini está sobrecargado en este momento (503 UNAVAILABLE). "
                "Es un problema temporal de Google: espera unos segundos y vuelve a intentarlo. "
                "Si persiste, prueba con otro modelo en Configuración."
            )

        return raw

    @staticmethod
    def _get_client() -> genai.Client:
        api_key = get_gemini_api_key()
        if not api_key:
            raise ValueError(
                "No se ha configurado GEMINI_API_KEY. "
                "Añade tu clave en Configuración o crea un archivo .env con tu clave de Google AI Studio."
            )
        return genai.Client(api_key=api_key)

    @staticmethod
    def preparar_imagen(image_bytes: bytes, mime_type: str) -> tuple[bytes, str]:
        """Valida, redimensiona y normaliza la imagen para la API."""
        if len(image_bytes) > MAX_IMAGE_SIZE_BYTES:
            raise ValueError(
                f"La imagen supera el tamaño máximo de {MAX_IMAGE_SIZE_BYTES // (1024 * 1024)} MB."
            )

        try:
            img = Image.open(BytesIO(image_bytes))
            img.verify()
            img = Image.open(BytesIO(image_bytes))
        except Exception as exc:
            raise ValueError("El archivo no es una imagen válida.") from exc

        if img.mode in ("RGBA", "P"):
            img = img.convert("RGB")

        max_dim = max(img.size)
        if max_dim > MAX_IMAGE_DIMENSION:
            ratio = MAX_IMAGE_DIMENSION / max_dim
            new_size = (int(img.width * ratio), int(img.height * ratio))
            img = img.resize(new_size, Image.Resampling.LANCZOS)

        output = BytesIO()
        fmt = "JPEG"
        out_mime = "image/jpeg"
        if mime_type == "image/png":
            fmt = "PNG"
            out_mime = "image/png"
        elif mime_type == "image/webp":
            fmt = "WEBP"
            out_mime = "image/webp"

        img.save(output, format=fmt, quality=90)
        return output.getvalue(), out_mime

    @staticmethod
    def _parse_json_response(text: str) -> IdentificacionGemini:
        cleaned = text.strip()
        if cleaned.startswith("```"):
            cleaned = re.sub(r"^```(?:json)?\s*", "", cleaned)
            cleaned = re.sub(r"\s*```$", "", cleaned)

        try:
            data = json.loads(cleaned)
        except json.JSONDecodeError as exc:
            raise ValueError("Gemini no devolvió un JSON válido.") from exc

        return IdentificacionGemini.model_validate(data)

    @staticmethod
    def identificar(image_bytes: bytes, mime_type: str) -> IdentificacionGemini:
        """Envía la imagen a Gemini y devuelve la identificación estructurada."""
        prepared_bytes, prepared_mime = GeminiService.preparar_imagen(
            image_bytes, mime_type
        )
        client = GeminiService._get_client()

        image_part = types.Part.from_bytes(
            data=prepared_bytes, mime_type=prepared_mime
        )

        max_attempts = 4
        last_error: Exception | None = None
        for attempt in range(max_attempts):
            try:
                response = client.models.generate_content(
                    model=get_gemini_model(),
                    contents=[PROMPT, image_part],
                    config=types.GenerateContentConfig(
                        temperature=0.2,
                        response_mime_type="application/json",
                    ),
                )
                text = response.text
                if not text:
                    raise ValueError("Gemini no devolvió ninguna respuesta.")
                return GeminiService._parse_json_response(text)
            except Exception as exc:
                last_error = exc
                es_ultimo = attempt == max_attempts - 1
                # Solo reintentamos errores temporales (503 sobrecarga, 429, red).
                if not es_ultimo and GeminiService._is_transient(exc):
                    time.sleep(1.5 * (2**attempt))  # 1.5s, 3s, 6s
                    continue
                raise ValueError(
                    f"Error al identificar la planta: {GeminiService._friendly_error(exc)}"
                ) from exc

        raise ValueError(
            f"Error al identificar la planta: {GeminiService._friendly_error(last_error or Exception('Error desconocido'))}"
        )
