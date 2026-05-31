package com.example.moneypad.data.remote

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val PREFS_NAME = "moneypad_prefs"
    private const val KEY_BASE_URL = "base_url"
    private const val DEFAULT_URL = "https://example.trycloudflare.com/backend/"

    var baseUrl: String = DEFAULT_URL
        private set

    lateinit var apiService: MoneyPadApiService
        private set

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    fun initialize(context: Context, url: String) {
        // Normalize: ensure trailing slash
        baseUrl = if (url.endsWith("/")) url else "$url/"
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BASE_URL, baseUrl)
            .apply()
        rebuildClient()
    }

    fun getSavedUrl(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_BASE_URL, DEFAULT_URL) ?: DEFAULT_URL
    }

    private fun rebuildClient() {
        apiService = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MoneyPadApiService::class.java)
    }
}
