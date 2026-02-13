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
import com.example.blindspotnews.backend.Api
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ScreenOutput(navController: NavController) {
    var outputText by remember { mutableStateOf("Loading...") }
    var isAuthenticated by remember { mutableStateOf(false) }

    val url = "https://www.tiktok.com/@thetalkshour/video/7505110474585836831?is_from_webapp=1&sender_device=pc&web_id=7551590510020920887"
    val isVideo = true

    FirebaseAuth.getInstance()
        .signInAnonymously()
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                isAuthenticated = true
            }
            else{
                outputText = "Authentification Failed. Try again."
            }
        }

    LaunchedEffect(isAuthenticated) {
        if(isAuthenticated) {
            try {
                outputText = Api().analyzeVideoOrArticle(url, isVideo)
            } catch (e: Exception) {
                outputText = "Error: ${e.message}"
            }
        }
    }


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

        // Output box (currently empty)
        OutlinedTextField(
            value = outputText,
            onValueChange = { outputText = it },
            placeholder = { Text("Your analysis output will appear here...") },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 24.dp),
            singleLine = false,
            maxLines = 20
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Back to Home
        Button(onClick = { navController.navigate("screen_one") }) {
            Text("Back to Home")
        }
    }
}
