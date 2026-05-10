package com.example.blindspotnews.ui

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

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
                    "right", "lean right" ->
                        if (AppThemeState.isDarkMode) Color(0xFFFF8A80).copy(alpha = 0.55f)
                        else Color.Red.copy(alpha = 0.30f)

                    "left", "lean left" ->
                        if (AppThemeState.isDarkMode) Color(0xFF82B1FF).copy(alpha = 0.55f)
                        else Color.Blue.copy(alpha = 0.30f)

                    "fake" ->
                        if (AppThemeState.isDarkMode) Color(0xFFFFF176).copy(alpha = 0.65f)
                        else Color.Yellow.copy(alpha = 0.35f)

                    else ->
                        if (AppThemeState.isDarkMode) Color(0xFFBDBDBD).copy(alpha = 0.50f)
                        else Color.LightGray.copy(alpha = 0.30f)
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
        style = MaterialTheme.typography.bodyMedium.copy(
            color = AppColors.text()
        ),
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
            containerColor = AppColors.card(),
            titleContentColor = AppColors.text(),
            textContentColor = AppColors.text(),
            title = {
                Text(
                    text = when (issue.type.lowercase()) {
                        "fake" -> "Possible misinformation"
                        "left" -> "Left-leaning framing"
                        "right" -> "Right-leaning framing"
                        else -> "Flagged: ${issue.type.uppercase()}"
                    },
                    color = AppColors.text(),
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                )
            },
            text = {
                Text(
                    text = issue.explanation,
                    color = AppColors.text()
                )
            },
            confirmButton = {
                Button(
                    onClick = { selectedIssue = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.buttonBackground(),
                        contentColor = AppColors.buttonText()
                    )
                ) {
                    Text("Got it")
                }
            }
        )
    }
}