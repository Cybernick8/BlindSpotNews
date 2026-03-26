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

            println(result.getData())

            val raw = result.getData() as? Map<*, *>
                ?: return "Unexpected response format"

            val textPreview = raw["text"]?.toString()?.take(50) ?: "null"
            val issuesPreview = raw["issues"]?.toString()?.take(50) ?: "null"
            val imgIssuesPreview = raw["image_issues"]?.toString()?.take(50) ?: "null"
            val biasPreview = raw["bias_score"]?.toString()?.take(50) ?: "null"
            val alignPreview = raw["alignment"]?.toString()?.take(50) ?: "null"

            val debugOutput = """
                TEXT: $textPreview
                ISSUES: $issuesPreview
                IMAGE ISSUES: $imgIssuesPreview
                BIAS_SCORE: $biasPreview
                ALIGNMENT: $alignPreview
                """.trimIndent()

            println(debugOutput)

            debugOutput
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
