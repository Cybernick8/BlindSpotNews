package com.example.blindspotnews.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.blindspotnews.backend.OutputViewModel

@Composable
fun AnalysisScreen(
    navController: NavController,
    viewModel: OutputViewModel = viewModel(),
    analysisViewModel: AnalysisViewModel = viewModel()
) {
    var urlInput by remember { mutableStateOf("") }
    var isVideoInput by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background())
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { navController.navigate("screen_one") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.buttonBackground(),
                    contentColor = AppColors.buttonText()
                )
            ) {
                Text("Home")
            }

            Button(
                onClick = { navController.navigate("screen_two") },
                modifier = Modifier.weight(1.7f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.buttonBackground(),
                    contentColor = AppColors.buttonText()
                )
            ) {
                Text("Text & Video Upload")
            }

            Button(
                onClick = { navController.navigate("screen_output") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.buttonBackground(),
                    contentColor = AppColors.buttonText()
                )
            ) {
                Text("Output")
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            label = { Text("Enter Article/Video URL") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.text(),
                unfocusedBorderColor = AppColors.text(),
                focusedTextColor = AppColors.text(),
                unfocusedTextColor = AppColors.text(),
                focusedLabelColor = AppColors.text(),
                unfocusedLabelColor = AppColors.text(),
                cursorColor = AppColors.text(),
                focusedContainerColor = AppColors.card(),
                unfocusedContainerColor = AppColors.card()
            )
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = isVideoInput,
                onCheckedChange = { isVideoInput = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = AppColors.buttonBackground(),
                    uncheckedColor = AppColors.text(),
                    checkmarkColor = AppColors.buttonText()
                )
            )

            Text(
                text = "Is this a video?",
                color = AppColors.text()
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = { viewModel.analyze(urlInput, isVideoInput) },
            enabled = urlInput.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.buttonBackground(),
                contentColor = AppColors.buttonText(),
                disabledContainerColor = AppColors.card(),
                disabledContentColor = AppColors.text()
            )
        ) {
            Text("Analyze Blindspot")
        }

        Spacer(modifier = Modifier.height(24.dp))

        HorizontalDivider(color = AppColors.text())

        Spacer(modifier = Modifier.height(24.dp))

        if (viewModel.isLoading) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Analyzing...",
                    color = AppColors.text(),
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                CircularProgressIndicator(color = AppColors.text())
            }
        } else if (viewModel.analyzedText.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AppColors.card()
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    Text(
                        text = "BlindSpot Analysis:",
                        color = AppColors.text(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Text(
                        text = viewModel.overallAnalysis,
                        color = AppColors.text(),
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AppColors.card()
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    Text(
                        text = "Article Text:",
                        color = AppColors.text(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    HighlightedArticleText(
                        fullText = viewModel.analyzedText,
                        issues = viewModel.detectedIssues
                    )
                }
            }
        } else {
            Text(
                text = "Analysis Result:",
                color = AppColors.text(),
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (viewModel.analyzedText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    analysisViewModel.saveArticleData(
                        text = viewModel.analyzedText,
                        overallAnalysis = viewModel.overallAnalysis,
                        issues = viewModel.detectedIssues,
                        imageIssues = viewModel.imageIssues,
                        sourceUrl = urlInput,
                        biasRating = viewModel.biasRating,
                        alignment = viewModel.alignment
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.buttonBackground(),
                    contentColor = AppColors.buttonText()
                )
            ) {
                Text("Save to Database")
            }
        }
    }
}