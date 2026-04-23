package com.example.blindspotnews.ui

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

data class BiasIssue(
    val type: String, // "left", "right", "fake"
    val start: Int,
    val end: Int,
    val explanation: String
)

@Composable
fun HighlightedArticleText(
    fullText: String,
    issues: List<BiasIssue>
) {
    // State to track which issue the user just clicked on (controls the popup)
    var selectedIssue by remember { mutableStateOf<BiasIssue?>(null) }

    // Build the annotated string with colors and clickable tags
    val annotatedText = buildAnnotatedString {
        append(fullText)

        issues.forEachIndexed { index, issue ->
            if (issue.start >= 0 && issue.end <= fullText.length && issue.start < issue.end) {

                // Set the color
                val highlightColor = when (issue.type.lowercase()) {
                    "right", "lean right" -> Color.Red.copy(alpha = 0.3f)
                    "left", "lean left" -> Color.Blue.copy(alpha = 0.3f)
                    "fake" -> Color.Yellow.copy(alpha = 0.3f)
                    else -> Color.LightGray.copy(alpha = 0.3f)
                }

                // Apply the background color
                addStyle(
                    style = SpanStyle(background = highlightColor),
                    start = issue.start,
                    end = issue.end
                )

                // Add a hidden "tag" to this exact text span so we can click it later
                addStringAnnotation(
                    tag = "BIAS_ISSUE",
                    annotation = index.toString(), // Store the index of the issue in the list
                    start = issue.start,
                    end = issue.end
                )
            }
        }
    }

    // The actual text UI component that listens for clicks
    ClickableText(
        text = annotatedText,
        style = MaterialTheme.typography.bodyMedium,
        onClick = { offset ->
            // Check if the character the user clicked has a "BIAS_ISSUE" tag
            annotatedText.getStringAnnotations(tag = "BIAS_ISSUE", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    // If they clicked a highlighted part, grab the issue and trigger the popup
                    val clickedIndex = annotation.item.toInt()
                    selectedIssue = issues[clickedIndex]
                }
        }
    )

    // The Popup Dialog (Only shows if selectedIssue is not null)
    selectedIssue?.let { issue ->
        AlertDialog(
            onDismissRequest = { selectedIssue = null }, // Closes if they click outside the box
            title = {
                Text(text = "Flagged: ${issue.type.uppercase()}")
            },
            text = {
                Text(text = issue.explanation)
            },
            confirmButton = {
                Button(onClick = { selectedIssue = null }) {
                    Text("Got it")
                }
            }
        )
    }
}