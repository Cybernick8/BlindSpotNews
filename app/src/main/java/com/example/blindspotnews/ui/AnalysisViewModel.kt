package com.example.blindspotnews.ui

import androidx.lifecycle.ViewModel
import com.example.blindspotnews.backend.AnalysisResult
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class AnalysisViewModel : ViewModel() {

    // This creates a direct connection to the Firestore Database
    // The ViewModel now has the database connection
    private val db = Firebase.firestore

    // A function our UI calls when an article is analyzed
    fun saveArticleData(analysis: AnalysisResult) {

        val article = hashMapOf(
            "url" to analysis.url,
            "text" to analysis.analyzedText,
            "overallAnalysis" to analysis.overallAnalysis,
            "biasRating" to analysis.biasRating,
            "alignment" to analysis.alignment,
            "issues" to analysis.detectedIssues,
            "imageIssues" to analysis.imageIssues,
            "createdAt" to analysis.createdAt
        )
        // Send it to the "analyzed_articles" collection
        db.collection("analyzed_articles").add(article)
            .addOnSuccessListener { documentReference ->
                println("SUCCESS: Article saved to Firestore with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                println("ERROR: Failed to save article: $e")
            }
    }
}