package com.plantia.api

import com.plantia.api.models.PlantaListResponse
import com.plantia.api.models.PlantaResponse
import com.plantia.api.models.PlantaUpdateRequest
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface PlantIAApi {
    @Multipart
    @POST("api/plantas/identificar")
    suspend fun identificar(
        @Part file: MultipartBody.Part
    ): PlantaResponse

    @GET("api/plantas")
    suspend fun listar(
        @Query("q") q: String = "",
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 24,
        @Query("orden") orden: String = "recientes",
    ): PlantaListResponse

    @GET("api/plantas/{id}")
    suspend fun detalle(@Path("id") id: Int): PlantaResponse

    @PATCH("api/plantas/{id}")
    @Headers("Content-Type: application/json; charset=UTF-8")
    suspend fun actualizar(@Path("id") id: Int, @Body body: PlantaUpdateRequest): PlantaResponse

    @POST("api/plantas/{id}/notas")
    @Headers("Content-Type: application/json; charset=UTF-8")
    suspend fun guardarNotas(@Path("id") id: Int, @Body body: PlantaUpdateRequest): PlantaResponse

    @DELETE("api/plantas/{id}")
    suspend fun eliminar(@Path("id") id: Int)
}

