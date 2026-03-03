package com.example.blindspotnews.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController


enum class Leaning { VERY_LEFT, LEFT, CENTER, RIGHT, VERY_RIGHT }

data class ArticleCardData(
    val title: String,
    val category: String,
    val ratingText: String,   // "5/10"
    val leaningText: String,  // "This article is: Right Leaning"
    val leaning: Leaning
)

@Composable
fun ScreenOne(navController: NavController) {
    var search by remember { mutableStateOf("") }
    var selectedTopic by remember { mutableStateOf("All") }
    var selectedFeed by remember { mutableStateOf("Trending") }

    val topics = listOf("All", "Business", "International", "Politics", "Tech")
    val feedTabs = listOf("Trending", "For You")

    // Sample cards (swap with real data later)
    val articles = listOf(
        ArticleCardData("Title of Article", "Category for Article", "5/10", "This article is: Right Leaning", Leaning.RIGHT),
        ArticleCardData("Title", "Business", "7/10", "VERY Left", Leaning.VERY_LEFT),
        ArticleCardData("Title", "Business", "7/10", "VERY Left", Leaning.VERY_LEFT),
    )
    val popular = ArticleCardData("Title", "International", "10/10", "Very Right", Leaning.VERY_RIGHT)

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("analysis_test") }, // "+" goes to Analysis screen
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                // --- NAV BUTTONS ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = { navController.navigate("screen_two") }) {
                        Text("Text & Video Upload")
                    }
                    Button(onClick = { navController.navigate("screen_output") }) {
                        Text("Output")
                    }
                }

            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Home title
            Text(
                text = "Home",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                color = Color.Black
            )

            Spacer(Modifier.height(12.dp))

            // Search bar
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(12.dp))

            // Topics to filter
            Text(
                text = "Topics To Filter News",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 6.dp),
                color = Color.Black
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                topics.take(4).forEach { topic ->
                    FilterChip(
                        selected = selectedTopic == topic,
                        onClick = { selectedTopic = topic },
                        label = { Text(topic) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Date + App Name row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("Date", style = MaterialTheme.typography.labelSmall, color = Color.Black)
                    Text(
                        "BlindSpotNews",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black
                    )
                }
                Text("Bias Rating", style = MaterialTheme.typography.labelSmall, color = Color.Black)
            }

            Spacer(Modifier.height(12.dp))

            // Trending / For You
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                feedTabs.forEach { tab ->
                    val selected = selectedFeed == tab
                    OutlinedButton(
                        onClick = { selectedFeed = tab },
                        modifier = Modifier.weight(1f),
                        border = if (selected) BorderStroke(2.dp, Color.Black)
                        else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(tab, color = Color.Black)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Article cards list
            articles.forEach { item ->
                ArticleCard(
                    data = item,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clickable {
                            // If you want: navController.navigate("screen_two")
                        }
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "POPULAR ARTICLE",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = Color.Black
            )

            Spacer(Modifier.height(10.dp))

            ArticleCard(
                data = popular,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )
        }
    }
}



@Composable
private fun ArticleCard(
    data: ArticleCardData,
    modifier: Modifier = Modifier
) {
    val leaningColor = when (data.leaning) {
        Leaning.VERY_LEFT -> MaterialTheme.colorScheme.primary
        Leaning.LEFT -> MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
        Leaning.CENTER -> Color.Black
        Leaning.RIGHT -> MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
        Leaning.VERY_RIGHT -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Thumbnail placeholder
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {}

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(data.title, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(data.category, style = MaterialTheme.typography.labelSmall, color = Color.Black)
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = data.leaningText,
                        color = leaningColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(data.ratingText, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }
    }
}