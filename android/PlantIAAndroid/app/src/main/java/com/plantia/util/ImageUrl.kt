package com.plantia.util

fun imageUrlFromPath(imagePath: String, baseUrl: String): String {
    val filename = imagePath.substringAfterLast("/")
    return baseUrl + "uploads/" + filename
}

fun wikipediaUrl(nombreCientifico: String?): String? {
    val name = nombreCientifico?.trim().orEmpty()
    if (name.isBlank()) return null
    val title = name.replace(" ", "_")
    return "https://en.wikipedia.org/wiki/${java.net.URLEncoder.encode(title, "UTF-8").replace("+", "%20")}"
}
