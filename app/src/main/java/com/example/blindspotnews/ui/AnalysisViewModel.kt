package com.example.blindspotnews.ui

import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class AnalysisViewModel : ViewModel() {

    // This creates a direct connection to the Firestore Database
    // The ViewModel now has the database connection
    private val db = Firebase.firestore

    // A function our UI calls when an article is analyzed
    fun saveArticleData(text: String, issues: String, imageIssues: String, sourceUrl: String, biasRating: String, alignment: String) {

        // Maps the data to key-value pairs for Firestore
        val article = hashMapOf(
            "text" to text,
            "issues" to issues,
            "imageIssues" to imageIssues,
            "source_url" to sourceUrl,
            "bias_rating" to biasRating,
            "alignment" to alignment
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