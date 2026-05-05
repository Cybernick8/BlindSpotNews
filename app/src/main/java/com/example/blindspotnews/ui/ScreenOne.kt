package com.example.blindspotnews.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import com.example.blindspotnews.R

@Composable
fun ScreenOne(
    navController: NavController,
    viewModel: NewsViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: "Guest"

    val topics = listOf("All", "Business", "International", "Politics", "Tech")

    LaunchedEffect(Unit) {
        viewModel.loadNews(viewModel.selectedTopic, viewModel.search)
    }

    Scaffold(
        containerColor = AppColors.background()
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(AppColors.background())
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Image(
                    painter = painterResource(id = R.drawable.blindspotnews_transparent),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(70.dp)
                        .padding(start = 2.dp, end = 8.dp)
                )

                Text(
                    text = "Home",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Start,
                    color = AppColors.text()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.text(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 115.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { navController.navigate("profile") },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(AppColors.buttonBackground())
                    ) {
                        Text("👤", color = AppColors.buttonText())
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = viewModel.search,
                onValueChange = { viewModel.search = it },
                placeholder = { Text("Search news") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { viewModel.loadNews(viewModel.selectedTopic, viewModel.search) }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.text(),
                    unfocusedBorderColor = AppColors.text(),
                    focusedTextColor = AppColors.text(),
                    unfocusedTextColor = AppColors.text(),
                    cursorColor = AppColors.text(),
                    focusedContainerColor = AppColors.card(),
                    unfocusedContainerColor = AppColors.card()
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                topics.forEach { topic ->
                    FilterChip(
                        selected = viewModel.selectedTopic == topic,
                        onClick = {
                            viewModel.selectedTopic = topic
                            viewModel.loadNews(viewModel.selectedTopic, viewModel.search)
                        },
                        label = { Text(topic) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppColors.AccentYellow,
                            selectedLabelColor = Color.Black,
                            containerColor = AppColors.card(),
                            labelColor = AppColors.text()
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = viewModel.selectedTopic == topic,
                            borderColor = AppColors.text(),
                            selectedBorderColor = AppColors.text()
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { navController.navigate("analysis_test") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.buttonBackground(),
                        contentColor = AppColors.buttonText()
                    )
                ) {
                    Text("Analyze an Article or Video")
                }

                Button(
                    onClick = { viewModel.loadNews(viewModel.selectedTopic, viewModel.search) },
                    modifier = Modifier
                        .height(42.dp)
                        .width(58.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.buttonBackground(),
                        contentColor = AppColors.buttonText()
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("↻", style = MaterialTheme.typography.titleLarge)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AppColors.text())
                    }
                }

                uiState.error != null -> {
                    Text(
                        text = uiState.error ?: "Unknown error",
                        color = Color.Red
                    )
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.articles) { article ->
                            NewsArticleCard(
                                article = article,
                                onClick = {
                                    if (article.url.isNotBlank()) {
                                        val encodedUrl = Uri.encode(article.url)
                                        navController.navigate("analysis_test?url=$encodedUrl")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewsArticleCard(
    article: HomeNewsArticle,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.5.dp, AppColors.text()),
        colors = CardDefaults.cardColors(containerColor = AppColors.card())
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = article.imageUrl,
                contentDescription = article.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .background(Color.LightGray)
            )

            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = article.source,
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.text(),
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.text(),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                if (article.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = article.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.text(),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (article.publishedAt.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = article.publishedAt,
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.text()
                    )
                }
            }
        }
    }
}