from __future__ import annotations

from datetime import datetime

from sqlalchemy import DateTime, Index, String, Text, func
from sqlalchemy.orm import Mapped, mapped_column

from plantia.models.base import Base


class Planta(Base):
    """Registro de una planta identificada."""

    __tablename__ = "plantas"
    __table_args__ = (
        Index("ix_plantas_nombre_comun", "nombre_comun"),
        Index("ix_plantas_nombre_cientifico", "nombre_cientifico"),
    )

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime, server_default=func.now(), nullable=False
    )
    image_path: Mapped[str] = mapped_column(String(512), nullable=False)

    nombre_comun: Mapped[str] = mapped_column(String(255), nullable=False)
    nombre_cientifico: Mapped[str] = mapped_column(String(255), nullable=False, default="")
    familia: Mapped[str] = mapped_column(String(255), nullable=False, default="")
    tipo: Mapped[str] = mapped_column(String(128), nullable=False, default="")
    descripcion: Mapped[str] = mapped_column(Text, nullable=False, default="")

    riego: Mapped[str] = mapped_column(Text, nullable=False, default="")
    luz: Mapped[str] = mapped_column(Text, nullable=False, default="")
    temperatura: Mapped[str] = mapped_column(Text, nullable=False, default="")
    humedad: Mapped[str] = mapped_column(Text, nullable=False, default="")
    suelo: Mapped[str] = mapped_column(Text, nullable=False, default="")
    fertilizacion: Mapped[str] = mapped_column(Text, nullable=False, default="")
    problemas_comunes: Mapped[str] = mapped_column(Text, nullable=False, default="")
    plagas_habituales: Mapped[str] = mapped_column(Text, nullable=False, default="")

    toxicidad_perros: Mapped[str] = mapped_column(Text, nullable=False, default="")
    toxicidad_gatos: Mapped[str] = mapped_column(Text, nullable=False, default="")
    dificultad: Mapped[str] = mapped_column(String(64), nullable=False, default="")
    tamano_adulto: Mapped[str] = mapped_column(Text, nullable=False, default="")
    crecimiento: Mapped[str] = mapped_column(Text, nullable=False, default="")
    floracion: Mapped[str] = mapped_column(Text, nullable=False, default="")
    epoca_poda: Mapped[str] = mapped_column(Text, nullable=False, default="")
    epoca_trasplante: Mapped[str] = mapped_column(Text, nullable=False, default="")
    senales_trasplante: Mapped[str] = mapped_column(Text, nullable=False, default="")
    maceta_y_sustrato: Mapped[str] = mapped_column(Text, nullable=False, default="")

    # Ayudas para mejor identificación / onboarding
    taxonomia_reino: Mapped[str] = mapped_column(String(128), nullable=False, default="")
    taxonomia_orden: Mapped[str] = mapped_column(String(128), nullable=False, default="")
    taxonomia_genero: Mapped[str] = mapped_column(String(128), nullable=False, default="")
    taxonomia_especie: Mapped[str] = mapped_column(String(128), nullable=False, default="")
    rasgos_observados_json: Mapped[str] = mapped_column(Text, nullable=False, default="[]")
    preguntas_para_mejorar_json: Mapped[str] = mapped_column(Text, nullable=False, default="[]")
    candidatos_json: Mapped[str] = mapped_column(Text, nullable=False, default="[]")
    guia_inicio_json: Mapped[str] = mapped_column(Text, nullable=False, default="{}")

    confianza: Mapped[str] = mapped_column(String(16), nullable=False, default="medio")
    notas_usuario: Mapped[str] = mapped_column(Text, nullable=False, default="")
