package com.example.blindspotnews.backend
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.util.concurrent.TimeUnit


class api {
    suspend fun getTranscript(videoUrl: String): String? = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        val supadataApiKey = "sd_b2a89ace15aaf36624c00853d67c4235"
        val supadataUrl = "https://api.supadata.ai/v1/transcript"
        //val supadataUrl = "https://api.supadata.ai/extract/tiktok/transcript"


        Log.d("Supadata_Link", "videoUrl: $videoUrl")

        try {
            val json = JSONObject().apply {
                put("url", videoUrl)
            }

            val request = Request.Builder()
                .url("$supadataUrl?url=${videoUrl}")
                .addHeader("x-api-key", supadataApiKey)
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                Log.d("API_DEBUG", "Supadata response code: ${response.code}")
                Log.d("API_DEBUG", "Supadata JSON: $responseBody")

                if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                    Log.e("API_ERROR", "Supadata request failed: HTTP ${response.code}")
                    return@withContext null
                }

                return@withContext responseBody
            }
        } catch (e: Exception) {
            Log.e("API_ERROR", "Supadata failed: ${e.message}")
            null
        }
    }

    suspend fun analyzeBias(text: String): String? {
        val client = OkHttpClient()
        // val openAiKey = api key goes here
        val openAiUrl = "https://api.openai.com/v1/chat/completions"

        val prompt = """
        Analyze this article. Output only:
        5 bullet points showing false info or bias. 
        Title each with the quoted text.
        Start each description with "left", "right", or "fake" (for left bias, right bias, or false info).
        At the end, write “Bias: X/10” rating its overall bias or inaccuracy.
        Then write either "Left", "Lean Left", "Center", "Lean Right", or "Right" based on what political alignment/bias the article shows.
        Nothing else.
        Article:
        $text
    """.trimIndent()

        val openAiMessages = org.json.JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", "You are a fact and bias checking assistant for articles and transcripts.")
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            })
        }

        val openAiBody = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("messages", openAiMessages)
        }

        return try {
            val requestBody = openAiBody.toString()
                .toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(openAiUrl)
                .addHeader("Authorization", "Bearer $openAiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val json = JSONObject(response.body?.string() ?: "{}")
                /* Previous code 11/11
                if (json.has("error")) {
                    Log.e("API_ERROR", "OpenAI error: ${json.getJSONObject("error").getString("message")}")
                }
                Log.d("API_DEBUG", "OpenAI JSON: $json")

                json.optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")*/
                if (json.has("error")) {
                    val msg = json.getJSONObject("error").getString("message")
                    Log.e("API_ERROR", "OpenAI error: $msg")
                    return@use "OpenAI error: $msg"
                }

                Log.d("API_DEBUG", "OpenAI JSON: $json")

                json.optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")

            }
        } catch (e: Exception) {
            Log.e("API_ERROR", "OpenAI failed: ${e.message}")
            "OpenAI exception: ${e.message ?: "unknown error"}"
        }

    }

    suspend fun analyzeVideoOrArticle(url: String, isVideo: Boolean): String? {
        if (isVideo) {
            val transcript = getTranscript(url)
            return if (transcript != null) analyzeBias(transcript) else "Failed to get transcript"
        } else {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(url)
                    .build()

                val response = client.newCall(request).execute()
                val html = response.body?.string()
                response.close()

                if (html.isNullOrEmpty()) {
                    Log.e("ARTICLE_ERROR", "Empty or invalid HTML for $url")
                    return "Failed to retrieve article HTML"
                }

                // Parse and clean the HTML with Jsoup
                val doc: Document = Jsoup.parse(html)

                // Removing unused elements to curb bloating
                doc.select(
                    "script, style, nav, footer, header, iframe, form, input, button, " +
                            "canvas, svg, video, audio, link, meta, noscript"
                ).remove()

                // Trying to find main content
                val possibleContentSelectors = listOf(
                    "article",
                    "main",
                    "div[class*=content]",
                    "div[class*=article]",
                    "div[class*=story]",
                    "div[class*=body]",
                    "div[class*=post]",
                    "div[class*=feature]",
                    "section[class*=content]"
                )

                // Find the first selector that has useful info
                var articleBody: Elements? = null
                for (selector in possibleContentSelectors) {
                    val selected = doc.select(selector)
                    if (selected.text().length > 100) { // only count if it actually has substance, 100 can be adjusted
                        articleBody = selected
                        break
                    }
                }

                // STEP 3: Fallback if nothing matched
                if (articleBody == null) {
                    articleBody = doc.select("p") // fallback to all paragraphs
                }

                val articleText = articleBody!!.text().trim()

                if (articleText.isEmpty()) {
                    Log.e("ARTICLE_ERROR", "No readable text found for $url")
                    return "No readable article text found"
                }

                return analyzeBias(articleText)
            } catch (e: Exception) {
                Log.e("ARTICLE_ERROR", "Failed to analyze article: ${e.message}")
                return "Failed to process article: ${e.message}"
            }

        }
    }

}