package com.example.blindspotnews.backend

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class OutputViewModel : ViewModel() {

    var analysisResult by mutableStateOf<AnalysisResult?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    fun analyze(url: String, isVideo: Boolean){
        isLoading = true
        analysisResult = null
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
                    }
                }
        } else {
            fetchData(url, isVideo)
        }
    }

    private fun fetchData(url: String, isVideo: Boolean) {
        viewModelScope.launch {
            try {
                val result = Api().analyzeVideoOrArticle(url, isVideo)

                analysisResult = result
                AnalysisResultStore.lastResult = result

            } catch (e: Exception) {

            } finally {
                isLoading = false
            }
        }
    }
}
