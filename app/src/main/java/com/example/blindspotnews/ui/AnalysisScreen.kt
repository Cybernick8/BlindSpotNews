package com.example.blindspotnews.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.blindspotnews.backend.OutputViewModel

@Composable
fun AnalysisScreen(
    // The viewModel() helper finds or creates the OutputViewModel for us
    viewModel: OutputViewModel = viewModel()
) {
    // 1. Local UI State: Holds the text the user is currently typing
    var urlInput by remember { mutableStateOf("") }

    // 2. Local UI State: Holds the state of the checkbox
    var isVideoInput by remember { mutableStateOf(false) }

    // 3. Simple layout container
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {

        // --- INPUT SECTION ---

        TextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            label = { Text("Enter Article/Video URL") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = isVideoInput,
                onCheckedChange = { isVideoInput = it }
            )
            Text(text = "Is this a video?")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- ACTION SECTION ---

        Button(
            onClick = {
                // 4. Trigger the ViewModel function
                // This starts the Auth -> Fetch -> API chain
                viewModel.analyze(urlInput, isVideoInput)
            },
            // Disable button if URL is empty to prevent errors
            enabled = urlInput.isNotBlank()
        ) {
            Text("Analyze Blindspot")
        }

        Spacer(modifier = Modifier.height(24.dp))

        HorizontalDivider() // Use Divider() if using Material 2

        Spacer(modifier = Modifier.height(24.dp))

        // --- OUTPUT SECTION ---

        Text(
            text = "Analysis Result:",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 5. Observing the ViewModel
        // outputText is a 'mutableStateOf', so this Text updates automatically
        Text(
            text = viewModel.outputText,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
