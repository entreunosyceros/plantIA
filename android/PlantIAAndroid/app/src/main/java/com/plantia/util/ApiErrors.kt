package com.plantia.util

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.HttpException
import java.io.IOException

@JsonClass(generateAdapter = false)
data class ApiErrorBody(
    val detail: String? = null,
)

private val errorMoshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

private val errorAdapter = errorMoshi.adapter(ApiErrorBody::class.java)

fun Throwable.userMessage(default: String): String {
    return when (this) {
        is HttpException -> {
            val raw = response()?.errorBody()?.string()
            if (!raw.isNullOrBlank()) {
                runCatching { errorAdapter.fromJson(raw)?.detail }.getOrNull()
                    ?: raw.take(200)
            } else {
                "Error HTTP ${code()}"
            }
        }
        is IOException -> "Sin conexión con el servidor. Comprueba la red Wi‑Fi."
        else -> message?.takeIf { it.isNotBlank() } ?: default
    }
}
