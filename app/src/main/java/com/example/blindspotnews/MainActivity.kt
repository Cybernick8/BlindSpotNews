package com.example.blindspotnews

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.blindspotnews.ui.ScreenOne
import com.example.blindspotnews.ui.ScreenTwo

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Force a light background for visibility with black text
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.White  // ensures black text is visible even in system dark mode
            ) {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "screen_one") {
        composable("screen_one") { ScreenOne(navController) }
        composable("screen_two") { ScreenTwo(navController) }
    }
}
