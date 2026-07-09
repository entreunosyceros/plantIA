"""
Rutas FastAPI: páginas HTML y API REST.
"""

from __future__ import annotations

from pathlib import Path

from fastapi import APIRouter, BackgroundTasks, File, Form, HTTPException, Query, Request, UploadFile
from fastapi.responses import HTMLResponse, RedirectResponse
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates

from plantia.config import (
    ALLOWED_IMAGE_TYPES,
    AVAILABLE_GEMINI_MODELS,
    PAGE_SIZE,
    UPLOADS_DIR,
    ensure_directories,
    get_gemini_api_key,
    get_gemini_model,
    save_gemini_config,
)
from plantia.database import init_database
from plantia.schemas.planta import PlantaListResponse, PlantaResponse, PlantaUpdate
from plantia.services.gemini_service import GeminiService
from plantia.services.planta_service import PlantaService
from plantia.utils.jinja_filters import detalle_json_fields

PACKAGE_DIR = Path(__file__).resolve().parent.parent
TEMPLATES_DIR = PACKAGE_DIR / "templates"
STATIC_DIR = PACKAGE_DIR / "static"

router = APIRouter()
templates = Jinja2Templates(directory=str(TEMPLATES_DIR))
templates.env.globals["image_url"] = lambda path: f"/uploads/{Path(path).name}"


def _contexto_detalle(planta, *, mensaje: str | None, focus_cuaderno: bool, luna) -> dict:
    return {
        "planta": planta,
        "mensaje": mensaje,
        "focus_cuaderno": focus_cuaderno,
        "luna": luna,
        **detalle_json_fields(planta),
    }


def _planta_to_response(planta) -> PlantaResponse:
    return PlantaResponse.model_validate(planta)


def _pagina_lista(pagina, q: str) -> PlantaListResponse:
    return PlantaListResponse(
        items=[_planta_to_response(p) for p in pagina.items],
        total=pagina.total,
        page=pagina.page,
        page_size=pagina.page_size,
        total_pages=pagina.total_pages,
    )


async def _leer_imagen(file: UploadFile) -> tuple[bytes, str]:
    if file.content_type not in ALLOWED_IMAGE_TYPES:
        raise HTTPException(
            status_code=400,
            detail="Formato no permitido. Usa JPEG, PNG o WebP.",
        )
    content = await file.read()
    if not content:
        raise HTTPException(status_code=400, detail="El archivo está vacío.")
    return content, file.content_type or "image/jpeg"


# --- Páginas HTML ---


@router.get("/", response_class=HTMLResponse)
async def pagina_inicio(request: Request):
    return templates.TemplateResponse(
        request,
        "index.html",
        {"planta": None, "error": None, "mensaje": None, "focus_cuaderno": False},
    )


@router.post("/salir")
async def salir(background_tasks: BackgroundTasks):
    """
    Cierra el proceso del servidor para liberar el puerto.
    Se ejecuta como background task para poder responder antes del shutdown.
    """

    def _shutdown() -> None:
        import os
        import signal

        os.kill(os.getpid(), signal.SIGTERM)

    background_tasks.add_task(_shutdown)
    return HTMLResponse("Cerrando PlantIA…")


def _contexto_configuracion(
    *,
    mensaje: str | None = None,
    error: str | None = None,
) -> dict:
    has_key = bool(get_gemini_api_key())
    return {
        "mensaje": mensaje,
        "error": error,
        "has_key": has_key,
        "masked_key": ("*" * 8) if has_key else "",
        "gemini_model": get_gemini_model(),
        "available_models": AVAILABLE_GEMINI_MODELS,
    }


@router.get("/configuracion", response_class=HTMLResponse)
async def pagina_configuracion(
    request: Request,
    mensaje: str = Query("", alias="mensaje"),
):
    return templates.TemplateResponse(
        request,
        "configuracion.html",
        _contexto_configuracion(mensaje=mensaje or None),
    )


@router.post("/configuracion", response_class=HTMLResponse)
async def guardar_configuracion(
    request: Request,
    gemini_api_key: str = Form(""),
    gemini_model: str = Form(...),
):
    try:
        save_gemini_config(
            api_key=gemini_api_key or None,
            model=gemini_model,
        )
        return RedirectResponse(
            url="/configuracion?mensaje=Configuración+guardada+correctamente",
            status_code=303,
        )
    except ValueError as exc:
        return templates.TemplateResponse(
            request,
            "configuracion.html",
            _contexto_configuracion(error=str(exc)),
            status_code=400,
        )


@router.post("/identificar", response_class=HTMLResponse)
async def identificar_planta(request: Request, file: UploadFile = File(...)):
    try:
        content, mime_type = await _leer_imagen(file)
        identificacion = GeminiService.identificar(content, mime_type)
        ext = PlantaService.extension_desde_mime(mime_type)
        image_path = PlantaService.guardar_imagen(content, ext)
        planta = PlantaService.crear_desde_identificacion(identificacion, image_path)
        return templates.TemplateResponse(
            request,
            "index.html",
            {
                "planta": planta,
                "error": None,
                "mensaje": None,
                "focus_cuaderno": True,
            },
        )
    except ValueError as exc:
        return templates.TemplateResponse(
            request,
            "index.html",
            {"planta": None, "error": str(exc)},
            status_code=400,
        )
    except Exception as exc:
        return templates.TemplateResponse(
            request,
            "index.html",
            {"planta": None, "error": f"Error inesperado: {exc}"},
            status_code=500,
        )


@router.get("/plantas", response_class=HTMLResponse)
async def pagina_historial(
    request: Request,
    q: str = Query("", alias="q"),
    page: int = Query(1, ge=1),
    orden: str = Query("recientes", alias="orden"),
    mensaje: str = Query("", alias="mensaje"),
):
    pagina = PlantaService.listar(termino=q, page=page, orden=orden)
    if orden not in PlantaService.ORDENES_VALIDOS:
        orden = "recientes"
    return templates.TemplateResponse(
        request,
        "historial.html",
        {
            "plantas": pagina.items,
            "q": q,
            "orden": orden,
            "page": pagina.page,
            "total": pagina.total,
            "total_pages": pagina.total_pages,
            "page_size": pagina.page_size,
            "mensaje": mensaje or None,
        },
    )


@router.get("/plantas/{planta_id}", response_class=HTMLResponse)
async def pagina_detalle(request: Request, planta_id: int):
    from plantia.utils.moon import moon_info

    planta = PlantaService.obtener_por_id(planta_id)
    if planta is None:
        raise HTTPException(status_code=404, detail="Planta no encontrada.")
    return templates.TemplateResponse(
        request,
        "detalle.html",
        _contexto_detalle(
            planta,
            mensaje=None,
            focus_cuaderno=False,
            luna=moon_info(),
        ),
    )


@router.post("/plantas/{planta_id}/notas", response_class=HTMLResponse)
async def guardar_notas(
    request: Request,
    planta_id: int,
    notas_usuario: str = Form(""),
    redirect: str = Form("detalle"),
):
    from plantia.utils.moon import moon_info

    planta = PlantaService.actualizar_notas(planta_id, notas_usuario)
    if planta is None:
        raise HTTPException(status_code=404, detail="Planta no encontrada.")
    mensaje = "Anotación guardada en tu cuaderno."
    contexto = {
        "planta": planta,
        "mensaje": mensaje,
        "focus_cuaderno": True,
    }
    if redirect == "index":
        return templates.TemplateResponse(
            request,
            "index.html",
            {**contexto, "error": None},
        )
    return templates.TemplateResponse(
        request,
        "detalle.html",
        _contexto_detalle(
            planta,
            mensaje=mensaje,
            focus_cuaderno=True,
            luna=moon_info(),
        ),
    )


@router.post("/plantas/{planta_id}/eliminar")
async def eliminar_planta(planta_id: int):
    if not PlantaService.eliminar(planta_id):
        raise HTTPException(status_code=404, detail="Planta no encontrada.")
    return RedirectResponse(
        url="/plantas?mensaje=Planta+eliminada+correctamente",
        status_code=303,
    )


# --- API REST ---


@router.post("/api/plantas/identificar", response_model=PlantaResponse)
async def api_identificar(file: UploadFile = File(...)):
    try:
        content, mime_type = await _leer_imagen(file)
        identificacion = GeminiService.identificar(content, mime_type)
        ext = PlantaService.extension_desde_mime(mime_type)
        image_path = PlantaService.guardar_imagen(content, ext)
        planta = PlantaService.crear_desde_identificacion(identificacion, image_path)
        return _planta_to_response(planta)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@router.get("/api/plantas", response_model=PlantaListResponse)
async def api_listar(
    q: str = Query("", alias="q"),
    page: int = Query(1, ge=1),
    page_size: int = Query(PAGE_SIZE, ge=1, le=100),
):
    pagina = PlantaService.listar(termino=q, page=page, page_size=page_size)
    return _pagina_lista(pagina, q)


@router.get("/api/plantas/{planta_id}", response_model=PlantaResponse)
async def api_detalle(planta_id: int):
    planta = PlantaService.obtener_por_id(planta_id)
    if planta is None:
        raise HTTPException(status_code=404, detail="Planta no encontrada.")
    return _planta_to_response(planta)


@router.patch("/api/plantas/{planta_id}", response_model=PlantaResponse)
async def api_actualizar(planta_id: int, datos: PlantaUpdate):
    if datos.notas_usuario is None:
        raise HTTPException(status_code=400, detail="No hay campos para actualizar.")
    planta = PlantaService.actualizar_notas(planta_id, datos.notas_usuario)
    if planta is None:
        raise HTTPException(status_code=404, detail="Planta no encontrada.")
    return _planta_to_response(planta)


@router.delete("/api/plantas/{planta_id}", status_code=204)
async def api_eliminar(planta_id: int):
    if not PlantaService.eliminar(planta_id):
        raise HTTPException(status_code=404, detail="Planta no encontrada.")


def create_app():
    """Crea y configura la aplicación FastAPI."""
    from fastapi import FastAPI

    ensure_directories()
    init_database()

    app = FastAPI(
        title="PlantIA",
        description="Identificación de plantas con Gemini",
        version="0.1.0",
    )
    app.include_router(router)
    app.mount("/static", StaticFiles(directory=str(STATIC_DIR)), name="static")
    app.mount("/uploads", StaticFiles(directory=str(UPLOADS_DIR)), name="uploads")

    return app
