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
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import coil.compose.AsyncImage
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import com.example.blindspotnews.R

@Composable
fun AnalysisScreen(
    navController: NavController,
    analysisViewModel: AnalysisViewModel = viewModel(),
    autoUrl: String = ""
) {
    val activity = LocalActivity.current as ComponentActivity
    val viewModel: OutputViewModel = viewModel(activity)

    val videoPatterns = listOf(
        "youtube.com/watch", "youtube.com/shorts", "youtu.be/",
        "tiktok.com", "vm.tiktok.com",
        "vimeo.com",
        "twitch.tv",
        "instagram.com/reel", "instagram.com/p",
        "facebook.com/watch", "fb.watch",
        "twitter.com/i/status", "x.com/i/status"
    )

    val isVideo = videoPatterns.any { viewModel.urlInput.contains(it) }

    LaunchedEffect(autoUrl) {
        if (autoUrl.isNotBlank() && autoUrl != viewModel.urlInput) {
            val autoIsVideo = videoPatterns.any { autoUrl.contains(it) }
            viewModel.urlInput = autoUrl
            viewModel.analyze(autoUrl, autoIsVideo)
        }
    }


    val scrollState = rememberScrollState()

    var selectedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    var selectedImageUrl by remember { mutableStateOf<String?>(null) }

    if (selectedImageUrl != null) {
        Dialog(onDismissRequest = { selectedImageUrl = null }) {
            AsyncImage(
                model = selectedImageUrl,
                contentDescription = "Full Image",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background())
    ) {

        // 👇 BACKGROUND LOGO
        Image(
            painter = painterResource(id = R.drawable.blindspotnews_transparent),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp),
            alpha = 0.08f // 👈 adjust transparency here
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                Image(
                    painter = painterResource(id = R.drawable.blindspotnews_transparent),
                    contentDescription = "Home",
                    modifier = Modifier
                        .size(80.dp)
                        .clickable {
                            navController.navigate("screen_one")
                        }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))


            OutlinedTextField(
                value = viewModel.urlInput,
                onValueChange = { viewModel.urlInput = it },
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


            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = { viewModel.analyze(viewModel.urlInput, isVideo) },
                enabled = viewModel.urlInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.buttonBackground(),
                    contentColor = AppColors.buttonText(),
                    disabledContainerColor = AppColors.buttonBackground().copy(alpha = 0.4f),
                    disabledContentColor = AppColors.buttonText().copy(alpha = 0.4f)
                )
            ) {
                Text("Analyze Blindspot")
            }

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider(color = AppColors.text())

            Spacer(modifier = Modifier.height(24.dp))

            val result = viewModel.analysisResult

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
            } else if (result != null) {
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
                            text = if (result.analyzedText.isEmpty()) "Unable to Analyze" else "BlindSpot Analysis:",
                            color = AppColors.text(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Text(
                            text = result.overallAnalysis,
                            color = AppColors.text(),
                            fontStyle = FontStyle.Italic
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (result.analyzedText.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Bias Score: ${result.biasRating}",
                                    fontWeight = FontWeight.SemiBold
                                )

                                Text(
                                    text = "Alignment: ${result.alignment}",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                if (result.analyzedText.isNotEmpty()) {
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
                                text = if (isVideo) "Video Transcript Analysis:" else "Article Analysis:",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            HighlightedArticleText(
                                fullText = result.analyzedText,
                                issues = result.detectedIssues
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            if (result.imageIssues.isNotEmpty()) {

                                Text(
                                    text = "Image Issues:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(result.imageIssues) { issue ->

                                        val frame = result.frames.getOrNull(issue.frameIndex)



                                        if (frame != null) {

                                            Card(
                                                modifier = Modifier.width(250.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    val data = frame.url

                                                    if (viewModel.isBase64(data)) {
                                                        val bitmap =
                                                            viewModel.decodeToBitmap(data)

                                                        if (bitmap != null) {
                                                            Image(
                                                                bitmap = bitmap.asImageBitmap(),
                                                                contentDescription = "Frame",
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .height(150.dp)
                                                                    .clickable {
                                                                        selectedImageUrl = data
                                                                    }
                                                            )
                                                        }
                                                    } else {
                                                        AsyncImage(
                                                            model = data,
                                                            contentDescription = "Frame",
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(150.dp)
                                                                .clickable {
                                                                    selectedImageUrl = data
                                                                }
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.height(8.dp))

                                                    Text(
                                                        text = issue.type,
                                                        fontWeight = FontWeight.Bold
                                                    )

                                                    Text(
                                                        text = issue.explanation,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                        }
                    }
                }

                if (selectedBitmap != null) {
                    Dialog(onDismissRequest = { selectedBitmap = null }) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(androidx.compose.ui.graphics.Color.Black)
                                .clickable { selectedBitmap = null },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = selectedBitmap!!.asImageBitmap(),
                                contentDescription = "Expanded Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )
                        }
                    }
                }

            }
            else {
                Text(
                    text = "Enter a URL above to receive an analysis",
                    color = AppColors.text(),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}