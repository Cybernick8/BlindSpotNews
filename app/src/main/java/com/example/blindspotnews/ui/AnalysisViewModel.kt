package com.example.blindspotnews.ui

import androidx.lifecycle.ViewModel
import com.example.blindspotnews.backend.AnalysisResult
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import android.net.Uri
import android.util.Base64
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.UploadTask

class AnalysisViewModel : ViewModel() {

    private val db = Firebase.firestore

    fun saveArticleData(analysis: AnalysisResult) {

        // Use NON-KTX Storage
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference

        val frameUrls = mutableListOf<Map<String, Any>>()
        val frames = analysis.frames

        if (frames.isEmpty()) {
            saveToFirestore(analysis, frameUrls)
            return
        }

        var uploadedCount = 0

        frames.forEach { frame ->

            val imageRef = storageRef.child(
                "frames/${System.currentTimeMillis()}_${frame.index}.jpg"
            )

            val cleanBase64 = frame.url.substringAfter("base64,", frame.url)
            val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)

            imageRef.putBytes(bytes)
                .continueWithTask { task: Task<UploadTask.TaskSnapshot> ->
                    if (!task.isSuccessful) {
                        throw task.exception ?: Exception("Frame upload failed")
                    }
                    imageRef.downloadUrl
                }
                .addOnSuccessListener { uri: Uri ->

                    frameUrls.add(
                        mapOf(
                            "index" to frame.index,
                            "url" to uri.toString()
                        )
                    )

                    uploadedCount++

                    println("Upload success: ${uri}")

                    if (uploadedCount == frames.size) {
                        saveToFirestore(analysis, frameUrls)
                    }
                }
                .addOnFailureListener { e ->
                    println("Frame upload failed: $e")

                    // still count failure so app doesn't hang
                    uploadedCount++

                    if (uploadedCount == frames.size) {
                        saveToFirestore(analysis, frameUrls)
                    }
                }
        }
    }

    private fun saveToFirestore(
        analysis: AnalysisResult,
        frameUrls: List<Map<String, Any>>
    ) {

        val article = hashMapOf(
            "url" to analysis.url,
            "text" to analysis.analyzedText,
            "overallAnalysis" to analysis.overallAnalysis,
            "biasRating" to analysis.biasRating,
            "alignment" to analysis.alignment,
            "issues" to analysis.detectedIssues,
            "imageIssues" to analysis.imageIssues,
            "frames" to frameUrls,
            "createdAt" to analysis.createdAt
        )

        db.collection("analyzed_articles")
            .add(article)
            .addOnSuccessListener { doc ->
                println("SUCCESS: Saved with ID: ${doc.id}")
            }
            .addOnFailureListener { e ->
                println("ERROR: Firestore save failed: $e")
            }
    }
}