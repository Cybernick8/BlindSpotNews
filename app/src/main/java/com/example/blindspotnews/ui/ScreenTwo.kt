package com.example.blindspotnews.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun ScreenTwo(navController: NavController) {
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
            onClick = { /* TODO: handle video upload */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Upload Video")
        }

        // Upload Text Button
        Button(
            onClick = { /* TODO: handle text upload */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Upload Text")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = { navController.navigate("screen_output") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Go to Output Screen")
        }

        // Back to the Analysis Screen
        Button(onClick = { navController.navigate("analysis_test") {
            popUpTo("analysis_test") { inclusive = false } } }
        ) { Text("Go to Analysis Screen") }

        Spacer(modifier = Modifier.height(32.dp))

        // Back to Home Button
        Button(onClick = { navController.navigate("screen_one") }) {
            Text("Back to Home")
        }
    }
}
