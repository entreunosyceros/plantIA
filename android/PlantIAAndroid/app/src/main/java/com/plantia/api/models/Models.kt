package com.plantia.api.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class PlantaResponse(
    val id: Int,
    val created_at: String,
    val image_path: String,
    val nombre_comun: String,
    val nombre_cientifico: String,
    val familia: String,
    val tipo: String,
    val descripcion: String,
    val riego: String,
    val luz: String,
    val temperatura: String,
    val humedad: String,
    val suelo: String,
    val fertilizacion: String,
    val problemas_comunes: String,
    val plagas_habituales: String,
    val toxicidad_perros: String,
    val toxicidad_gatos: String,
    val dificultad: String,
    val tamano_adulto: String,
    val crecimiento: String,
    val floracion: String,
    val epoca_poda: String,
    val epoca_trasplante: String,
    val senales_trasplante: String,
    val maceta_y_sustrato: String,
    val taxonomia_reino: String,
    val taxonomia_orden: String,
    val taxonomia_genero: String,
    val taxonomia_especie: String,
    val rasgos_observados_json: String,
    val preguntas_para_mejorar_json: String,
    val candidatos_json: String,
    val guia_inicio_json: String,
    val confianza: String,
    val notas_usuario: String,
)

@JsonClass(generateAdapter = false)
data class PlantaListResponse(
    val items: List<PlantaResponse>,
    val total: Int,
    val page: Int,
    val page_size: Int,
    val total_pages: Int,
)

@JsonClass(generateAdapter = false)
data class PlantaUpdateRequest(
    @Json(name = "notas_usuario") val notas_usuario: String,
)
