package com.plantia.util

import android.content.Context
import android.net.Uri
import com.plantia.api.PlantIAClient
import com.plantia.api.models.PlantaResponse
import com.plantia.api.models.PlantaUpdateRequest
import com.plantia.reminders.WaterReminderStore
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

fun appendJournalLine(current: String, line: String): String {
    val trimmed = current.trimEnd()
    return if (trimmed.isBlank()) line else "$trimmed\n$line"
}

suspend fun markPlantWatered(context: Context, plant: PlantaResponse): PlantaResponse {
    val line = "Regada el ${formatJournalDate()}"
    val notas = appendJournalLine(plant.notas_usuario, line)
    val request = PlantaUpdateRequest(notas_usuario = notas)
    val updated = try {
        PlantIAClient.api(context).guardarNotas(plant.id, request)
    } catch (_: Exception) {
        PlantIAClient.api(context).actualizar(plant.id, request)
    }
    WaterReminderStore(context).markWatered(plant.id, plant.nombre_comun)
    return updated
}

suspend fun identifyImageFile(context: Context, file: File, mimeType: String = "image/jpeg"): PlantaResponse {
    val part = MultipartBody.Part.createFormData(
        "file",
        file.name,
        file.asRequestBody(mimeType.toMediaType()),
    )
    return PlantIAClient.api(context).identificar(part)
}

fun uriToTempFile(context: Context, uri: Uri): File {
    val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
    val ext = when {
        mime.contains("png") -> ".png"
        mime.contains("webp") -> ".webp"
        else -> ".jpg"
    }
    val file = File.createTempFile("plantia_gallery_", ext, context.cacheDir)
    context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(file).use { output -> input.copyTo(output) }
    } ?: error("No se pudo leer la imagen")
    return file
}

fun mimeTypeForFile(file: File): String = when (file.extension.lowercase()) {
    "png" -> "image/png"
    "webp" -> "image/webp"
    else -> "image/jpeg"
}
