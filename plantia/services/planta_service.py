"""
Servicio de gestión de plantas identificadas.
"""

from __future__ import annotations

import json
import uuid
from dataclasses import dataclass
from pathlib import Path

from sqlalchemy import func, or_, select

from plantia.config import PAGE_SIZE, UPLOADS_DIR, ensure_directories
from plantia.database import session_scope
from plantia.models import Planta
from plantia.schemas.planta import IdentificacionGemini, PlantaCreate
from plantia.services.candidate_image_service import get_thumbnail_url


@dataclass
class PaginaPlantas:
    items: list[Planta]
    total: int
    page: int
    page_size: int

    @property
    def total_pages(self) -> int:
        if self.total == 0:
            return 1
        return (self.total + self.page_size - 1) // self.page_size


class PlantaService:
    """Operaciones CRUD sobre plantas identificadas."""

    @staticmethod
    def guardar_imagen(image_bytes: bytes, extension: str) -> str:
        """Guarda la imagen en disco y devuelve la ruta absoluta."""
        ensure_directories()
        ext = extension.lstrip(".").lower()
        if ext not in ("jpg", "jpeg", "png", "webp"):
            ext = "jpg"
        filename = f"{uuid.uuid4().hex}.{ext}"
        path = UPLOADS_DIR / filename
        path.write_bytes(image_bytes)
        return str(path)

    @staticmethod
    def crear_desde_identificacion(
        identificacion: IdentificacionGemini, image_path: str
    ) -> Planta:
        # Enriquecer candidatos con miniaturas (si es posible)
        candidatos = []
        for cand in identificacion.candidatos:
            if not cand.thumbnail_url and cand.nombre_cientifico:
                cand.thumbnail_url = get_thumbnail_url(cand.nombre_cientifico)
            candidatos.append(cand.model_dump())

        datos = PlantaCreate(
            nombre_comun=identificacion.nombre_comun,
            nombre_cientifico=identificacion.nombre_cientifico,
            familia=identificacion.familia or identificacion.taxonomia.familia,
            tipo=identificacion.tipo,
            descripcion=identificacion.descripcion,
            riego=identificacion.cuidados.riego,
            luz=identificacion.cuidados.luz,
            temperatura=identificacion.cuidados.temperatura,
            humedad=identificacion.cuidados.humedad,
            suelo=identificacion.cuidados.suelo,
            fertilizacion=identificacion.cuidados.fertilizacion,
            problemas_comunes=identificacion.cuidados.problemas_comunes,
            plagas_habituales=identificacion.cuidados.plagas_habituales,
            toxicidad_perros=identificacion.toxicidad.perros,
            toxicidad_gatos=identificacion.toxicidad.gatos,
            dificultad=identificacion.caracteristicas.dificultad,
            tamano_adulto=identificacion.caracteristicas.tamano_adulto,
            crecimiento=identificacion.caracteristicas.crecimiento,
            floracion=identificacion.caracteristicas.floracion,
            epoca_poda=identificacion.caracteristicas.epoca_poda,
            epoca_trasplante=identificacion.caracteristicas.epoca_trasplante,
            senales_trasplante=identificacion.caracteristicas.senales_trasplante,
            maceta_y_sustrato=identificacion.caracteristicas.maceta_y_sustrato,
            taxonomia_reino=identificacion.taxonomia.reino,
            taxonomia_orden=identificacion.taxonomia.orden,
            taxonomia_genero=identificacion.taxonomia.genero,
            taxonomia_especie=identificacion.taxonomia.especie,
            rasgos_observados_json=json.dumps(
                identificacion.rasgos_observados, ensure_ascii=False
            ),
            preguntas_para_mejorar_json=json.dumps(
                identificacion.preguntas_para_mejorar, ensure_ascii=False
            ),
            candidatos_json=json.dumps(candidatos, ensure_ascii=False),
            guia_inicio_json=json.dumps(
                identificacion.guia_inicio.model_dump(), ensure_ascii=False
            ),
            confianza=identificacion.confianza,
            image_path=image_path,
        )
        return PlantaService.crear(datos)

    @staticmethod
    def crear(datos: PlantaCreate) -> Planta:
        with session_scope() as session:
            planta = Planta(
                image_path=datos.image_path,
                nombre_comun=datos.nombre_comun.strip(),
                nombre_cientifico=datos.nombre_cientifico.strip(),
                familia=datos.familia.strip(),
                tipo=datos.tipo.strip(),
                descripcion=datos.descripcion.strip(),
                riego=datos.riego.strip(),
                luz=datos.luz.strip(),
                temperatura=datos.temperatura.strip(),
                humedad=datos.humedad.strip(),
                suelo=datos.suelo.strip(),
                fertilizacion=datos.fertilizacion.strip(),
                problemas_comunes=datos.problemas_comunes.strip(),
                plagas_habituales=datos.plagas_habituales.strip(),
                toxicidad_perros=datos.toxicidad_perros.strip(),
                toxicidad_gatos=datos.toxicidad_gatos.strip(),
                dificultad=datos.dificultad.strip(),
                tamano_adulto=datos.tamano_adulto.strip(),
                crecimiento=datos.crecimiento.strip(),
                floracion=datos.floracion.strip(),
                epoca_poda=datos.epoca_poda.strip(),
                epoca_trasplante=datos.epoca_trasplante.strip(),
                senales_trasplante=datos.senales_trasplante.strip(),
                maceta_y_sustrato=datos.maceta_y_sustrato.strip(),
                taxonomia_reino=datos.taxonomia_reino.strip(),
                taxonomia_orden=datos.taxonomia_orden.strip(),
                taxonomia_genero=datos.taxonomia_genero.strip(),
                taxonomia_especie=datos.taxonomia_especie.strip(),
                rasgos_observados_json=datos.rasgos_observados_json.strip() or "[]",
                preguntas_para_mejorar_json=datos.preguntas_para_mejorar_json.strip()
                or "[]",
                candidatos_json=datos.candidatos_json.strip() or "[]",
                guia_inicio_json=datos.guia_inicio_json.strip() or "{}",
                confianza=datos.confianza.strip(),
            )
            session.add(planta)
            session.flush()
            session.refresh(planta)
            return planta

    @staticmethod
    def obtener_por_id(planta_id: int) -> Planta | None:
        with session_scope() as session:
            return session.get(Planta, planta_id)

    @staticmethod
    def listar(
        termino: str = "", page: int = 1, page_size: int = PAGE_SIZE
    ) -> PaginaPlantas:
        page = max(1, page)
        texto = termino.strip()

        with session_scope() as session:
            query = select(Planta)
            count_query = select(func.count()).select_from(Planta)

            if texto:
                patron = f"%{texto}%"
                filtro = or_(
                    Planta.nombre_comun.ilike(patron),
                    Planta.nombre_cientifico.ilike(patron),
                    Planta.familia.ilike(patron),
                    Planta.tipo.ilike(patron),
                )
                query = query.where(filtro)
                count_query = count_query.where(filtro)

            total = session.scalar(count_query) or 0
            offset = (page - 1) * page_size
            items = list(
                session.scalars(
                    query.order_by(Planta.created_at.desc())
                    .offset(offset)
                    .limit(page_size)
                )
            )
            return PaginaPlantas(
                items=items, total=total, page=page, page_size=page_size
            )

    @staticmethod
    def actualizar_notas(planta_id: int, notas: str) -> Planta | None:
        with session_scope() as session:
            planta = session.get(Planta, planta_id)
            if planta is None:
                return None
            planta.notas_usuario = notas.strip()
            session.flush()
            session.refresh(planta)
            return planta

    @staticmethod
    def eliminar(planta_id: int) -> bool:
        with session_scope() as session:
            planta = session.get(Planta, planta_id)
            if planta is None:
                return False
            image_path = Path(planta.image_path)
            session.delete(planta)
            session.flush()
            if image_path.exists():
                image_path.unlink()
            return True

    @staticmethod
    def extension_desde_mime(mime_type: str) -> str:
        mapping = {
            "image/jpeg": "jpg",
            "image/png": "png",
            "image/webp": "webp",
        }
        return mapping.get(mime_type, "jpg")
