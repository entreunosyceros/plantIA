package com.plantia.domain

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@JsonClass(generateAdapter = false)
data class GuiaInicio(
    val pasos: List<String> = emptyList(),
    val errores_comunes: List<String> = emptyList(),
)

@JsonClass(generateAdapter = false)
data class Candidato(
    val nombre_comun: String? = null,
    val nombre_cientifico: String? = null,
    val razon: String? = null,
    val confianza: String? = null,
    val thumbnail_url: String? = null,
)

object JsonHelpers {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val listStringAdapter = moshi.adapter<List<String>>(
        com.squareup.moshi.Types.newParameterizedType(List::class.java, String::class.java)
    )

    private val guiaAdapter = moshi.adapter(GuiaInicio::class.java)
    private val candidatosAdapter = moshi.adapter<List<Candidato>>(
        com.squareup.moshi.Types.newParameterizedType(List::class.java, Candidato::class.java)
    )

    fun parseStringList(json: String): List<String> =
        runCatching { listStringAdapter.fromJson(json) }.getOrNull() ?: emptyList()

    fun parseGuia(json: String): GuiaInicio =
        runCatching { guiaAdapter.fromJson(json) }.getOrNull() ?: GuiaInicio()

    fun parseCandidatos(json: String): List<Candidato> =
        runCatching { candidatosAdapter.fromJson(json) }.getOrNull() ?: emptyList()
}

