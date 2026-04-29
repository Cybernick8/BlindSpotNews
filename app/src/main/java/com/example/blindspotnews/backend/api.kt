package com.example.blindspotnews.backend

import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import com.google.gson.Gson

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
            // Call the Firebase Function
            val result = functions
                .getHttpsCallable("analyze_url")
                .call(data)
                .await()

            // Grab the raw Map that Firebase automatically created
            val rawData = result.getData()

            // Convert that Map directly into a perfect JSON String!
            val jsonString = Gson().toJson(rawData)

            // Print it to your Android Studio console so you can still debug it
            println("API RETURNED JSON: $jsonString")

            // 4. Return the pure JSON string to the ViewModel
            jsonString

        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}