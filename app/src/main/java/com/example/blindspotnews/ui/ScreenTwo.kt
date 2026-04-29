package com.example.blindspotnews.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun ScreenTwo(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Text and Video Upload",
            style = MaterialTheme.typography.headlineMedium,
            color = AppColors.text(),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 24.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = { navController.navigate("screen_output") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.buttonBackground(),
                contentColor = AppColors.buttonText()
            )
        ) {
            Text("Go to Output Screen")
        }

        Button(
            onClick = {
                navController.navigate("analysis_test") {
                    popUpTo("analysis_test") { inclusive = false }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.buttonBackground(),
                contentColor = AppColors.buttonText()
            )
        ) {
            Text("Go to Analysis Screen")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { navController.navigate("screen_one") },
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.buttonBackground(),
                contentColor = AppColors.buttonText()
            )
        ) {
            Text("Back to Home")
        }
    }
}