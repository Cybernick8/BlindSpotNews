package com.example.blindspotnews.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

data class HomeNewsArticle(
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val url: String = "",
    val source: String = "",
    val publishedAt: String = ""
)

data class HomeNewsUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val articles: List<HomeNewsArticle> = emptyList(),
    val error: String? = null,
    val hasMore: Boolean = true
)

class NewsViewModel : ViewModel() {
    private val functions = FirebaseFunctions.getInstance()

    private var currentPage = 1
    private var totalResults = Int.MAX_VALUE
    private var isRequestRunning = false

    private val _uiState = MutableStateFlow(HomeNewsUiState())
    val uiState: StateFlow<HomeNewsUiState> = _uiState

    var search by mutableStateOf("")
    var selectedTopic by mutableStateOf("All")

    fun loadNews(topic: String, search: String, reset: Boolean = true) {
        if (isRequestRunning) return

        if (!reset && !_uiState.value.hasMore) return

        viewModelScope.launch {
            isRequestRunning = true

            if (reset) {
                currentPage = 1
                totalResults = Int.MAX_VALUE
                _uiState.value = HomeNewsUiState(
                    isLoading = true,
                    articles = emptyList(),
                    hasMore = true
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoadingMore = true)
            }

            try {
                val result = functions
                    .getHttpsCallable("get_home_news")
                    .call(
                        mapOf(
                            "topic" to topic,
                            "search" to search,
                            "page" to currentPage
                        )
                    )
                    .await()

                @Suppress("UNCHECKED_CAST")
                val data = result.getData() as? Map<String, Any?> ?: emptyMap()

                @Suppress("UNCHECKED_CAST")
                val rawArticles = data["articles"] as? List<Map<String, Any?>> ?: emptyList()

                totalResults = (data["totalResults"] as? Number)?.toInt() ?: totalResults

                val newArticles = rawArticles.map {
                    HomeNewsArticle(
                        title = it["title"] as? String ?: "",
                        description = it["description"] as? String ?: "",
                        imageUrl = it["imageUrl"] as? String ?: "",
                        url = it["url"] as? String ?: "",
                        source = it["source"] as? String ?: "",
                        publishedAt = it["publishedAt"] as? String ?: ""
                    )
                }

                val currentArticles = if (reset) emptyList() else _uiState.value.articles

                val mergedArticles = (currentArticles + newArticles)
                    .distinctBy { it.url.ifBlank { it.title } }

                val addedNewArticles = mergedArticles.size > currentArticles.size

                if (addedNewArticles) {
                    currentPage++
                }

                val hasMore =
                    addedNewArticles &&
                            mergedArticles.size < totalResults &&
                            newArticles.isNotEmpty()

                _uiState.value = HomeNewsUiState(
                    isLoading = false,
                    isLoadingMore = false,
                    articles = mergedArticles,
                    error = null,
                    hasMore = hasMore
                )

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = e.message ?: "Failed to load news"
                )
            } finally {
                isRequestRunning = false
            }
        }
    }

    fun loadMoreNews() {
        loadNews(selectedTopic, search, reset = false)
    }
}