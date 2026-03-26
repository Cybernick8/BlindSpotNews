package com.example.blindspotnews.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ProfileScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .border(3.dp, Color.Black)
            .padding(20.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Profile",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(22.dp))

            ProfileOptionButton("Dark Mode") {
                // later: toggle dark mode
            }

            Spacer(modifier = Modifier.height(12.dp))

            ProfileOptionButton("History of Past Custom\nArticles Analyzed") {
                // later: open history
            }

            Spacer(modifier = Modifier.height(12.dp))

            ProfileOptionButton("Topic Preferences") {
                // later: open topic preferences
            }

            Spacer(modifier = Modifier.height(12.dp))

            ProfileOptionButton("Blocked Article?") {
                // later: open blocked articles
            }

            Spacer(modifier = Modifier.height(12.dp))

            ProfileOptionButton("Personalization Tab?") {
                // later: open personalization
            }

            Spacer(modifier = Modifier.height(12.dp))

            ProfileOptionButton("Recently Viewed") {
                // later: open recently viewed
            }

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("login") {
                        popUpTo("screen_one") { inclusive = true }
                    }
                },
                // other button params...
            ) {
                Text("Sign Out")
            }
        }
    }
}

@Composable
fun ProfileOptionButton(
    text: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .height(72.dp),
        border = BorderStroke(2.dp, Color.Black),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = Color.Black
        )
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            fontSize = 20.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color.Black
        )
    }
}