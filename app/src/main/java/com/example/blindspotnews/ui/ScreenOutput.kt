package com.example.blindspotnews.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.blindspotnews.backend.OutputViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ScreenOutput(
    navController: NavController,
    viewModel: OutputViewModel = viewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "UID: ${FirebaseAuth.getInstance().currentUser?.uid ?: "Not signed in"}",
            color = AppColors.text()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Output",
            style = MaterialTheme.typography.headlineMedium,
            color = AppColors.text(),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = "hi",
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 24.dp),
            singleLine = false,
            maxLines = 20,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.text(),
                unfocusedBorderColor = AppColors.text(),
                focusedTextColor = AppColors.text(),
                unfocusedTextColor = AppColors.text(),
                focusedContainerColor = AppColors.card(),
                unfocusedContainerColor = AppColors.card(),
                cursorColor = AppColors.text()
            )
        )

        Button(
            onClick = { navController.navigate("screen_one") },
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.buttonBackground(),
                contentColor = AppColors.buttonText()
            )
        ) {
            Text("Back to Home")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                navController.navigate("analysis_test") {
                    popUpTo("analysis_test") { inclusive = false }
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.buttonBackground(),
                contentColor = AppColors.buttonText()
            )
        ) {
            Text("Go to Analysis Screen")
        }
    }
}