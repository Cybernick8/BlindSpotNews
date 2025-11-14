package com.example.blindspotnews.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.blindspotnews.backend.api
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.blindspotnews.backend.AnalysisResultStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext





@Composable
fun ScreenTwo(navController: NavController) {

    // Backend
    val api = remember { api() }
    val scope = rememberCoroutineScope()

    // UI state
    var showUrlDialog by remember { mutableStateOf(false) }
    var isVideoMode by remember { mutableStateOf(true) }  // true = video, false = article/text
    var urlInput by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var resultText by remember { mutableStateOf<String?>(null) }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text(
            text = "Text and Video Upload",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.Black,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Upload Video Button
        Button(
            onClick = {
                isVideoMode = true
                urlInput = ""
                showUrlDialog = true

            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Upload Video")
        }

        // Upload Text Button
        Button(
            onClick = {
                isVideoMode = false
                urlInput = ""
                showUrlDialog = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Upload Text")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { navController.navigate("screen_output") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Go to Output Screen")
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Back to Home Button
        Button(onClick = { navController.navigate("screen_one") }) {
            Text("Back to Home")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Loading indicator
        if (isLoading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Analyzing… please wait")
        }

        // Error message
        errorMessage?.let { msg ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = msg, color = Color.Red)
        }

        // Show result text on this screen for now
        resultText?.let { text ->
            Spacer(modifier = Modifier.height(16.dp))
            Text("Analysis Result:")
            Spacer(modifier = Modifier.height(4.dp))
            Text(text)
        }
    }

    // URL Input Dialog
    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = {
                showUrlDialog = false
            },
            title = {
                Text(if (isVideoMode) "Enter Video URL" else "Enter Article URL")
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val currentUrl = urlInput.trim()
                        if (currentUrl.isBlank()) {
                            return@TextButton
                        }

                        showUrlDialog = false

                        scope.launch {
                            val rawResult = try {
                                withContext(Dispatchers.IO) {
                                    api.analyzeVideoOrArticle(
                                        url = currentUrl,
                                        isVideo = isVideoMode
                                    )
                                }
                            } catch (e: Exception) {
                                "Failed to analyze: ${e.message}"
                            }

                            val displayText = rawResult ?: "No response from analysis."

                            // Save the result in shared object
                            AnalysisResultStore.lastResult = displayText

                            // Navigate to the output screen
                            navController.navigate("screen_output")
                        }
                    }
                ) {
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
