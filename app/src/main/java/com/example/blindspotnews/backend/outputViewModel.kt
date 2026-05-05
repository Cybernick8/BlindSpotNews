package com.example.blindspotnews.backend

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.example.blindspotnews.ui.BiasIssue

class OutputViewModel : ViewModel() {

    var isLoading by mutableStateOf(false)
        private set
    var outputText by mutableStateOf("Processing...")
        private set

    var overallAnalysis by mutableStateOf("")

    var urlInput by mutableStateOf("")

    var biasRating by mutableStateOf("")
        private set

    var alignment by mutableStateOf("")
        private set

    // Variables to hold the parsed text and the list of issues for UI highlight component
    var analyzedText by mutableStateOf("")
        private set
    var detectedIssues by mutableStateOf<List<BiasIssue>>(emptyList())
        private set

    var imageIssues by mutableStateOf<List<ImageIssue>>(emptyList())
        private set

    var analysisResult by mutableStateOf<AnalysisResult?>(null)
        private set

    fun analyze(url: String, isVideo: Boolean){
        isLoading = true
        outputText = "Loading..."
        analyzedText = "" // Clear old text before new search
        detectedIssues = emptyList() // Clear old highlights before new search
        imageIssues = emptyList()
        authenticateAndFetch(url, isVideo)
    }

    private fun authenticateAndFetch(url: String, isVideo: Boolean) {
        val auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            auth.signInAnonymously()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        auth.currentUser?.getIdToken(true)
                            ?.addOnSuccessListener {
                                fetchData(url, isVideo)
                            }
                    } else {
                        outputText = "Auth failed: ${task.exception?.message}"
                    }
                }
        } else {
            fetchData(url, isVideo)
        }
    }

    private fun fetchData(url: String, isVideo: Boolean) {
        viewModelScope.launch {
            // Move this variable outside the try block so the catch block can see it
            var rawResult = "Nothing returned"

            try {
                // Fetch the raw string from your API
                rawResult = Api().analyzeVideoOrArticle(url, isVideo)
                outputText = rawResult

                val gson = Gson()

                // If the API wrapped the JSON inside a string, unwrap it first
                val cleanJson = if (rawResult.trim().startsWith("\"")) {
                    gson.fromJson(rawResult, String::class.java)
                } else {
                    rawResult
                }

                // Now parse the clean JSON
                val parsedData = gson.fromJson(cleanJson, Map::class.java)

                // Extract properties safely
                analyzedText = parsedData["text"] as? String ?: "Unable to analyze. Try again later, or use a different source."
                overallAnalysis = parsedData["overall_analysis"] as? String ?: "No overall analysis provided."

                val parsedBias = (parsedData["bias_score"] as? Double)?.toInt() ?: 0
                biasRating = parsedBias.toString()

                alignment = parsedData["alignment"] as? String ?: "Center"
                val rawIssues: List<*> = parsedData["issues"] as? List<*> ?: emptyList<Any>()

                detectedIssues = rawIssues.mapNotNull { item ->
                    val issueMap = item as? Map<*, *> ?: return@mapNotNull null

                    try {
                        BiasIssue(
                            type = issueMap["type"] as? String ?: "Unknown",
                            start = (issueMap["start"] as? Double)?.toInt() ?: 0,
                            end = (issueMap["end"] as? Double)?.toInt() ?: 0,
                            explanation = issueMap["explanation"] as? String
                                ?: "No explanation provided."
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                val imageIssuesRaw: List<*> = parsedData["image_issues"] as? List<*> ?: emptyList<Any>()

                imageIssues = imageIssuesRaw.mapNotNull { item ->
                    val issue = item as? Map<*, *> ?: return@mapNotNull null

                    ImageIssue(
                        id = issue["id"] as? String ?: "",
                        frameIndex = when (val idx = issue["frame_index"] ?: issue["frameIndex"]) {
                            is Double -> idx.toInt()
                            is Int -> idx
                            else -> 0
                        },
                        type = issue["type"] as? String ?: "Unknown",
                        explanation = issue["explanation"] as? String ?: "No explanation provided."
                    )
                }


                val framesRaw: List<*> = parsedData["frames"] as? List<*> ?: emptyList<Any>()

                val frames = framesRaw.mapIndexedNotNull { index, item ->
                    when (item) {
                        is String -> {
                            FrameData(index, item)
                        }
                        is Map<*, *> -> {
                            val url = item["url"] as? String ?: return@mapIndexedNotNull null
                            FrameData(index, url)
                        }
                        else -> null
                    }
                }
                Log.d("FRAMES_DEBUG", frames.firstOrNull()?.url ?: "no frames")

                analysisResult = AnalysisResult(
                    url = url,
                    isVideo = isVideo,
                    analyzedText = analyzedText,
                    overallAnalysis = overallAnalysis,
                    biasRating = parsedBias,
                    alignment = alignment,
                    detectedIssues = detectedIssues,
                    imageIssues = imageIssues,
                    frames = frames
                )


            } catch (e: Exception) {
                outputText = "Error: ${e.message}"
                overallAnalysis = "We weren't able to analyze this link..."
                analyzedText = ""
                detectedIssues = emptyList()
                imageIssues = emptyList()
                analysisResult = null
            } finally {
                isLoading = false
            }
        }
    }

    fun isBase64(data: String): Boolean {
        return !data.startsWith("http")
    }

    fun decodeToBitmap(data: String): Bitmap? {
        return try {
            if (data.startsWith("http")) {
                // It's a URL → DO NOT decode as base64
                null
            } else {
                val bytes = Base64.decode(data, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        } catch (e: Exception) {
            null
        }
    }
}

data class AnalysisResult(
    val url: String = "",
    val isVideo: Boolean = false,
    val analyzedText: String = "",
    val overallAnalysis: String = "",
    val biasRating: Int = 0,
    val alignment: String = "",
    val detectedIssues: List<BiasIssue> = emptyList(),
    val imageIssues: List<ImageIssue> = emptyList(),
    val frames: List<FrameData> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class FrameData(
    val index: Int = 0,
    val url: String = ""
)