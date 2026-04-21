package com.example.blindspotnews.backend

import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class Api {

    private val functions = Firebase.functions

    suspend fun analyzeVideoOrArticle(
        url: String,
        isVideo: Boolean
    ): AnalysisResult {

        val data = hashMapOf(
            "url" to url,
            "isVideo" to isVideo
        )

        try {
            val result = functions
                .getHttpsCallable("analyze_url")
                .call(data)
                .await()

            println(result.getData())

            val raw = result.getData() as? Map<*, *>
                ?: throw Exception("Unexpected response format")

            return AnalysisResult(
                text = raw["text"]?.toString() ?: "",
                issues = raw["issues"]?.toString() ?: "",
                imageIssues = raw["image_issues"]?.toString() ?: "",
                summary = raw["summary"]?.toString() ?: "",
                biasScore = raw["bias_score"]?.toString() ?: "",
                alignment = raw["alignment"]?.toString() ?: ""
            )

        } catch (e: Exception) {
            return AnalysisResult(
                text = "",
                issues = "",
                imageIssues = "",
                summary = "",
                biasScore = "",
                alignment = ""
            )
        }
    }
}


data class AnalysisResult(
    val text: String,
    val issues: String,
    val imageIssues: String,
    val summary: String,
    val biasScore: String,
    val alignment: String
)