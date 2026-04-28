package com.example.blindspotnews.backend

import android.graphics.Bitmap
import com.example.blindspotnews.ui.BiasIssue

//object AnalysisResultStore {
//    var lastResult: AnalysisResult? = null
//}

data class ImageIssue(
    val id: String,
    val frameIndex: Int,
    val type: String,
    val explanation: String
)

data class AnalysisResponse(
    val text: String,
    val issues: List<BiasIssue>,
    val image_issues: List<ImageIssue>,
    val frames: List<String>, // base64 images
    val overall_analysis: String,
    val bias_score: Int,
    val alignment: String
)

data class ImageIssueUI(
    val bitmap: Bitmap,
    val explanation: String,
    val type: String
)