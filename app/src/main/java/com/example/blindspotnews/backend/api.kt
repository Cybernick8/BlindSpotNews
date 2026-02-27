package com.example.blindspotnews.backend

import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class Api {

    private val functions = Firebase.functions

    suspend fun analyzeVideoOrArticle(
        url: String,
        isVideo: Boolean
    ): String {

        val data = hashMapOf(
            "url" to url,
            "isVideo" to isVideo
        )

        return try {
            val result = functions
                .getHttpsCallable("analyze_url")
                .call(data)
                .await()

            val raw = result.getData()

            if (raw !is String) {
                return "Unexpected response format"
            }

            raw
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
