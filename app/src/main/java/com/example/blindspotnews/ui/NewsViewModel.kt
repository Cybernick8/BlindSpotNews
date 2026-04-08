package com.example.blindspotnews.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class HomeNewsArticle(
    val title: String = "",
    val imageUrl: String = "",
    val url: String = "",
    val source: String = ""
)

data class HomeNewsUiState(
    val isLoading: Boolean = false,
    val articles: List<HomeNewsArticle> = emptyList(),
    val error: String? = null
)

class NewsViewModel : ViewModel() {
    private val functions = FirebaseFunctions.getInstance()

    private val _uiState = MutableStateFlow(HomeNewsUiState())
    val uiState: StateFlow<HomeNewsUiState> = _uiState

    fun loadNews(topic: String, search: String) {
        viewModelScope.launch {
            _uiState.value = HomeNewsUiState(isLoading = true)

            try {
                val result = functions
                    .getHttpsCallable("get_home_news")
                    .call(
                        mapOf(
                            "topic" to topic,
                            "search" to search
                        )
                    )
                    .await()

                @Suppress("UNCHECKED_CAST")
                val data = result.getData() as? Map<String, Any?> ?: emptyMap()

                @Suppress("UNCHECKED_CAST")
                val rawArticles = data["articles"] as? List<Map<String, Any?>> ?: emptyList()

                val articles = rawArticles.map {
                    HomeNewsArticle(
                        title = it["title"] as? String ?: "",
                        imageUrl = it["imageUrl"] as? String ?: "",
                        url = it["url"] as? String ?: "",
                        source = it["source"] as? String ?: ""
                    )
                }

                _uiState.value = HomeNewsUiState(
                    isLoading = false,
                    articles = articles
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = HomeNewsUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to load news"
                )
            }
        }
    }
}