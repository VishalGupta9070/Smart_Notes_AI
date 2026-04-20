package com.example.smartnotesai.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface GeminiApiService {
    @POST("v1beta/{model}:generateContent")
    suspend fun generateContent(
        @Path(value = "model", encoded = true) model: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>
}
