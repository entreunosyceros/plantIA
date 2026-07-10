package com.plantia.api

import android.content.Context
import com.plantia.data.ServerPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object PlantIAClient {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        )
        .build()

    @Volatile
    private var cachedBaseUrl: String? = null

    @Volatile
    private var cachedApi: PlantIAApi? = null

    fun baseUrl(context: Context): String = ServerPreferences(context).baseUrl

    fun api(context: Context): PlantIAApi {
        val url = baseUrl(context)
        val current = cachedApi
        if (current != null && cachedBaseUrl == url) return current
        synchronized(this) {
            if (cachedApi != null && cachedBaseUrl == url) return cachedApi!!
            val retrofit = Retrofit.Builder()
                .baseUrl(url)
                .client(okHttp)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
            cachedBaseUrl = url
            cachedApi = retrofit.create(PlantIAApi::class.java)
            return cachedApi!!
        }
    }

    fun invalidate() {
        synchronized(this) {
            cachedBaseUrl = null
            cachedApi = null
        }
    }
}
