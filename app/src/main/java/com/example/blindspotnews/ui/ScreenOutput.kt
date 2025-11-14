package com.example.blindspotnews.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.blindspotnews.backend.AnalysisResultStore
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll


@Composable
fun ScreenOutput(navController: NavController) {
    // Get whatever the last result was (or empty string if null)
    val initialText = AnalysisResultStore.lastResult ?: ""

    var outputText by remember { mutableStateOf(TextFieldValue(initialText)) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text(
            text = "Output",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.Black,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 24.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)    // Enables scrolling
        ) {
            OutlinedTextField(
                value = outputText,
                onValueChange = { outputText = it },
                placeholder = { Text("Your analysis output will appear here...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = Int.MAX_VALUE         // Allows longer output
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Back to Home
        Button(onClick = { navController.navigate("screen_one") }) {
            Text("Back to Home")
        }
    }
}
