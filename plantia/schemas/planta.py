from __future__ import annotations

from datetime import datetime
from typing import Literal

from pydantic import BaseModel, Field


class CuidadosSchema(BaseModel):
    riego: str = ""
    luz: str = ""
    temperatura: str = ""
    humedad: str = ""
    suelo: str = ""
    fertilizacion: str = ""
    problemas_comunes: str = ""
    plagas_habituales: str = ""


class CaracteristicasSchema(BaseModel):
    dificultad: str = ""
    tamano_adulto: str = ""
    crecimiento: str = ""
    floracion: str = ""
    epoca_poda: str = ""
    epoca_trasplante: str = ""
    senales_trasplante: str = ""
    maceta_y_sustrato: str = ""


class ToxicidadSchema(BaseModel):
    perros: str = ""
    gatos: str = ""


class TaxonomiaSchema(BaseModel):
    reino: str = ""
    orden: str = ""
    familia: str = ""
    genero: str = ""
    especie: str = ""


class CandidatoSchema(BaseModel):
    nombre_comun: str = ""
    nombre_cientifico: str = ""
    razon: str = ""
    confianza: Literal["alto", "medio", "bajo"] = "medio"
    thumbnail_url: str = ""


class GuiaInicioSchema(BaseModel):
    pasos: list[str] = Field(default_factory=list)
    errores_comunes: list[str] = Field(default_factory=list)


class IdentificacionGemini(BaseModel):
    nombre_comun: str
    nombre_cientifico: str = ""
    familia: str = ""
    tipo: str = ""
    descripcion: str = ""
    cuidados: CuidadosSchema = Field(default_factory=CuidadosSchema)
    caracteristicas: CaracteristicasSchema = Field(default_factory=CaracteristicasSchema)
    toxicidad: ToxicidadSchema = Field(default_factory=ToxicidadSchema)
    taxonomia: TaxonomiaSchema = Field(default_factory=TaxonomiaSchema)
    rasgos_observados: list[str] = Field(default_factory=list)
    preguntas_para_mejorar: list[str] = Field(default_factory=list)
    candidatos: list[CandidatoSchema] = Field(default_factory=list)
    guia_inicio: GuiaInicioSchema = Field(default_factory=GuiaInicioSchema)
    confianza: Literal["alto", "medio", "bajo"] = "medio"


class PlantaCreate(BaseModel):
    nombre_comun: str
    nombre_cientifico: str = ""
    familia: str = ""
    tipo: str = ""
    descripcion: str = ""
    riego: str = ""
    luz: str = ""
    temperatura: str = ""
    humedad: str = ""
    suelo: str = ""
    fertilizacion: str = ""
    problemas_comunes: str = ""
    plagas_habituales: str = ""
    toxicidad_perros: str = ""
    toxicidad_gatos: str = ""
    dificultad: str = ""
    tamano_adulto: str = ""
    crecimiento: str = ""
    floracion: str = ""
    epoca_poda: str = ""
    epoca_trasplante: str = ""
    senales_trasplante: str = ""
    maceta_y_sustrato: str = ""
    # Campos de ayuda a la identificación
    taxonomia_reino: str = ""
    taxonomia_orden: str = ""
    taxonomia_genero: str = ""
    taxonomia_especie: str = ""
    rasgos_observados_json: str = ""
    preguntas_para_mejorar_json: str = ""
    candidatos_json: str = ""
    guia_inicio_json: str = ""
    confianza: str = "medio"
    image_path: str


class PlantaUpdate(BaseModel):
    notas_usuario: str | None = None


class PlantaResponse(BaseModel):
    id: int
    created_at: datetime
    image_path: str
    nombre_comun: str
    nombre_cientifico: str
    familia: str
    tipo: str
    descripcion: str
    riego: str
    luz: str
    temperatura: str
    humedad: str
    suelo: str
    fertilizacion: str
    problemas_comunes: str
    plagas_habituales: str
    toxicidad_perros: str
    toxicidad_gatos: str
    dificultad: str
    tamano_adulto: str
    crecimiento: str
    floracion: str
    epoca_poda: str
    epoca_trasplante: str
    senales_trasplante: str
    maceta_y_sustrato: str
    taxonomia_reino: str
    taxonomia_orden: str
    taxonomia_genero: str
    taxonomia_especie: str
    rasgos_observados_json: str
    preguntas_para_mejorar_json: str
    candidatos_json: str
    guia_inicio_json: str
    confianza: str
    notas_usuario: str

    model_config = {"from_attributes": True}


class PlantaListResponse(BaseModel):
    items: list[PlantaResponse]
    total: int
    page: int
    page_size: int
    total_pages: int
